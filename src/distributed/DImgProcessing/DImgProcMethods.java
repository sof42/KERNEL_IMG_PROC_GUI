package distributed.DImgProcessing;

import Processing.IMGProcessor;
import mpi.MPI;
import mpi.Status;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class DImgProcMethods implements IMGProcessor {

    private static final int MASTER = 0;
    private static final int TAG_DIMENSIONS = 0;
    private static final int TAG_PIXELS = 1;
    private static final int TAG_KERNEL_DIMS = 10;
    private static final int TAG_KERNEL_DATA = 11;
    private static final int TAG_RESULT_DIMENSIONS = 20;
    private static final int TAG_RESULT_PIXELS = 21;
    private static final int TAG_STOP = 99;

    @Override
    public BufferedImage convertToGrayscale(BufferedImage colorImage) {
        int width = colorImage.getWidth();
        int height = colorImage.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = colorImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (r + g + b) / 3;
                int grayRgb = (gray << 16) | (gray << 8) | gray;
                grayImage.setRGB(x, y, grayRgb);
            }
        }
        return grayImage;
    }

    @Override
    public BufferedImage applyConvolution(BufferedImage inputImage, int[][] kernel) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int kernelHeight = kernel.length;
        int kernelWidth = kernel[0].length;
        int kernelCenterX = kernelWidth / 2;
        int kernelCenterY = kernelHeight / 2;

        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sum = 0;
                for (int ky = 0; ky < kernelHeight; ky++) {
                    for (int kx = 0; kx < kernelWidth; kx++) {
                        int px = x + kx - kernelCenterX;
                        int py = y + ky - kernelCenterY;
                        if (px < 0 || px >= width || py < 0 || py >= height) continue;

                        int pixelGray = inputImage.getRaster().getSample(px, py, 0);
                        sum += pixelGray * kernel[ky][kx];
                    }
                }
                int val = Math.min(Math.max(sum, 0), 255);
                outputImage.getRaster().setSample(x, y, 0, val);
            }
        }
        return outputImage;
    }

    /**
     * Master process: partition image into overlapping chunks, distribute,
     * collect results and reassemble without overlapping duplicates.
     */
    @Override
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

            BufferedImage grayImage = convertToGrayscale(originalImage);
            byte[] grayPixels = ((java.awt.image.DataBufferByte) grayImage.getRaster().getDataBuffer()).getData();

            int numWorkers = size - 1;
            int kernelHeight = kernel.length;
            int overlap = kernelHeight / 2; // overlap rows top & bottom

            int baseChunkHeight = height / numWorkers;

            // chunkStartY[i] = start row (inclusive) of chunk i (0-based for workers)
            int[] chunkStartY = new int[numWorkers + 1];
            chunkStartY[0] = 0;
            for (int i = 1; i < numWorkers; i++) {
                // start of chunk i = i * baseChunkHeight - overlap
                int start = i * baseChunkHeight - overlap;
                if (start < 0) start = 0;
                chunkStartY[i] = start;
            }
            chunkStartY[numWorkers] = height;

            // Now send chunks to workers (worker rank = i + 1)
            for (int i = 0; i < numWorkers; i++) {
                int startY = chunkStartY[i];
                int endY = chunkStartY[i + 1];
                int rows = endY - startY;

                byte[] chunkPixels = new byte[width * rows];
                System.arraycopy(grayPixels, startY * width, chunkPixels, 0, width * rows);

                int workerRank = i + 1;

                // Send chunk dimensions and startY
                MPI.COMM_WORLD.Send(new int[]{width, rows, startY}, 0, 3, MPI.INT, workerRank, TAG_DIMENSIONS);
                MPI.COMM_WORLD.Send(chunkPixels, 0, chunkPixels.length, MPI.BYTE, workerRank, TAG_PIXELS);

                // Send kernel info
                MPI.COMM_WORLD.Send(new int[]{kernelHeight, kernel[0].length}, 0, 2, MPI.INT, workerRank, TAG_KERNEL_DIMS);

                int[] flatKernel = new int[kernelHeight * kernel[0].length];
                for (int r = 0; r < kernelHeight; r++) {
                    for (int c = 0; c < kernel[0].length; c++) {
                        flatKernel[r * kernel[0].length + c] = kernel[r][c];
                    }
                }
                MPI.COMM_WORLD.Send(flatKernel, 0, flatKernel.length, MPI.INT, workerRank, TAG_KERNEL_DATA);
            }

            // Prepare output buffer
            byte[] outputPixels = new byte[width * height];

            // Receive processed chunks and stitch
            for (int i = 0; i < numWorkers; i++) {
                int workerRank = i + 1;

                int[] dimsAndStart = new int[3];
                MPI.COMM_WORLD.Recv(dimsAndStart, 0, 3, MPI.INT, workerRank, TAG_RESULT_DIMENSIONS);

                int chunkWidth = dimsAndStart[0];
                int chunkHeight = dimsAndStart[1];
                int chunkStartYReceived = dimsAndStart[2];

                byte[] processedChunkPixels = new byte[chunkWidth * chunkHeight];
                MPI.COMM_WORLD.Recv(processedChunkPixels, 0, processedChunkPixels.length, MPI.BYTE, workerRank, TAG_RESULT_PIXELS);

                // Calculate how many rows to copy excluding overlaps
                int startCopyRow = 0;
                int rowsToCopy = chunkHeight;

                if (i == 0) {
                    // First chunk: exclude bottom overlap rows
                    rowsToCopy = chunkHeight - overlap;
                } else if (i == numWorkers - 1) {
                    // Last chunk: exclude top overlap rows
                    startCopyRow = overlap;
                    rowsToCopy = chunkHeight - overlap;
                } else {
                    // Middle chunks: exclude top and bottom overlap rows
                    startCopyRow = overlap;
                    rowsToCopy = chunkHeight - 2 * overlap;
                }

                // Copy non-overlapping rows to output image
                System.arraycopy(
                        processedChunkPixels,
                        startCopyRow * chunkWidth,
                        outputPixels,
                        (chunkStartYReceived + startCopyRow) * width,
                        rowsToCopy * chunkWidth
                );
            }

            BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            outputImage.getRaster().setDataElements(0, 0, width, height, outputPixels);

            return outputImage;

        } catch (IOException e) {
            System.err.println("Master error: " + e.getMessage());
            return null;
        }
    }


    /**
     * Worker process: receive chunk with overlap, apply convolution, send back full processed chunk.
     */
    public static void workerProcess(int rank) {
        while (true) {
            Status status = MPI.COMM_WORLD.Probe(MASTER, MPI.ANY_TAG);
            int tag = status.tag;

            if (tag == TAG_STOP) {
                MPI.COMM_WORLD.Recv(new byte[0], 0, 0, MPI.BYTE, MASTER, TAG_STOP);
                break;
            }

            if (tag == TAG_DIMENSIONS) {
                // Receive chunk dimensions + start Y
                int[] dimsAndStart = new int[3];
                MPI.COMM_WORLD.Recv(dimsAndStart, 0, 3, MPI.INT, MASTER, TAG_DIMENSIONS);
                int width = dimsAndStart[0];
                int height = dimsAndStart[1];
                int startY = dimsAndStart[2];

                // Receive pixels
                byte[] pixels = new byte[width * height];
                MPI.COMM_WORLD.Recv(pixels, 0, pixels.length, MPI.BYTE, MASTER, TAG_PIXELS);

                // Receive kernel dims
                int[] kernelDims = new int[2];
                MPI.COMM_WORLD.Recv(kernelDims, 0, 2, MPI.INT, MASTER, TAG_KERNEL_DIMS);
                int kRows = kernelDims[0];
                int kCols = kernelDims[1];

                // Receive kernel data
                int[] flatKernel = new int[kRows * kCols];
                MPI.COMM_WORLD.Recv(flatKernel, 0, flatKernel.length, MPI.INT, MASTER, TAG_KERNEL_DATA);

                // Rebuild kernel 2D array
                int[][] kernel = new int[kRows][kCols];
                for (int i = 0; i < kRows; i++) {
                    for (int j = 0; j < kCols; j++) {
                        kernel[i][j] = flatKernel[i * kCols + j];
                    }
                }

                // Build input chunk image
                BufferedImage chunkImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                chunkImg.getRaster().setDataElements(0, 0, width, height, pixels);

                // Apply convolution on entire chunk (including overlap)
                DImgProcMethods proc = new DImgProcMethods();
                BufferedImage processedChunk = proc.applyConvolution(chunkImg, kernel);

                // Convert back to byte array
                byte[] processedPixels = ((java.awt.image.DataBufferByte) processedChunk.getRaster().getDataBuffer()).getData();

                // Send back processed chunk + startY for stitching
                MPI.COMM_WORLD.Send(new int[]{width, height, startY}, 0, 3, MPI.INT, MASTER, TAG_RESULT_DIMENSIONS);
                MPI.COMM_WORLD.Send(processedPixels, 0, processedPixels.length, MPI.BYTE, MASTER, TAG_RESULT_PIXELS);
            }
        }
    }

    public static void stopWorkers(int numWorkers) {
        for (int i = 1; i <= numWorkers; i++) {
            MPI.COMM_WORLD.Send(new byte[0], 0, 0, MPI.BYTE, i, TAG_STOP);
        }
    }
    @Override
    public String toString() {
        return "Distributed Image Processor";
    }
}
