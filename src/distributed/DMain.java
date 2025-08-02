package distributed;

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

        if (rank == 0) {
            // Master process runs GUI and manages workers
            DImgProcMethods processor = new DImgProcMethods();

            // Run the GUI on the master process (this will block until GUI closes)
            GUI.run(processor);

            // After GUI closes, send stop signals to workers
            processor.stopWorkers(size - 1);

        } else {
            // Worker processes create their own instance and run the worker loop
            DImgProcMethods processor = new DImgProcMethods();
            processor.workerProcess(rank);
        }

        MPI.Finalize();
    }
}
