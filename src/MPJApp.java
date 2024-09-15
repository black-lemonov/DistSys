/** 
 * Лабораторные работы по MPJ
 */

import mpi.*;



public class MPJApp {
    
    /**
     * Run with "mpjrun -np [processes number] [class name]"
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        task2NonBlocking(args);
    }

    /**
     * При запуске четного числа процессов
     * четные процессы отправляют сообщения нечетным
     * @param args параметры mpj
     */
    public static void task1(String[] args) {
        MPI.Init(args);
        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        if (size % 2 == 0) {
            char msg[] = {'H', 'e', 'l', 'l', 'o'};
            char buf[] = new char[msg.length];
            int tag = 0;
            if (me % 2 == 0) {
                // send next
                MPI.COMM_WORLD.Send(msg, 0, msg.length, MPI.CHAR, me + 1, tag);
                System.out.println(
                    String.format("Process %d: send message \"%s\" to %d", me, String.valueOf(msg), me + 1)
                );
            } else {
                MPI.COMM_WORLD.Recv(buf, 0, buf.length, MPI.CHAR, me - 1, tag);
                System.out.println(
                    String.format("Proccess %d: recieved message \"%s\" from %d", me, String.valueOf(buf), me - 1)
                );
            }
        }
        MPI.Finalize();
    }

    /**
     * Пересылка сообщений между процессами по кругу
     * в блокирующем режиме 
     * @param args параметры mpj
     */
    public static void task2Blocking(String[] args) {
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

    /**
     * Пересылка сообщений в асинхронном режиме
     * @param args параметры mpj
     */
    public static void task2NonBlocking(String[] args) {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        int nextRank = (rank + 1 == size)? 0: rank + 1; 
        int prevRank = (rank == 0)? size - 1: rank - 1;
        int msg[] = {rank};
        int tag = 0;
        if (rank == 0) {
            MPI.COMM_WORLD.Isend(msg, 0, 1, MPI.INT, nextRank, tag);
            System.out.println(
                String.format("Process <%d> sends %d", rank, msg[0])
            );
            Request r = MPI.COMM_WORLD.Irecv(msg, 0, 1, MPI.INT, prevRank, tag);
            r.Wait();
            System.out.println(
                String.format("Process <%d> receives %d", rank, msg[0])
            );
        } else {
            Request r = MPI.COMM_WORLD.Irecv(msg, 0, 1, MPI.INT, prevRank, tag);
            r.Wait();
            System.out.println(
                String.format("Process <%d> receives %d", rank, msg[0])
            );
            msg[0] += rank;
            MPI.COMM_WORLD.Isend(msg, 0, 1, MPI.INT, nextRank, tag);
            System.out.println(
                String.format("Process <%d> sends %d", rank, msg[0])
            );
        }
    }

}
