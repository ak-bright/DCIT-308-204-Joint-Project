package algorithms;

import java.util.Comparator;

/**
 * Searching algorithms implemented from scratch: linear search and binary
 * search, each generic over a {@link Comparator}.
 *
 * <p><b>Where used in the system:</b> the UI's "look up a location/resource by
 * id or name" option and the sort-then-search demonstrations. Binary search also
 * underpins the correctness discussion in the report (loop invariant on the
 * search interval).</p>
 */
public final class SearchAlgorithms {
    private SearchAlgorithms() {}

    /**
     * Linear (sequential) search: scan left to right for the first element equal
     * to {@code target}. Returns its index or -1.
     *
     * <p><b>Time:</b> best O(1) (match at front), average/worst O(n).
     * <b>Space:</b> O(1).</p>
     * <p><b>Correctness:</b> the loop maintains the invariant "target is not in
     * a[0..i-1]"; when it finds a match it returns, and if it exits the loop the
     * invariant with i = n proves target is absent — so -1 is correct.</p>
     */
    public static <T> int linearSearch(T[] a, T target, Comparator<? super T> cmp) {
        for (int i = 0; i < a.length; i++) {
            if (cmp.compare(a[i], target) == 0) return i; // first match wins
        }
        return -1;
    }

    /**
     * Binary search over an array that MUST already be sorted ascending by the
     * same comparator. Repeatedly halves a candidate interval [lo, hi].
     *
     * <p><b>Precondition:</b> {@code a} is sorted; on unsorted input the result
     * is undefined (the report and tests treat "binary search on unsorted input"
     * as the invalid case).</p>
     * <p><b>Time:</b> best O(1), average/worst O(log n). <b>Space:</b> O(1).</p>
     * <p><b>Correctness (loop invariant):</b> at the top of every iteration, if
     * target is in the array then its index lies within [lo, hi]. Each step
     * discards a half that cannot contain target, preserving the invariant and
     * strictly shrinking the interval, so the loop terminates with either a hit
     * or lo &gt; hi (target absent).</p>
     */
    public static <T> int binarySearch(T[] a, T target, Comparator<? super T> cmp) {
        int lo = 0, hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;   // avoids (lo+hi) integer overflow
            int c = cmp.compare(a[mid], target);
            if (c == 0)      return mid;    // hit
            else if (c < 0)  lo = mid + 1;  // target is in the right half
            else             hi = mid - 1;  // target is in the left half
        }
        return -1; // interval empty: target absent
    }
}
