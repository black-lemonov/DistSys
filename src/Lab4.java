import mpi.MPI;
import mpi.MPIException;
import mpi.Request;

import java.nio.ByteBuffer;
import java.util.Random;

public class Lab4 {
    private int n1, n2, n3;

    private int[][] matrixA, matrixB, resultMatrix;

    public static void main(String[] args) throws Exception, MPIException {
        int size = 1000;
        Lab4 mult = new Lab4(size, size, size);
        mult.initMatrices();
        long startTime = System.currentTimeMillis();
        try {
            mult.multiplyBroadcast(args);
        } finally {
            int rank = MPI.COMM_WORLD.Rank();
            if (rank == 0) {
                long endTime = System.currentTimeMillis();
                System.out.println(String.format("Time: %d (ms)", endTime - startTime));
                System.out.println(mult.equalsActual()? "correct!": "false");
            }
        }
    }

    private boolean equalsActual() {
        var actual = multiplyMatrices(matrixA, matrixB);
        for (int i = 0; i != actual.length; ++i) {
            for (int j = 0; j != actual[i].length; ++j) {
                if (actual[i][j] != resultMatrix[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    private int[][] multiplyMatrices(int[][] A, int[][] B) {
        int rowsA = A.length;
        int colsA = A[0].length;
        int rowsB = B.length;
        int colsB = B[0].length;

        if (colsA != rowsB) {
            throw new IllegalArgumentException("Matrices should have the same size!");
        }

        int[][] result = new int[rowsA][colsB];

        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                for (int k = 0; k < colsA; k++) {
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
        }

        return result;
    }

    /**
     * Конструктор для операции перемножения матриц
     * Матрица A имеет размер n1 x n2
     * Матрица B имеет размер n2 x n3
     * Результирующая матрица будет иметь размер n1 x n3
     *
     */
    public Lab4(int n1, int n2, int n3) {
        this.n1 = n1;
        this.n2 = n2;
        this.n3 = n3;
        matrixA = new int[n1][n2];
        matrixB = new int[n2][n3];
        resultMatrix = new int[n1][n3];
    }

    public void initMatrices() {
        fillMatrixByRandom(matrixA);
        fillMatrixByRandom(matrixB);
    }

    public void multiplyBlocking(String[] args) {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        // Учитываем случай, когда n1 не делится на количество процессов
        int rowsPerProc = (n1 + (size - 2)) / (size - 1);

        if (rank == 0) {
            System.out.println("Process <0> Broadcasting matrices to workers.");

            // Отправка строк матрицы A процессам
            for (int i = 1; i < size; i++) {
                for (int j = 0; j < rowsPerProc; j++) {
                    int row = (i - 1) * rowsPerProc + j;
                    if (row < n1) { // Отправляем только существующие строки
                        MPI.COMM_WORLD.Send(matrixA[row], 0, n2, MPI.INT, i, 0);
                        System.out.println(String.format("Process <0> sent row %d of matrix A to process <%d>", row, i));
                    }
                }
            }

            // Отправка всей матрицы B всем процессам
            for (int i = 1; i < size; i++) {
                for (int rowIdx = 0; rowIdx < n2; rowIdx++) {
                    MPI.COMM_WORLD.Send(matrixB[rowIdx], 0, n3, MPI.INT, i, 1);
                }
                System.out.println(String.format("Process <0> sent entire matrix B to process <%d>", i));
            }
        } else {
            // Получение строк матрицы A
            int[][] localRows = new int[rowsPerProc][n2];
            for (int j = 0; j < rowsPerProc; j++) {
                int globalRow = (rank - 1) * rowsPerProc + j;
                if (globalRow < n1) { // Получаем только существующие строки
                    MPI.COMM_WORLD.Recv(localRows[j], 0, n2, MPI.INT, 0, 0);
                    System.out.println(String.format("Process <%d> received row %d of matrix A.", rank, globalRow));
                }
            }

            // Получение всей матрицы B
            int[][] localB = new int[n2][n3];
            for (int i = 0; i < n2; i++) {
                MPI.COMM_WORLD.Recv(localB[i], 0, n3, MPI.INT, 0, 1);
            }
            System.out.println(String.format("Process <%d> received entire matrix B.", rank));

            // Умножение строк матрицы A на столбцы матрицы B
            int[][] localResult = new int[rowsPerProc][n3];
            for (int i = 0; i < rowsPerProc; i++) {
                int globalRow = (rank - 1) * rowsPerProc + i;
                if (globalRow < n1) { // Умножаем только существующие строки
                    for (int j = 0; j < n3; j++) {
                        localResult[i][j] = 0;
                        for (int k = 0; k < n2; k++) {
                            localResult[i][j] += localRows[i][k] * localB[k][j];
                        }
                    }
                }
            }
            System.out.println(String.format("Process <%d> computed local result.", rank));

            // Отправка локальных результатов обратно процессу 0
            for (int i = 0; i < rowsPerProc; i++) {
                int globalRow = (rank - 1) * rowsPerProc + i;
                if (globalRow < n1) {
                    MPI.COMM_WORLD.Send(localResult[i], 0, n3, MPI.INT, 0, 2);
                    System.out.println(String.format("Process <%d> sent local result for row %d to process <0>.",rank, globalRow));
                }
            }
        }

        // Сбор результатов на процессе 0
        if (rank == 0) {
            for (int i = 1; i < size; i++) {
                for (int j = 0; j < rowsPerProc; j++) {
                    int row = (i - 1) * rowsPerProc + j;
                    if (row < n1) { // Собираем только существующие строки
                        MPI.COMM_WORLD.Recv(resultMatrix[row], 0, n3, MPI.INT, i, 2);
                        System.out.println(String.format("Process <0> Received result for row %d from process <%d> .", row, i));
                    }
                }
            }
        }

        MPI.Finalize();
    }

    public void multiplyNonBlocking(String[] args) {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        // Учитываем остаточные строки
        int rowsPerProc = (n1 + (size - 2)) / (size - 1);

        if (rank == 0) {
            System.out.println("Process <0>: Broadcasting matrices to workers.");

            // Неблокирующее отправление строк матрицы A
            for (int i = 1; i < size; i++) {
                for (int j = 0; j < rowsPerProc; j++) {
                    int row = (i - 1) * rowsPerProc + j;
                    if (row < n1) {
                        MPI.COMM_WORLD.Isend(matrixA[row], 0, n2, MPI.INT, i, 0);
                        System.out.println(String.format("Process <0> sent row %d of matrix A to process <%d>", row, i));
                    }
                }
            }

            // Неблокирующее отправление всей матрицы B
            for (int i = 1; i < size; i++) {
                for (int rowIdx = 0; rowIdx < n2; rowIdx++) {
                    MPI.COMM_WORLD.Isend(matrixB[rowIdx], 0, n3, MPI.INT, i, 1);
                }
                System.out.println(String.format("Process <0> sent entire matrix B to process <%d>", i));
            }
        } else {
            // Неблокирующее получение строк матрицы A
            int[][] localRows = new int[rowsPerProc][n2];
            Request[] requestsA = new Request[rowsPerProc];
            for (int j = 0; j < rowsPerProc; j++) {
                int globalRow = (rank - 1) * rowsPerProc + j;
                if (globalRow < n1) {
                    requestsA[j] = MPI.COMM_WORLD.Irecv(localRows[j], 0, n2, MPI.INT, 0, 0);
                }
            }

            // Неблокирующее получение всей матрицы B
            int[][] localB = new int[n2][n3];
            Request[] requestsB = new Request[n2];
            for (int i = 0; i < n2; i++) {
                requestsB[i] = MPI.COMM_WORLD.Irecv(localB[i], 0, n3, MPI.INT, 0, 1);
            }

            // Ожидание завершения всех операций приема
            Request.Waitall(requestsA);
            Request.Waitall(requestsB);

            System.out.println(String.format("Process <%d> received all data.", rank));

            // Умножение строк матрицы A на столбцы матрицы B
            int[][] localResult = new int[rowsPerProc][n3];
            for (int i = 0; i < rowsPerProc; i++) {
                int globalRow = (rank - 1) * rowsPerProc + i;
                if (globalRow < n1) {
                    for (int j = 0; j < n3; j++) {
                        localResult[i][j] = 0;
                        for (int k = 0; k < n2; k++) {
                            localResult[i][j] += localRows[i][k] * localB[k][j];
                        }
                    }
                }
            }
            System.out.println(String.format("Process <%d> computed local result.", rank));

            // Неблокирующее отправление локального результата
            Request[] requestsResult = new Request[rowsPerProc];
            for (int i = 0; i < rowsPerProc; i++) {
                int globalRow = (rank - 1) * rowsPerProc + i;
                if (globalRow < n1) {
                    requestsResult[i] = MPI.COMM_WORLD.Isend(localResult[i], 0, n3, MPI.INT, 0, 2);
                    System.out.println(String.format("Process <%d> sent local result for row %d to process <0>.",rank, globalRow));
                }
            }

            // Ожидание завершения отправки всех результатов
            Request.Waitall(requestsResult);
        }

        // Сбор результатов на процессе 0
        if (rank == 0) {
            Request[] requestsRecv = new Request[(size - 1) * rowsPerProc];
            int requestIndex = 0;
            for (int i = 1; i < size; i++) {
                for (int j = 0; j < rowsPerProc; j++) {
                    int row = (i - 1) * rowsPerProc + j;
                    if (row < n1) {
                        requestsRecv[requestIndex++] = MPI.COMM_WORLD.Irecv(resultMatrix[row], 0, n3, MPI.INT, i, 2);
                    }
                }
            }

            // Ожидание завершения всех операций приема результатов
            Request.Waitall(requestsRecv);
            System.out.println("Process <0> collected all results.");
        }

        MPI.Finalize();
    }

    public void multiplyBroadcast(String[] args) {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        int bufferSize = 1024 * 1024; // Размер буфера (выбирается на основе задач)
        byte[] buffer = new byte[bufferSize];
        MPI.Buffer_attach(ByteBuffer.wrap(buffer));

        // Учитываем случай, когда n1 не делится на количество процессов
        int rowsPerProc = (n1 + (size - 2)) / (size - 1);

        if (rank == 0) {
            System.out.println("Process <0> Broadcasting matrices to workers.");

            // Отправка строк матрицы A процессам
            for (int i = 1; i < size; i++) {
                for (int j = 0; j < rowsPerProc; j++) {
                    int row = (i - 1) * rowsPerProc + j;
                    if (row < n1) { // Отправляем только существующие строки
                        MPI.COMM_WORLD.Bsend(matrixA[row], 0, n2, MPI.INT, i, 0);
                        System.out.println(String.format("Process <0> Sent row %d of matrix A to process <%d>", row, i));
                    }
                }
            }

            // Отправка всей матрицы B всем процессам
            for (int i = 1; i < size; i++) {
                for (int rowIdx = 0; rowIdx < n2; rowIdx++) {
                    MPI.COMM_WORLD.Bsend(matrixB[rowIdx], 0, n3, MPI.INT, i, 1);
                }
                System.out.println(String.format("Process <0> sent entire matrix B to process <%d>", i));
            }
        } else {
            // Получение строк матрицы A
            int[][] localRows = new int[rowsPerProc][n2];
            for (int j = 0; j < rowsPerProc; j++) {
                int globalRow = (rank - 1) * rowsPerProc + j;
                if (globalRow < n1) { // Получаем только существующие строки
                    MPI.COMM_WORLD.Recv(localRows[j], 0, n2, MPI.INT, 0, 0);
                    System.out.println(String.format("Process <%d> received row %d of matrix A.", rank, globalRow));
                }
            }

            // Получение всей матрицы B
            int[][] localB = new int[n2][n3];
            for (int i = 0; i < n2; i++) {
                MPI.COMM_WORLD.Recv(localB[i], 0, n3, MPI.INT, 0, 1);
            }
            System.out.println(String.format("Process <%d> received entire matrix B.", rank));

            // Умножение строк матрицы A на столбцы матрицы B
            int[][] localResult = new int[rowsPerProc][n3];
            for (int i = 0; i < rowsPerProc; i++) {
                int globalRow = (rank - 1) * rowsPerProc + i;
                if (globalRow < n1) { // Умножаем только существующие строки
                    for (int j = 0; j < n3; j++) {
                        localResult[i][j] = 0;
                        for (int k = 0; k < n2; k++) {
                            localResult[i][j] += localRows[i][k] * localB[k][j];
                        }
                    }
                }
            }
            System.out.println(String.format("Process <%d> computed local result.", rank));

            // Отправка локальных результатов обратно процессу 0
            for (int i = 0; i < rowsPerProc; i++) {
                int globalRow = (rank - 1) * rowsPerProc + i;
                if (globalRow < n1) {
                    MPI.COMM_WORLD.Bsend(localResult[i], 0, n3, MPI.INT, 0, 2);
                    System.out.println(String.format("Process <%d> sent local result for row %d to process <0>.",rank, globalRow));
                }
            }
        }

        // Сбор результатов на процессе 0
        if (rank == 0) {
            for (int i = 1; i < size; i++) {
                for (int j = 0; j < rowsPerProc; j++) {
                    int row = (i - 1) * rowsPerProc + j;
                    if (row < n1) { // Собираем только существующие строки
                        MPI.COMM_WORLD.Recv(resultMatrix[row], 0, n3, MPI.INT, i, 2);
                        System.out.println(String.format("Process <0> Received result for row %d from process <%d> .", row, i));
                    }
                }
            }
        }

        MPI.Buffer_detach();
        MPI.Finalize();
    }

    private static void fillMatrixByRandom(int[][] matrix) {
        var rand = new Random();
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = rand.nextInt(100); // случайные числа от 0 до 99
            }
        }
    }
}