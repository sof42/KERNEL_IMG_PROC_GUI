package sequential.ImgProcessing;

import Processing.IMGProcessor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImgProcMethods implements IMGProcessor {

    // converts a color image to grayscale using weighted average
    @Override
    public BufferedImage convertToGrayscale(BufferedImage colorImage) {
        int width = colorImage.getWidth();
        int height = colorImage.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(colorImage.getRGB(x, y));

                // just using the standard formula for luminance
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                Color grayColor = new Color(gray, gray, gray);
                grayImage.setRGB(x, y, grayColor.getRGB());
            }
        }
        return grayImage;
    }

    // applies a basic convolution using the given kernel (no edge handling beyond bounds check)
    @Override
    public BufferedImage applyConvolution(BufferedImage inputImage, int[][] kernel) {
        int kernelSize = kernel.length;
        int halfKernelSize = kernelSize / 2;
        BufferedImage outputImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < inputImage.getHeight(); y++) {
            for (int x = 0; x < inputImage.getWidth(); x++) {
                int accumulator = 0;

                // loop through the kernel
                for (int i = 0; i < kernelSize; i++) {
                    for (int j = 0; j < kernelSize; j++) {
                        int pixelX = x + j - halfKernelSize;
                        int pixelY = y + i - halfKernelSize;

                        // make sure we’re within bounds
                        if (pixelX >= 0 && pixelX < inputImage.getWidth() && pixelY >= 0 && pixelY < inputImage.getHeight()) {
                            int pixel = new Color(inputImage.getRGB(pixelX, pixelY)).getRed();
                            int kernelValue = kernel[i][j];
                            accumulator += pixel * kernelValue;
                        }
                    }
                }

                // clamp result so it stays within 0–255
                int outputPixel = Math.min(255, Math.max(0, accumulator));
                outputImage.setRGB(x, y, new Color(outputPixel, outputPixel, outputPixel).getRGB());
            }
        }
        return outputImage;
    }

    // runs the full processing: loads image, grayscales it, applies kernel, then saves it
    @Override
    public void processImage(String inputFilePath, String outputFilePath, int[][] kernel) {
        try {
            // load image from file
            BufferedImage inputImage = ImageIO.read(new File(inputFilePath));

            // convert to grayscale before filtering
            BufferedImage grayImage = convertToGrayscale(inputImage);

            // apply kernel convolution
            BufferedImage outputImage = applyConvolution(grayImage, kernel);

            // write processed image to file
            ImageIO.write(outputImage, "jpg", new File(outputFilePath));
        } catch (IOException e) {
            // basic error handling
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Sequential Image Processor";
    }
}
