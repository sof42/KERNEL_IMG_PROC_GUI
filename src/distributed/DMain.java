package distributed;

import Processing.IMGProcessor;
import distributed.DImgProcessing.DImgProcMethods;
import mpi.MPI;
import GUI.GUI;

public class DMain {
    public static void main(String[] args) {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if (size < 2) {
            if (rank == 0) {
                System.err.println("Error: At least 2 MPI processes are required.");
            }
            MPI.Finalize();
            return;
        }

        IMGProcessor processor = new DImgProcMethods();

        if (rank == 0) {
            // Run the GUI on the master process
            GUI.run(processor);

            // Once GUI closes, stop workers
            DImgProcMethods.stopWorkers(size);

        } else {
            // Worker process loop
            DImgProcMethods.workerProcess(rank);
        }

        MPI.Finalize(); // Finalize ONLY after work is fully done
    }
}
