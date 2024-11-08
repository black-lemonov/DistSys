import mpi.MPI;

/**
 * Пересылка сообщений между процессами по кругу
 * в блокирующем режиме 
 */
public class Lab2Blocking {
    public static void main(String[] args) {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        int nextRank = (rank + 1 == size)? 0: rank + 1; 
        int prevRank = (rank == 0)? size - 1: rank - 1;
        int msg[] = {rank};
        int tag = 0;
        if (rank == 0) {
            MPI.COMM_WORLD.Send(msg, 0, 1, MPI.INT, nextRank, tag);
            System.out.println(
                String.format("Process <%d> sends %d", rank, msg[0])
            );
            MPI.COMM_WORLD.Recv(msg, 0, 1, MPI.INT, prevRank, tag);
            System.out.println(
                String.format("Process <%d> receives %d", rank, msg[0])
            );
        } else {
            MPI.COMM_WORLD.Recv(msg, 0, 1, MPI.INT, prevRank, tag);
            System.out.println(
                String.format("Process <%d> receives %d", rank, msg[0])
            );
            msg[0] += rank;
            MPI.COMM_WORLD.Send(msg, 0, 1, MPI.INT, nextRank, tag);
            System.out.println(
                String.format("Process <%d> sends %d", rank, msg[0])
            );
        }
    }
}
