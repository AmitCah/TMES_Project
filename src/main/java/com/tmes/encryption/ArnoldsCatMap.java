package com.tmes.encryption;

/**
 * Implements the 2D Generalized Arnold's Cat Map for chaotic image scrambling.
 * This mathematical model uses linear algebra (matrix multiplication) across a discrete
 * torus (modulo N) to scramble pixel coordinates based on the P and Q cryptographic keys.
 */
public class ArnoldsCatMap {
    private final int p;
    private final int q;
    private final int N; // The dimension of the square image (Width/Height)

    /**
     * Initializes the transformation matrix parameters for the chaotic map.
     *
     * @param p The Maximum Flow metric derived from the graph topology.
     * @param q The Min-Cut Hash metric derived from the graph topology.
     * @param N The dimension of the square image matrix (Width/Height).
     */
    public ArnoldsCatMap(int p, int q, int N) {
        this.p = p;
        this.q = q;
        this.N = N;
    }

    /**
     * Encryption: Forward transformation.
     * Maps a pixel's current coordinates to a new, pseudo-random location using linear algebra
     * across a discrete torus (modulo N).
     * The matrix multiplication is:
     * [1, p]   * [x]
     * [q, pq+1]  [y]
     *
     * @param x The current X coordinate of the pixel.
     * @param y The current Y coordinate of the pixel.
     * @return An integer array of size 2 containing the new [x, y] coordinates.
     */
    public int[] encryptPixel(int x, int y) {
        // Cast coordinates to long to prevent arithmetic overflow when multiplying by large P/Q values.
        long xLong = x;
        long yLong = y;

        // Calculate the raw transformed coordinates using the generalized Arnold matrix.
        long xNewVal = (1 * xLong + (long)p * yLong);
        long yNewVal = ((long)q * xLong + ((long)p * q + 1) * yLong);

        // Apply Modulo N to wrap the coordinates back onto the valid image plane.
        int xNew = (int) (xNewVal % N);
        int yNew = (int) (yNewVal % N);

        return new int[]{xNew, yNew};
    }

    /**
     * A mathematical floor modulo function.
     * Required because Java's native '%' operator calculates remainder, not a true mathematical modulo.
     * This prevents negative coordinate values from causing IndexOutOfBoundsExceptions during decryption.
     *
     * @param a The dividend.
     * @param n The divisor (image dimension N).
     * @return The true positive mathematical modulo.
     */
    private int floorMod(long a, int n) {
        return (int) ((a % n + n) % n);
    }

    /**
     * Decryption: Inverse transformation.
     * Restores a pixel's scrambled coordinates to their original location by multiplying
     * with the inverse of the Arnold matrix:
     * [ pq+1,  -p] * [x_new]
     * [ -q,     1]   [y_new]
     *
     * @param x The encrypted X coordinate.
     * @param y The encrypted Y coordinate.
     * @return An integer array of size 2 containing the original [x, y] coordinates.
     */
    public int[] decryptPixel(int x, int y) {
        long xLong = x;
        long yLong = y;

        // Apply the inverse matrix formulas. The determinant of the original matrix is 1,
        // which guarantees that this inverse exists and uses integer coefficients.
        long term1 = ((long)p * q + 1) * xLong - ((long)p * yLong);
        long term2 = -((long)q * xLong) + yLong;

        // Use the safe floor modulo to handle negative intermediate values from the inverse matrix.
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