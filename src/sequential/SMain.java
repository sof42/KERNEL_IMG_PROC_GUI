package sequential;

import GUI.GUI;
import Processing.IMGProcessor;
import sequential.ImgProcessing.ImgProcMethods;

public class SMain {
    public static void main(String[] args) {
        IMGProcessor processor = new ImgProcMethods();
        GUI.run(processor);
    }
}
