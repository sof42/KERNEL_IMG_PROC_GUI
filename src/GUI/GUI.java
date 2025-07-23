package GUI;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import javax.swing.*;
import Constants.CONSTANTS;
import Processing.IMGProcessor;

public class GUI {
    private final JLabel originalImageLabel;
    private final JLabel processedImageLabel;
    private final JPanel imageDisplayPanel;
    private final JLabel runtimeLabel;
    private final IMGProcessor processor;
    private File selectedImageFile; // currently selected image file
    private static final CountDownLatch latch = new CountDownLatch(1);


    private JFrame frame;

    public GUI(IMGProcessor processor) {
        this.processor = processor;
        this.selectedImageFile = null; // no image initially

        frame = new JFrame("Kernel Image Processing");
        frame.setSize(800, 800);
        frame.setLocationRelativeTo(null);

        JPanel main = new JPanel(new FlowLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        main.add(mainPanel);
        frame.add(main);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.BASELINE;

        // Panel for image selection
        JPanel imagePanel = new JPanel(new FlowLayout());
        JLabel labelImage = new JLabel("Selected Image: ");
        labelImage.setFont(new Font("Courier", Font.BOLD, 18));
        imagePanel.add(labelImage);

        // Label to show selected image filename
        JLabel selectedImageLabel = new JLabel("No image selected");
        selectedImageLabel.setFont(new Font("Courier", Font.ITALIC, 16));
        imagePanel.add(selectedImageLabel);

        // Button to choose image file
        JButton chooseImageButton = new JButton("Choose Image");
        chooseImageButton.setFont(new Font("Courier", Font.BOLD, 14));
        imagePanel.add(chooseImageButton);

        mainPanel.add(imagePanel, gbc);
        gbc.gridy++;

        // Kernel modification panel (same as your original)
        JPanel kernelModificationPanel = new JPanel(new FlowLayout());
        JLabel labelKernel = new JLabel("Kernel Type: ");
        labelKernel.setFont(new Font("Courier", Font.BOLD, 18));

        JPanel kernelPanel = new JPanel();
        kernelPanel.setLayout(new BoxLayout(kernelPanel, BoxLayout.Y_AXIS));

        JTextField[][] kernelFields = new JTextField[3][3];

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

        populateKernelFields(kernelFields, CONSTANTS.RIDGE_DETECTION_KERNEL, false);

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
            switch (selectedKernel) {
                case "Ridge Detection" -> populateKernelFields(kernelFields, CONSTANTS.RIDGE_DETECTION_KERNEL, false);
                case "Edge Detection" -> populateKernelFields(kernelFields, CONSTANTS.EDGE_DETECTION_KERNEL, false);
                case "Identity" -> populateKernelFields(kernelFields, CONSTANTS.IDENTITY_KERNEL, false);
                case "Sharpen" -> populateKernelFields(kernelFields, CONSTANTS.SHARPEN_KERNEL, false);
                case "Custom" -> populateKernelFields(kernelFields, CONSTANTS.DEFAULT_KERNEL, true);
            }
        });

        kernelModificationPanel.add(labelKernel);
        kernelModificationPanel.add(kernelComboBox);
        kernelModificationPanel.add(kernelPanel);

        mainPanel.add(kernelModificationPanel, gbc);
        gbc.gridy++;

        // Process button panel
        JPanel processButtonPanel = new JPanel(new FlowLayout());
        JButton processButton = new JButton("Process");
        processButton.setFont(new Font("Courier", Font.BOLD, 16));
        processButtonPanel.add(processButton);

        mainPanel.add(processButtonPanel, gbc);
        gbc.gridy++;

        imageDisplayPanel = new JPanel(new FlowLayout());

        originalImageLabel = new JLabel();
        processedImageLabel = new JLabel();

        imageDisplayPanel.add(originalImageLabel);
        imageDisplayPanel.add(processedImageLabel);

        mainPanel.add(imageDisplayPanel, gbc);
        gbc.gridy++;

        runtimeLabel = new JLabel("Runtime: N/A");
        runtimeLabel.setFont(new Font("Courier", Font.BOLD, 16));
        mainPanel.add(runtimeLabel, gbc);
        gbc.gridy++;

        // Choose image button action: open file chooser to pick image file
        chooseImageButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(CONSTANTS.INPUT_IMAGES_DIRECTORY);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) return true;
                    String name = f.getName().toLowerCase();
                    return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".bmp");
                }

                @Override
                public String getDescription() {
                    return "Image Files (.png, .jpg, .jpeg, .bmp)";
                }
            });

            int returnVal = chooser.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                selectedImageFile = chooser.getSelectedFile();
                selectedImageLabel.setText(selectedImageFile.getName());
                clearImages();
                runtimeLabel.setText("Runtime: N/A");
            }
        });

        processButton.addActionListener(e -> {
            if (selectedImageFile == null) {
                JOptionPane.showMessageDialog(frame, "Please select an image first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int[][] customKernel = GUImethods.getCustomKernel(kernelFields);
            String inputPath = selectedImageFile.getAbsolutePath();

            BufferedImage inputImage = GUImethods.loadImage(inputPath);
            if (inputImage == null) {
                JOptionPane.showMessageDialog(frame, "Failed to load the selected image.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            processButton.setEnabled(false);
            runtimeLabel.setText("Processing...");

            new SwingWorker<BufferedImage, Void>() {
                long startTime;
                long endTime;

                @Override
                protected BufferedImage doInBackground() throws Exception {
                    startTime = System.currentTimeMillis();
                    BufferedImage outputImage = processor.processImage(inputPath, customKernel);
                    endTime = System.currentTimeMillis();
                    return outputImage;
                }

                @Override
                protected void done() {
                    try {
                        BufferedImage outputImage = get(); // get processed image from doInBackground
                        long runtime = endTime - startTime;

                        int width = inputImage.getWidth();
                        int height = inputImage.getHeight();

                        runtimeLabel.setText(processor.toString() + " Runtime: " + runtime + " ms for Image Size: " + width + " x " + height);
                        System.out.println(processor.toString() + " Runtime: " + runtime + " ms for Image Size: " + width + " x " + height);

                        // Scale and display input and output images
                        int displayWidth = 300;
                        int displayHeight = 300;

                        Image scaledInput = inputImage.getScaledInstance(displayWidth, displayHeight, Image.SCALE_SMOOTH);
                        ImageIcon inputIcon = new ImageIcon(scaledInput);

                        Image scaledOutput = outputImage.getScaledInstance(displayWidth, displayHeight, Image.SCALE_SMOOTH);
                        ImageIcon outputIcon = new ImageIcon(scaledOutput);

                        originalImageLabel.setIcon(inputIcon);
                        processedImageLabel.setIcon(outputIcon);

                        originalImageLabel.setText("Original (rescaled)");
                        originalImageLabel.setHorizontalTextPosition(JLabel.CENTER);
                        originalImageLabel.setVerticalTextPosition(JLabel.BOTTOM);

                        processedImageLabel.setText("Processed (rescaled)");
                        processedImageLabel.setHorizontalTextPosition(JLabel.CENTER);
                        processedImageLabel.setVerticalTextPosition(JLabel.BOTTOM);

                        imageDisplayPanel.revalidate();
                        imageDisplayPanel.repaint();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame, "Error during image processing.", "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        processButton.setEnabled(true);
                    }
                }
            }.execute();
        });



        // On window close, delete output images
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                GUImethods.deleteOutputImages();
                latch.countDown();   // Signal that GUI is closing, release the main thread
                frame.dispose();
                // Don't call System.exit(0) here, let the main thread handle shutdown after latch.await()
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

    private void clearImages() {
        originalImageLabel.setIcon(null);
        originalImageLabel.setText("");
        processedImageLabel.setIcon(null);
        processedImageLabel.setText("");
    }

    public static void run(IMGProcessor processor) {
        SwingUtilities.invokeLater(() -> new GUI(processor));
        try {
            latch.await();  // Wait until the GUI window closes
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
