import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Main {
    private static final String INPUT_IMAGES_DIRECTORY = "inputImages/";
    private static final String OUTPUT_IMAGES_DIRECTORY = "outputImages/";
    private static final int[][] DEFAULT_KERNEL = {
            {-1, -1, -1},
            {-1, 8, -1},
            {-1, -1, -1}
    };

    public static void main(String[] args) {

        JFrame frame = new JFrame("Kernel Image Processing (GUI version)");
        frame.setSize(800, 400);

        // Center the frame on the screen
        frame.setLocationRelativeTo(null);

        JPanel main = new JPanel(new FlowLayout());

        // Create a main panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        main.add(mainPanel);
        frame.add(main);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.BASELINE; // Align to the top-left

        // Create a panel for processing option
        JPanel processingPanel = new JPanel(new FlowLayout());
        JLabel labelProcessingOption = new JLabel("Select Processing Option:");
        processingPanel.add(labelProcessingOption);

        String[] processingOptions = {"Sequential", "Parallel", "Distributed"};
        JComboBox<String> processingComboBox = new JComboBox<>(processingOptions);
        processingComboBox.setFont(new Font("Courier", Font.ITALIC, 20));
        processingPanel.add(processingComboBox);

        mainPanel.add(processingPanel, gbc);

        gbc.gridy++;

        // Create a panel for image selection
        JPanel imagePanel = new JPanel(new FlowLayout());
        JLabel labelImage = new JLabel("Select Image:");
        imagePanel.add(labelImage);

        File inputImagesDirectory = new File(INPUT_IMAGES_DIRECTORY);
        File[] inputImageFiles = inputImagesDirectory.listFiles();

        assert inputImageFiles != null; // to suppress warning
        String[] imageNames = new String[inputImageFiles.length];
        for (int i = 0; i < inputImageFiles.length; i++) {
            imageNames[i] = inputImageFiles[i].getName();
        }

        JComboBox<String> imageComboBox = new JComboBox<>(imageNames);
        imageComboBox.setFont(new Font("Courier", Font.ITALIC, 20));
        imagePanel.add(imageComboBox);

        mainPanel.add(imagePanel, gbc);

        gbc.gridy++;

        // Create a panel for kernel modification
        JPanel kernelModificationPanel = new JPanel(new FlowLayout());
        JLabel labelKernel = new JLabel("Modify Kernel: ");
        labelKernel.setFont(new Font("Courier", Font.BOLD, 18));

        JPanel kernelPanel = new JPanel();
        kernelPanel.setLayout(new BoxLayout(kernelPanel, BoxLayout.Y_AXIS));

        JTextField[][] kernelFields = new JTextField[3][3];

        for (int i = 0; i < 3; i++) {
            // Create a panel for each row
            JPanel rowPanel = new JPanel();
            rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));

            for (int j = 0; j < 3; j++) {
                kernelFields[i][j] = new JTextField(4);
                kernelFields[i][j].setFont(new Font("Courier", Font.BOLD, 16));
                kernelFields[i][j].setHorizontalAlignment(JTextField.CENTER);
                // Set the default values from the DEFAULT_KERNEL
                kernelFields[i][j].setText(Integer.toString(DEFAULT_KERNEL[i][j]));
                rowPanel.add(kernelFields[i][j]);
            }

            kernelPanel.add(rowPanel);
        }

        kernelModificationPanel.add(labelKernel);
        kernelModificationPanel.add(kernelPanel);

        mainPanel.add(kernelModificationPanel, gbc);

        gbc.gridy++;

        // Create a panel for the "Process" button
        JPanel processButtonPanel = new JPanel(new FlowLayout());
        JButton processButton = new JButton("Process");
        processButton.setFont(new Font("Courier", Font.BOLD, 16));
        processButtonPanel.add(processButton);

        mainPanel.add(processButtonPanel, gbc);

        // Action listener for the "Process" button
        processButton.addActionListener((ActionEvent e) -> {
            String selectedOption = (String) processingComboBox.getSelectedItem();
            String selectedImageName = (String) imageComboBox.getSelectedItem();
            int[][] customKernel = getCustomKernel(kernelFields);

            BufferedImage inputImage = loadImage(INPUT_IMAGES_DIRECTORY + selectedImageName);

            assert inputImage != null;
            BufferedImage grayImage = convertToGrayscale(inputImage);

            if (Objects.equals(selectedOption, "Sequential")) {
                // Measure the time around the actual image processing operation
                long startTime = System.currentTimeMillis();
                BufferedImage outputImage = applyConvolution(grayImage, customKernel);
                long endTime = System.currentTimeMillis();
                long runtime = endTime - startTime;
                System.out.println(selectedOption + " runtime: " + runtime + " milliseconds");

                int width = inputImage.getWidth();
                int height = inputImage.getHeight();

                System.out.println("Image Size: " + width + " x " + height);

                processImage(selectedImageName, customKernel);

                // Display images in a new frame
                displayImages(inputImage, outputImage, runtime);
            } else if (Objects.equals(selectedOption, "Parallel") || Objects.equals(selectedOption, "Distributed")) {
                JOptionPane.showMessageDialog(null, selectedOption + " option needs to be programmed :)");
                // Don't proceed further
            } else {
                JOptionPane.showMessageDialog(null, "Invalid processing option selected");
                // Don't proceed further
            }
        });


        labelProcessingOption.setFont(new Font("Courier", Font.BOLD, 18));
        labelImage.setFont(new Font("Courier", Font.BOLD, 18));

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Override close operation
        // Add a WindowListener to handle closing event
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Delete all files in the outputImages directory
                deleteOutputImages();
                // Exit the application
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    private static BufferedImage loadImage(String imagePath) {
        try {
            return ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void displayImages(BufferedImage inputImage, BufferedImage outputImage, long runtime) {
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
            deleteOutputImages();
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
                deleteOutputImages();
                // Exit the application
                System.exit(0);
            }
        });
        resultFrame.setVisible(true);
    }

    private static int[][] getCustomKernel(JTextField[][] kernelFields) {
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

    private static void processImage(String selectedImage, int[][] customKernel) {
            String inputImagePath = INPUT_IMAGES_DIRECTORY + selectedImage;

            try {
                BufferedImage inputImage = ImageIO.read(new File(inputImagePath));

                BufferedImage grayImage = convertToGrayscale(inputImage);

                BufferedImage outputImage = applyConvolution(grayImage, customKernel);

                String outputImagePath = OUTPUT_IMAGES_DIRECTORY + "output_" + selectedImage;

                ImageIO.write(outputImage, "jpg", new File(outputImagePath));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
    }

    private static BufferedImage convertToGrayscale(BufferedImage colorImage) {
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

    private static BufferedImage applyConvolution(BufferedImage inputImage, int[][] kernel) {
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

    private static void deleteOutputImages() {
        File outputImagesDirectory = new File(OUTPUT_IMAGES_DIRECTORY);

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
