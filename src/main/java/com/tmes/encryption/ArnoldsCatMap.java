package com.tmes.encryption;

public class ArnoldsCatMap {
    private final int p;
    private final int q;
    private final int N; // Image dimension (Width/Height)

    public ArnoldsCatMap(int p, int q, int N) {
        this.p = p;
        this.q = q;
        this.N = N;
    }

    /**
     * Encryption: Forward transformation
     * x_new = (x + p*y) % N
     * y_new = (q*x + (pq+1)*y) % N
     */
    public int[] encryptPixel(int x, int y) {
        long xLong = x;
        long yLong = y;

        // Calculate using long to prevent overflow
        long xNewVal = (1 * xLong + (long)p * yLong);
        long yNewVal = ((long)q * xLong + ((long)p * q + 1) * yLong);

        // Apply Modulo N
        int xNew = (int) (xNewVal % N);
        int yNew = (int) (yNewVal % N);

        return new int[]{xNew, yNew};
    }

    /**
     * Decryption: Inverse transformation
     * x_old = ((pq+1)*x_new - p*y_new) % N
     * y_old = (-q*x_new + y_new) % N
     */
    // Helper to ensure positive result (Java's % operator leaks negatives)
    private int floorMod(long a, int n) {
        return (int) ((a % n + n) % n);
    }

    public int[] decryptPixel(int x, int y) {
        long xLong = x;
        long yLong = y;

        // Formula 1: ((pq+1)*x_new - p*y_new)
        long term1 = ((long)p * q + 1) * xLong - ((long)p * yLong);

        // Formula 2: (-q*x_new + y_new)
        long term2 = -((long)q * xLong) + yLong;

        // Use the safe modulo
        int xOld = floorMod(term1, N);
        int yOld = floorMod(term2, N);

        return new int[]{xOld, yOld};
    }
    public int getP() {
        return p;
    }

    public int getQ() {
        return q;
    }
}