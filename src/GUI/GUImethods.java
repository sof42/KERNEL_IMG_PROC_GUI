package GUI;

import Constants.CONSTANTS;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class GUImethods {

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

    public static BufferedImage loadImage(String imagePath) {
        try {
            return ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void deleteOutputImages() {
        File outputImagesDirectory = new File(CONSTANTS.OUTPUT_IMAGES_DIRECTORY);

        // Get all files in the directory
        File[] outputImageFiles = outputImagesDirectory.listFiles();

        if (outputImageFiles != null) {
            for (File file : outputImageFiles) {
                try {
                    Files.delete(file.toPath());
                } catch (IOException e) {
                    System.out.println("Failed to delete file: " + file.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }
    }
}
