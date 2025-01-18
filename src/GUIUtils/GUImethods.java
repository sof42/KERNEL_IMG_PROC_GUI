package GUIUtils;

import Constants.CONSTANTS;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class GUImethods {
    public static BufferedImage loadImage(String imagePath) {
        try {
            return ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void displayImages(BufferedImage inputImage, BufferedImage outputImage, long runtime) {
        JFrame resultFrame = new JFrame("Image Processing Result");

        // Set the size of the frame based on the output image size
        int frameWidth = Math.max(inputImage.getWidth(), outputImage.getWidth()) * 2 + 60; // Add padding
        int frameHeight = Math.max(inputImage.getHeight(), outputImage.getHeight()) + 120; // Add padding
        resultFrame.setSize(frameWidth, frameHeight);
        resultFrame.setLocationRelativeTo(null);

        JPanel resultPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel inputLabel = new JLabel("Input Image:");
        JLabel outputLabel = new JLabel("Output Image:");

        // Set the preferred size for the JLabels
        inputLabel.setPreferredSize(new Dimension(frameWidth / 2 + 15, 40));
        outputLabel.setPreferredSize(new Dimension(frameWidth / 2 + 15, 40));

        inputLabel.setFont(new Font("Courier", Font.BOLD, 16));
        outputLabel.setFont(new Font("Courier", Font.BOLD, 16));

        JLabel inputImageLabel = new JLabel(new ImageIcon(inputImage));
        JLabel outputImageLabel = new JLabel(new ImageIcon(outputImage));

        // Set the preferred size of the JLabels
        inputImageLabel.setPreferredSize(new Dimension(inputImage.getWidth(), inputImage.getHeight()));
        outputImageLabel.setPreferredSize(new Dimension(outputImage.getWidth(), outputImage.getHeight()));

        JLabel runtimeLabel = new JLabel("Running Time: " + runtime + " milliseconds");
        runtimeLabel.setPreferredSize(new Dimension(frameWidth + 10, 60));
        runtimeLabel.setFont(new Font("Courier", Font.BOLD, 16));

        JButton resetButton = new JButton("Reset");
        resetButton.setFont(new Font("Courier", Font.ITALIC, 14));
        resetButton.addActionListener(e -> {
            GUImethods.deleteOutputImages();
            resultFrame.dispose(); // Close the current frame
        });

        // Add labels and images to the result panel using GridBagConstraints
        gbc.gridy++;
        resultPanel.add(inputLabel, gbc);
        gbc.gridx++;
        resultPanel.add(outputLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        resultPanel.add(inputImageLabel, gbc);
        gbc.gridx++;
        resultPanel.add(outputImageLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        resultPanel.add(runtimeLabel, gbc);

        gbc.gridy++;
        resultPanel.add(resetButton, gbc);

        // Create a JScrollPane and add resultPanel to it
        JScrollPane scrollPane = new JScrollPane(resultPanel);

        // Set up the resultFrame with the JScrollPane
        resultFrame.add(scrollPane);

        resultFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Override close operation
        // Add a WindowListener to handle closing event
        resultFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Delete all files in the outputImages directory
                GUImethods.deleteOutputImages();
                // Exit the application
                System.exit(0);
            }
        });
        resultFrame.setVisible(true);
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
