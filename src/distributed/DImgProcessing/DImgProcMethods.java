package distributed.DImgProcessing;

import Processing.IMGProcessor;
import mpi.MPI;
import mpi.Status;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class DImgProcMethods implements IMGProcessor {

    // Tag constants for MPI messaging
    private static final int MASTER = 0;
    private static final int TAG_DIMENSIONS = 0;
    private static final int TAG_PIXELS = 1;
    private static final int TAG_KERNEL_DIMS = 10;
    private static final int TAG_KERNEL_DATA = 11;
    private static final int TAG_RESULT_DIMENSIONS = 20;
    private static final int TAG_RESULT_PIXELS = 21;
    private static final int TAG_STOP = 99;

    /**
     * I switched from creating full BufferedImage objects just to convert to grayscale
     * to working directly with raw byte arrays. This saves memory and processing time,
     * especially for large images that would otherwise duplicate internal data structures.
     */
    public static byte[] convertToGrayscaleBytes(BufferedImage colorImage) {
        int width = colorImage.getWidth();
        int height = colorImage.getHeight();
        byte[] grayPixels = new byte[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = colorImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (r + g + b) / 3;
                grayPixels[y * width + x] = (byte) gray;
            }
        }

        return grayPixels;
    }

    /**
     * Instead of working with BufferedImages for convolution, I apply it directly on
     * byte arrays. This avoids unnecessary object creation and lets me reuse buffers.
     */
    public static byte[] applyConvolution(byte[] input, int width, int height, int[][] kernel) {
        int kernelHeight = kernel.length;
        int kernelWidth = kernel[0].length;
        int kCenterX = kernelWidth / 2;
        int kCenterY = kernelHeight / 2;

        byte[] output = new byte[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sum = 0;
                for (int ky = 0; ky < kernelHeight; ky++) {
                    for (int kx = 0; kx < kernelWidth; kx++) {
                        int px = x + kx - kCenterX;
                        int py = y + ky - kCenterY;

                        if (px < 0 || px >= width || py < 0 || py >= height) continue;
                        int gray = input[py * width + px] & 0xFF;
                        sum += gray * kernel[ky][kx];
                    }
                }
                output[y * width + x] = (byte) Math.min(Math.max(sum, 0), 255);
            }
        }

        return output;
    }

    public BufferedImage convertToGrayscale(BufferedImage colorImage) {
        // This just wraps the raw grayscale array into a BufferedImage for display/output
        int width = colorImage.getWidth();
        int height = colorImage.getHeight();
        byte[] grayBytes = convertToGrayscaleBytes(colorImage);

        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        grayImage.getRaster().setDataElements(0, 0, width, height, grayBytes);
        return grayImage;
    }


    public BufferedImage applyConvolution(BufferedImage inputImage, int[][] kernel) {
        // Same idea — perform the operation in memory-efficient byte form, then wrap
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        byte[] input = ((java.awt.image.DataBufferByte) inputImage.getRaster().getDataBuffer()).getData();
        byte[] result = applyConvolution(input, width, height, kernel);

        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        outputImage.getRaster().setDataElements(0, 0, width, height, result);
        return outputImage;
    }

    /**
     * This is the master logic. It partitions the image and distributes chunks to workers.
     * I avoid BufferedImage overhead by sending only byte arrays and stitching them together.
     */

    public BufferedImage processImage(String inputFilePath, int[][] kernel) {
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if (rank != MASTER) {
            System.err.println("processImage() should only be called on MASTER.");
            return null;
        }

        try {
            BufferedImage originalImage = ImageIO.read(new File(inputFilePath));
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            // Convert to grayscale immediately and extract bytes
            byte[] grayPixels = convertToGrayscaleBytes(originalImage);

            int numWorkers = size - 1;
            int kernelHeight = kernel.length;
            int overlap = kernelHeight / 2;
            int baseChunkHeight = height / numWorkers;

            // Precompute Y boundaries for each chunk (with overlap)
            int[] chunkStartY = new int[numWorkers + 1];
            chunkStartY[0] = 0;
            for (int i = 1; i < numWorkers; i++) {
                chunkStartY[i] = Math.max(i * baseChunkHeight - overlap, 0);
            }
            chunkStartY[numWorkers] = height;

            // Send chunks and kernel to workers
            for (int i = 0; i < numWorkers; i++) {
                int startY = chunkStartY[i];
                int endY = chunkStartY[i + 1];
                int rows = endY - startY;

                byte[] chunk = new byte[width * rows];
                System.arraycopy(grayPixels, startY * width, chunk, 0, width * rows);

                int workerRank = i + 1;
                MPI.COMM_WORLD.Send(new int[]{width, rows, startY}, 0, 3, MPI.INT, workerRank, TAG_DIMENSIONS);
                MPI.COMM_WORLD.Send(chunk, 0, chunk.length, MPI.BYTE, workerRank, TAG_PIXELS);

                // Flatten kernel and send
                MPI.COMM_WORLD.Send(new int[]{kernelHeight, kernel[0].length}, 0, 2, MPI.INT, workerRank, TAG_KERNEL_DIMS);
                int[] flatKernel = new int[kernelHeight * kernel[0].length];
                for (int r = 0; r < kernelHeight; r++)
                    for (int c = 0; c < kernel[0].length; c++)
                        flatKernel[r * kernel[0].length + c] = kernel[r][c];
                MPI.COMM_WORLD.Send(flatKernel, 0, flatKernel.length, MPI.INT, workerRank, TAG_KERNEL_DATA);
            }

            // Prepare output array
            byte[] output = new byte[width * height];

            // Receive results and stitch them back (excluding overlaps)
            for (int i = 0; i < numWorkers; i++) {
                int workerRank = i + 1;
                int[] dims = new int[3];
                MPI.COMM_WORLD.Recv(dims, 0, 3, MPI.INT, workerRank, TAG_RESULT_DIMENSIONS);

                int chunkWidth = dims[0];
                int chunkHeight = dims[1];
                int chunkY = dims[2];

                byte[] resultChunk = new byte[chunkWidth * chunkHeight];
                MPI.COMM_WORLD.Recv(resultChunk, 0, resultChunk.length, MPI.BYTE, workerRank, TAG_RESULT_PIXELS);

                // Avoid overlapping duplication
                int startCopy = (i == 0) ? 0 : overlap;
                int rowsToCopy = chunkHeight - ((i == 0 || i == numWorkers - 1) ? overlap : 2 * overlap);

                System.arraycopy(
                        resultChunk,
                        startCopy * chunkWidth,
                        output,
                        (chunkY + startCopy) * width,
                        rowsToCopy * chunkWidth
                );
            }

            BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            finalImage.getRaster().setDataElements(0, 0, width, height, output);
            return finalImage;

        } catch (IOException e) {
            System.err.println("Master error: " + e.getMessage());
            return null;
        }
    }

    /**
     * I changed this from static to instance method to avoid needing to create
     * an instance of DImgProcMethods just to call `applyConvolution`.
     * This also fits better since the class is already managing processing.
     */
    public void workerProcess(int rank) {
        while (true) {
            Status status = MPI.COMM_WORLD.Probe(MASTER, MPI.ANY_TAG);
            int tag = status.tag;

            if (tag == TAG_STOP) {
                MPI.COMM_WORLD.Recv(new byte[0], 0, 0, MPI.BYTE, MASTER, TAG_STOP);
                break;
            }

            if (tag == TAG_DIMENSIONS) {
                int[] dims = new int[3];
                MPI.COMM_WORLD.Recv(dims, 0, 3, MPI.INT, MASTER, TAG_DIMENSIONS);
                int width = dims[0];
                int height = dims[1];
                int startY = dims[2];

                byte[] chunk = new byte[width * height];
                MPI.COMM_WORLD.Recv(chunk, 0, chunk.length, MPI.BYTE, MASTER, TAG_PIXELS);

                int[] kDims = new int[2];
                MPI.COMM_WORLD.Recv(kDims, 0, 2, MPI.INT, MASTER, TAG_KERNEL_DIMS);

                int[] flatKernel = new int[kDims[0] * kDims[1]];
                MPI.COMM_WORLD.Recv(flatKernel, 0, flatKernel.length, MPI.INT, MASTER, TAG_KERNEL_DATA);

                // Reconstruct kernel
                int[][] kernel = new int[kDims[0]][kDims[1]];
                for (int i = 0; i < kDims[0]; i++)
                    for (int j = 0; j < kDims[1]; j++)
                        kernel[i][j] = flatKernel[i * kDims[1] + j];

                byte[] processed = applyConvolution(chunk, width, height, kernel);

                MPI.COMM_WORLD.Send(new int[]{width, height, startY}, 0, 3, MPI.INT, MASTER, TAG_RESULT_DIMENSIONS);
                MPI.COMM_WORLD.Send(processed, 0, processed.length, MPI.BYTE, MASTER, TAG_RESULT_PIXELS);
            }
        }
    }

    /**
     * Stop signal for workers. This sends a zero-length message with a specific tag.
     */
    public void stopWorkers(int numWorkers) {
        for (int i = 1; i <= numWorkers; i++) {
            MPI.COMM_WORLD.Send(new byte[0], 0, 0, MPI.BYTE, i, TAG_STOP);
        }
    }

    @Override
    public String toString() {
        return "Distributed Image Processor";
    }
}
