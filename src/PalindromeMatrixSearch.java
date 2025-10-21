import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class PalindromeMatrixSearch {

    static final int ROWS = 1000;
    static final int COLS = 1000;
    static final int MIN_LEN = 3;
    static final int MAX_LEN = 6;
    static final int MAX_THREADS = 8;

    private final int rows;
    private final int cols;
    private final char[][] matrix;
    private final Random rand;

    public PalindromeMatrixSearch(int rows, int cols, long seed) {
        this.rows = rows;
        this.cols = cols;
        this.matrix = new char[rows][cols];
        this.rand = (seed == Long.MIN_VALUE) ? new Random() : new Random(seed);
        fillMatrix();
    }

    private void fillMatrix() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = (char) ('a' + rand.nextInt(26));
            }
        }
    }

    private static boolean isPalindrome(char[] buf, int len) {
        for (int i = 0, j = len - 1; i < j; i++, j--) {
            if (buf[i] != buf[j]) return false;
        }
        return true;
    }

    public long countPalindromes(int len, int numThreads) throws InterruptedException {
        AtomicLong total = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        int rowsPerThread = Math.max(1, rows / numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int t = 0; t < numThreads; t++) {
            final int startRow = t * rowsPerThread;
            final int endRow = (t == numThreads - 1) ? rows : startRow + rowsPerThread;

            pool.submit(() -> {
                char[] buffer = new char[len];
                long localCount = 0;

                for (int r = startRow; r < endRow; r++) {
                    for (int c = 0; c < cols; c++) {

                        // Check horizontal (right to left)
                        if (c - len + 1 >= 0) {
                            for (int k = 0; k < len; k++) buffer[k] = matrix[r][c - k];
                            if (isPalindrome(buffer, len)) localCount++;
                        }

                        // Check vertical
                        if (r + len <= rows) {
                            for (int k = 0; k < len; k++) buffer[k] = matrix[r + k][c];
                            if (isPalindrome(buffer, len)) localCount++;
                        }

                        // Check diagonal
                        if (r + len <= rows && c + len <= cols) {
                            for (int k = 0; k < len; k++) buffer[k] = matrix[r + k][c + k];
                            if (isPalindrome(buffer, len)) localCount++;
                        }
                    }
                }

                total.addAndGet(localCount);
                latch.countDown();
            });
        }

        latch.await();
        pool.shutdown();
        return total.get();
    }

    public static void main(String[] args) throws Exception {
        long seed = Long.MIN_VALUE;

        System.out.println("Running a small 10x10 test");
        PalindromeMatrixSearch smallTest = new PalindromeMatrixSearch(10, 10, seed);
        for (int len = MIN_LEN; len <= MAX_LEN; len++) {
            long start = System.nanoTime();
            long count = smallTest.countPalindromes(len, 1);
            long end = System.nanoTime();
            System.out.printf("%d palindromes of length %d found in %.3f seconds\n",
                    count, len, (end - start) / 1_000_000_000.0);
        }

        System.out.println("\nRunning full 1000x1000 test");
        PalindromeMatrixSearch largeTest = new PalindromeMatrixSearch(ROWS, COLS, seed);
        for (int threads = 1; threads <= MAX_THREADS; threads++) {
            System.out.printf("\nUsing %d threads:\n", threads);
            for (int len = MIN_LEN; len <= MAX_LEN; len++) {
                long start = System.nanoTime();
                long count = largeTest.countPalindromes(len, threads);
                long end = System.nanoTime();
                System.out.printf("%d palindromes of length %d found in %.3f seconds\n",
                        count, len, (end - start) / 1_000_000_000.0);
            }
        }
    }
}
