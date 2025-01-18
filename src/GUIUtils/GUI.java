package GUIUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;
import javax.swing.*;
import Constants.CONSTANTS;
import ImgProcessing.ImgProcMethods;

public class GUI {

    public GUI() {
        JFrame frame = new JFrame("Kernel Image Processing");
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
        gbc.gridy++;

        // Create a panel for image selection
        JPanel imagePanel = new JPanel(new FlowLayout());
        JLabel labelImage = new JLabel("Select Image:");
        imagePanel.add(labelImage);

        File inputImagesDirectory = new File(CONSTANTS.INPUT_IMAGES_DIRECTORY);
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
        JLabel labelKernel = new JLabel("Kernel Type: ");
        labelKernel.setFont(new Font("Courier", Font.BOLD, 18));

        JPanel kernelPanel = new JPanel();
        kernelPanel.setLayout(new BoxLayout(kernelPanel, BoxLayout.Y_AXIS));

        JTextField[][] kernelFields = new JTextField[3][3];

    // Define kernel options
        String[] kernelOptions = {"Ridge Detection", "Edge Detection", "Identity", "Sharpen", "Custom"};
        JComboBox<String> kernelComboBox = new JComboBox<>(kernelOptions);
        kernelComboBox.setFont(new Font("Courier", Font.BOLD, 16));

        // Initialize kernel fields
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                kernelFields[i][j] = new JTextField(4);
                kernelFields[i][j].setFont(new Font("Courier", Font.BOLD, 16));
                kernelFields[i][j].setHorizontalAlignment(JTextField.CENTER);
            }
        }

        // Populate the kernel fields
        populateKernelFields(kernelFields, CONSTANTS.RIDGE_DETECTION_KERNEL, false); // Default to Identity Kernel
        // Add kernel fields to the kernelPanel
        for (int i = 0; i < 3; i++) {
            JPanel rowPanel = new JPanel();
            rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
            for (int j = 0; j < 3; j++) {
                rowPanel.add(kernelFields[i][j]);
            }
            kernelPanel.add(rowPanel);
        }

        kernelComboBox.addActionListener(e -> {
            String selectedKernel = (String) kernelComboBox.getSelectedItem();
            switch (Objects.requireNonNull(selectedKernel)) {
                case "Ridge Detection":
                    populateKernelFields(kernelFields, CONSTANTS.RIDGE_DETECTION_KERNEL, false);
                    break;
                case "Edge Detection":
                    populateKernelFields(kernelFields, CONSTANTS.EDGE_DETECTION_KERNEL, false);
                    break;
                case "Identity":
                    populateKernelFields(kernelFields, CONSTANTS.IDENTITY_KERNEL, false);
                    break;
                case "Sharpen":
                    populateKernelFields(kernelFields, CONSTANTS.SHARPEN_KERNEL, false);
                    break;
                case "Custom":
                    populateKernelFields(kernelFields, CONSTANTS.DEFAULT_KERNEL, true);
                    break;
            }
        });


// Add the dropdown and kernel fields to the panel
        kernelModificationPanel.add(labelKernel);
        kernelModificationPanel.add(kernelComboBox);
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
            String selectedImageName = (String) imageComboBox.getSelectedItem();
            int[][] customKernel = ImgProcMethods.getCustomKernel(kernelFields);

            BufferedImage inputImage = GUImethods.loadImage(CONSTANTS.INPUT_IMAGES_DIRECTORY + selectedImageName);

            assert inputImage != null;
            BufferedImage grayImage = ImgProcMethods.convertToGrayscale(inputImage);

                // Measure the time around the actual image processing operation
                long startTime = System.currentTimeMillis();
                BufferedImage outputImage = ImgProcMethods.applyConvolution(grayImage, customKernel);
                long endTime = System.currentTimeMillis();
                long runtime = endTime - startTime;
                System.out.println("Sequential runtime: " + runtime + " milliseconds");

                int width = inputImage.getWidth();
                int height = inputImage.getHeight();

                System.out.println("Image Size: " + width + " x " + height);

                ImgProcMethods.processImage(selectedImageName, customKernel);

                // Display images in a new frame
                GUImethods.displayImages(inputImage, outputImage, runtime);
        });

        labelImage.setFont(new Font("Courier", Font.BOLD, 18));

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Override close operation
        // Add a WindowListener to handle closing event
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Delete all files in the outputImages directory
                GUImethods.deleteOutputImages();
                // Exit the application
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    private void populateKernelFields(JTextField[][] kernelFields, int[][] matrix, boolean editable) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                kernelFields[i][j].setText(String.valueOf(matrix[i][j]));
                kernelFields[i][j].setEditable(editable);
            }
        }
    }

    public static void run() {
        SwingUtilities.invokeLater(() -> new GUI());
    }
}
