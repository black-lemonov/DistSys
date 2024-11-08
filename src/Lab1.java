import mpi.MPI;


/**
 * При запуске четного числа процессов
 * четные процессы отправляют сообщения нечетным
 */
public class Lab1 {
    public static void main(String[] args) {
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
}
