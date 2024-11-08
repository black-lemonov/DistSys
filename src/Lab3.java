import java.util.Arrays;
import java.util.Random;

import mpi.MPI;

/** 4 потока 3-го уровня */
public class Lab3 {
    public static void main(String[] args) {
        MPI.Init(args);
        int sender = MPI.COMM_WORLD.Rank();
        if (sender == 0) {
            int[] arr = generateArray(10, 20, 100);
            System.out.println(Arrays.toString(arr));

            int pivot = new Random().nextInt(4);
            int[] less = lessArr(arr, pivot);
            MPI.COMM_WORLD.Send(less, 0, less.length, MPI.INT, 1, 0);
            int[] greater = greaterOrEqualArr(arr, pivot);
            MPI.COMM_WORLD.Send(greater, 0, greater.length, MPI.INT, 2, 0);

            MPI.COMM_WORLD.Recv(less, 0, less.length, MPI.INT, 1, 0);
            MPI.COMM_WORLD.Recv(greater, 0, greater.length, MPI.INT, 2, 0);

            int[] res = joinArrays(less, new int[]{arr[pivot]}, greater);
            System.out.println(Arrays.toString(res));
            
        } else if (sender == 1 || sender == 2) {
            var st = MPI.COMM_WORLD.Probe(0, 0);
            int size = st.Get_count(MPI.INT);
            int[] arr = new int[size];
            MPI.COMM_WORLD.Recv(arr, 0, arr.length, MPI.INT, 0, 0);

            int recv1 = sender == 1 ? 3 : 5;
            int recv2 = sender == 1 ? 4 : 6;

            int pivot = new Random().nextInt(arr.length);
            
            int[] less = lessArr(arr, pivot);
            MPI.COMM_WORLD.Send(less, 0, less.length, MPI.INT, recv1, 0);
            
            int[] greater = greaterOrEqualArr(arr, pivot);
            MPI.COMM_WORLD.Send(greater, 0, greater.length, MPI.INT, recv2, 0);
            
            MPI.COMM_WORLD.Recv(less, 0, less.length, MPI.INT, recv1, 0);
            MPI.COMM_WORLD.Recv(greater, 0, greater.length, MPI.INT, recv2, 0);

            int[] res = joinArrays(less, new int[]{arr[pivot]}, greater);

            MPI.COMM_WORLD.Send(res, 0, res.length, MPI.INT, 0, 0);

        } else if (sender > 2 && sender < 7) {

            int from = sender == 3 || sender == 4 ? 1 : 2;

            var st = MPI.COMM_WORLD.Probe(from, 0);
            int size = st.Get_count(MPI.INT);
            int[] arr = new int[size];

            MPI.COMM_WORLD.Recv(arr, 0, size, MPI.INT, from, 0);

            Arrays.sort(arr);

            MPI.COMM_WORLD.Send(arr, 0, size, MPI.INT, from, 0);
        } 
        MPI.Finalize();
    }

    private static int[] generateArray(int size, int lowBnd, int topBnd) {
        int[] arr = new int[size];
        for (int i = 0; i < size; ++i) {
            arr[i] = (new Random().nextInt(topBnd) + lowBnd) % topBnd;
        }
        return arr;
    }

    private static int[] lessArr(int[] src, int index) {
        int[] less = new int[countLess(src, index)];
        for (int i = 0, offset = 0; i < src.length; ++i) {
            if (i != index && src[i] < src[index]) {
                less[offset] = src[i];
                ++offset; 
            }
        }
        return less;
    } 

    private static int[] greaterOrEqualArr(int[] src, int index) {
        int[] greater = new int[countGreaterOrEgual(src, index)];
        for (int i = 0, offset = 0; i < src.length; ++i) {
            if (i != index && src[i] >= src[index]) {
                greater[offset] = src[i];
                ++offset; 
            }
        }
        return greater;
    }

    private static int countGreaterOrEgual(int[] src, int index) {
        int count = 0;
        for (int i = 0; i < src.length; ++i) {
            if (i != index && src[i] >= src[index]) {
                ++count;
            }
        }
        return count;
    }

    private static int countLess(int[] src, int index) {
        int count = 0;
        for (int i = 0; i < src.length; ++i) {
            if (i != index && src[i] < src[index]) {
                ++count;
            }
        }
        return count;
    }

    private static int[] joinArrays(int[] arr1, int[] arr2, int[] arr3) {
        int totalLength = arr1.length + arr2.length + arr3.length;
        int[] resArr = new int[totalLength]; 
        for (int i = 0; i < totalLength; ++i) {
            if (i < arr1.length) {
                resArr[i] = arr1[i];
            } else if (i < arr2.length + arr1.length) {
                resArr[i] = arr2[i - arr1.length];
            } else {
                resArr[i] = arr3[i - arr1.length - arr2.length];
            }
        }
        return resArr;
    }
}
