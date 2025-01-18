package ImgProcessing;

import Constants.CONSTANTS;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ImgProcMethods {

    public static int[][] getCustomKernel(JTextField[][] kernelFields) {
        int[][] customKernel = new int[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                try {
                    customKernel[i][j] = Integer.parseInt(kernelFields[i][j].getText());
                } catch (NumberFormatException e) {
                    // Handle invalid input (non-integer)
                    customKernel[i][j] = 0;
                }
            }
        }
        return customKernel;
    }

    public static void processImage(String selectedImage, int[][] customKernel) {
        String inputImagePath = CONSTANTS.INPUT_IMAGES_DIRECTORY + selectedImage;

        try {
            BufferedImage inputImage = ImageIO.read(new File(inputImagePath));

            BufferedImage grayImage = convertToGrayscale(inputImage);

            BufferedImage outputImage = applyConvolution(grayImage, customKernel);

            String outputImagePath = CONSTANTS.OUTPUT_IMAGES_DIRECTORY + "output_" + selectedImage;

            ImageIO.write(outputImage, "jpg", new File(outputImagePath));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static BufferedImage convertToGrayscale(BufferedImage colorImage) {
        int width = colorImage.getWidth();
        int height = colorImage.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(colorImage.getRGB(x, y));

                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();

                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                Color grayColor = new Color(gray, gray, gray);
                int grayRgb = grayColor.getRGB();

                grayImage.setRGB(x, y, grayRgb);
            }
        }

        return grayImage;
    }

    public static BufferedImage applyConvolution(BufferedImage inputImage, int[][] kernel) {
        int kernelSize = kernel.length;
        int halfKernelSize = kernelSize / 2;
        BufferedImage outputImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < inputImage.getHeight(); y++) {
            for (int x = 0; x < inputImage.getWidth(); x++) {

                int accumulator = 0;

                for (int i = 0; i < kernelSize; i++) {
                    for (int j = 0; j < kernelSize; j++) {
                        int pixelX = x + j - halfKernelSize;
                        int pixelY = y + i - halfKernelSize;

                        if (pixelX >= 0 && pixelX < inputImage.getWidth() && pixelY >= 0 && pixelY < inputImage.getHeight()) {
                            int pixel = new Color(inputImage.getRGB(pixelX, pixelY)).getRed();
                            int kernelValue = kernel[i][j];
                            accumulator += pixel * kernelValue;
                        }
                    }
                }

                int outputPixel = Math.min(255, Math.max(0, accumulator));
                outputImage.setRGB(x, y, new Color(outputPixel, outputPixel, outputPixel).getRGB());
            }
        }

        return outputImage;
    }
}
