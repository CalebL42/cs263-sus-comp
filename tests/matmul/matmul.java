package tests.matmul;

public class matmul {

    // generate a square matrix of given size with values starting from 'start'
    public static int[][] generate(int size, int start) {
        int[][] matrix = new int[size][size];
        int counter = start;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = counter;
                counter++;
            }
        }
        return matrix;
    }

    // perform matrix multiplication
    public static int[][] matMul(int[][] m1, int[][] m2, int size) {
        int[][] prod = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    prod[i][j] += m1[i][k] * m2[k][j];
                }
            }
        }
        return prod;
    }

    // sum the full matrix
    public static long sumMatrix(int[][] matrix, int size) {
        long sum = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                sum += matrix[i][j];
            }
        }
        return sum;
    }

    public static void main(String[] args) {
        int n = (args.length > 0) ? Integer.parseInt(args[0]) : 10;

        int[][] m1 = generate(n, 0);
        int[][] m2 = generate(n, n);
        int[][] prod = matMul(m1, m2, n);

        long sum = sumMatrix(prod, n);
        System.out.println(sum);
    }
}
