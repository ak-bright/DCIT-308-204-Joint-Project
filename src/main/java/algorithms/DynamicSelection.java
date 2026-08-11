package algorithms;

import datastructures.DynamicArray;
import model.ServiceRequest;

/**
 * Dynamic-programming request selection: choose the subset of pending
 * {@link ServiceRequest}s that maximises total <b>value</b> (operational benefit)
 * without exceeding a fixed capacity budget of staff-minutes. This is the
 * <b>0/1 knapsack</b> problem — each request is taken whole or not at all.
 *
 * <p>We use bottom-up <b>tabulation</b>. Let {@code w_i} = request i's service
 * minutes and {@code v_i} = its value. Define {@code dp[i][c]} = the best total
 * value achievable using the first {@code i} requests within capacity {@code c}:
 * </p>
 * <pre>
 *   dp[0][c] = 0                                         (no requests -> value 0)
 *   dp[i][c] = dp[i-1][c]                                 if w_i &gt; c  (cannot fit)
 *            = max( dp[i-1][c],                           skip request i
 *                   v_i + dp[i-1][c - w_i] )              take request i
 * </pre>
 *
 * <p><b>Time:</b> O(n · C) where C is the capacity budget. <b>Space:</b> O(n · C)
 * for the table (kept in full so we can reconstruct which requests were chosen).</p>
 *
 * <p><b>Correctness (optimal substructure + no after-effect):</b> the best packing
 * of the first i items either uses item i or not; both sub-cases are themselves
 * optimal packings of the first i-1 items (over the appropriate residual
 * capacity), so taking the max is optimal. Unlike the greedy dispatcher, this
 * considers <em>combinations</em>, so it finds B+C over A in the counterexample.</p>
 *
 * <hr>
 * <p><b>WORKED TABULATION — one full example run.</b><br>
 * Capacity C = 10, three requests (same numbers as the greedy counterexample):</p>
 * <pre>
 *   i  Request  w_i  v_i
 *   1     A      10   10
 *   2     B       5    8
 *   3     C       5    8
 *
 *   dp[i][c], columns c = 0..10  (rows = after considering first i requests):
 *
 *          c: 0  1  2  3  4  5  6  7  8  9 10
 *   i=0 (—):  0  0  0  0  0  0  0  0  0  0  0
 *   i=1 (A):  0  0  0  0  0  0  0  0  0  0 10   <- A only helps once c reaches 10
 *   i=2 (B):  0  0  0  0  0  8  8  8  8  8 10   <- B (w5,v8) fills c>=5
 *   i=3 (C):  0  0  0  0  0  8  8  8  8  8 16   <- at c=10: max(10, 8+dp[2][5]=8) = 16
 *
 *   Answer dp[3][10] = 16, chosen by back-tracking the table = {B, C}.
 * </pre>
 * The DP beats the greedy dispatcher's value of 10 for the throughput/value
 * objective, at the cost of the O(n·C) table.</p>
 */
public final class DynamicSelection {
    private DynamicSelection() {}

    /** The chosen subset plus the optimal total value the DP achieved. */
    public static final class Selection {
        public final DynamicArray<ServiceRequest> chosen = new DynamicArray<>();
        public int totalValue = 0;
        public int totalWeight = 0;
    }

    /**
     * Solve 0/1 knapsack over {@code requests} with the given capacity budget
     * (in staff-minutes). Weight = {@link ServiceRequest#getServiceMinutes()},
     * value = {@link ServiceRequest#getValue()}.
     */
    public static Selection selectBest(DynamicArray<ServiceRequest> requests, int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("capacity must be >= 0");
        int n = requests.size();

        // dp[i][c] as described in the class Javadoc. (n+1) x (capacity+1).
        int[][] dp = new int[n + 1][capacity + 1];
        for (int i = 1; i <= n; i++) {
            int w = requests.get(i - 1).getServiceMinutes();
            int v = requests.get(i - 1).getValue();
            for (int c = 0; c <= capacity; c++) {
                dp[i][c] = dp[i - 1][c];                 // case: skip request i
                if (w <= c) {                            // case: take request i (if it fits)
                    int take = v + dp[i - 1][c - w];
                    if (take > dp[i][c]) dp[i][c] = take;
                }
            }
        }

        // Reconstruct the chosen set by walking the table back from dp[n][capacity].
        Selection sel = new Selection();
        sel.totalValue = dp[n][capacity];
        int c = capacity;
        for (int i = n; i >= 1; i--) {
            // If this row differs from the row above, request i was taken here.
            if (dp[i][c] != dp[i - 1][c]) {
                ServiceRequest r = requests.get(i - 1);
                sel.chosen.add(r);
                sel.totalWeight += r.getServiceMinutes();
                c -= r.getServiceMinutes();
            }
        }
        return sel;
    }
}
