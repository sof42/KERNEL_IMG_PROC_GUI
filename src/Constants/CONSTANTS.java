package Constants;

public class CONSTANTS {
        public static final String INPUT_IMAGES_DIRECTORY = "inputImages/";
        public static final String OUTPUT_IMAGES_DIRECTORY = "outputImages/";
        public static final int[][] RIDGE_DETECTION_KERNEL = {
                {-1, -1, -1},
                {-1, 8, -1},
                {-1, -1, -1}
        };

        public static final int[][] EDGE_DETECTION_KERNEL = {
                {0, -1, 0},
                {-1, 4, -1},
                {0, -1, 0}
        };

        public static final int[][] IDENTITY_KERNEL = {
                {0, 0, 0},
                {0, 1, 0},
                {0, 0, 0}
        };

        public static final int[][] SHARPEN_KERNEL = {
                {0, -1, 0},
                {-1, 5, -1},
                {0, -1, 0}
        };

        public static final int[][] DEFAULT_KERNEL = {
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 1}
        };
}
