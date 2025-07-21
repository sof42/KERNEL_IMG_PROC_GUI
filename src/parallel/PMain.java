package parallel;

import GUI.GUI;
import Processing.IMGProcessor;
import parallel.PImgProcessing.PImgProcMethods;
public class PMain {
        public static void main(String[] args) {
            IMGProcessor processor = new PImgProcMethods();
            GUI.run(processor);
        }
}
