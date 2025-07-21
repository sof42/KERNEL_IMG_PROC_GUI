package Processing;

import java.awt.image.BufferedImage;

public interface IMGProcessor {
        BufferedImage convertToGrayscale(BufferedImage colorImage);

        BufferedImage applyConvolution(BufferedImage inputImage, int[][] kernel);

        void processImage(String inputFilePath, String outputFilePath, int[][] kernel);
}
