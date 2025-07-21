package parallel.PImgProcessing;

import Processing.IMGProcessor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

public class PImgProcMethods implements IMGProcessor {

    @Override
    public BufferedImage convertToGrayscale(BufferedImage colorImage) {
        int width = colorImage.getWidth();
        int height = colorImage.getHeight();

        // create new image to store grayscale pixels
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // run parallel processing using ForkJoinPool sized to available CPUs
        try (ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors())) {
            pool.submit(() ->
                    // we flatten 2D image pixels into 1D stream and process in parallel
                    IntStream.range(0, width * height).parallel().forEach(i -> {
                        int x = i % width;       // x coordinate of pixel
                        int y = i / width;       // y coordinate of pixel
                        Color color = new Color(colorImage.getRGB(x, y));
                        int r = color.getRed();
                        int g = color.getGreen();
                        int b = color.getBlue();
                        // convert to grayscale using weighted formula
                        int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                        Color grayColor = new Color(gray, gray, gray);
                        grayImage.setRGB(x, y, grayColor.getRGB());
                    })
            ).join();  // wait until all pixels are processed
        }

        return grayImage;
    }

    @Override
    public BufferedImage applyConvolution(BufferedImage inputImage, int[][] kernel) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int kernelSize = kernel.length;
        int halfKernel = kernelSize / 2;

        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // same parallel approach here for the convolution operation
        try (ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors())) {
            pool.submit(() ->
                    IntStream.range(0, width * height).parallel().forEach(i -> {
                        int x = i % width;
                        int y = i / width;

                        int accumulator = 0;

                        // apply kernel to pixel neighborhood
                        for (int ky = 0; ky < kernelSize; ky++) {
                            for (int kx = 0; kx < kernelSize; kx++) {
                                int pixelX = x + kx - halfKernel;
                                int pixelY = y + ky - halfKernel;

                                // check boundaries so we don’t go out of the image
                                if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                                    int pixel = new Color(inputImage.getRGB(pixelX, pixelY)).getRed();
                                    int kernelValue = kernel[ky][kx];
                                    accumulator += pixel * kernelValue;
                                }
                            }
                        }

                        // clamp value between 0-255 so it fits in grayscale
                        int outputPixel = Math.min(255, Math.max(0, accumulator));
                        Color outColor = new Color(outputPixel, outputPixel, outputPixel);
                        outputImage.setRGB(x, y, outColor.getRGB());
                    })
            ).join();
        }

        return outputImage;
    }

    @Override
    public void processImage(String inputFilePath, String outputFilePath, int[][] kernel) {
        try {
            // read input image from file
            BufferedImage inputImage = ImageIO.read(new File(inputFilePath));

            // first convert to grayscale (parallelized)
            BufferedImage grayImage = convertToGrayscale(inputImage);

            // then apply convolution with given kernel (also parallelized)
            BufferedImage outputImage = applyConvolution(grayImage, kernel);

            // save processed image to output file
            ImageIO.write(outputImage, "jpg", new File(outputFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Parallel Image Processor";
    }
}
