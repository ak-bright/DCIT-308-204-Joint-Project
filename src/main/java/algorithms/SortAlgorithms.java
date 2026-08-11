package algorithms;

import java.util.Comparator;

/**
 * Sorting algorithms implemented from scratch: selection sort, insertion sort,
 * merge sort, and quicksort. All are generic over a {@link Comparator} and sort
 * the array <b>in place</b> (merge sort uses an auxiliary buffer internally but
 * writes the result back into the input array).
 *
 * <p><b>Where used in the system:</b> ordering service requests for display,
 * sorting locations by name before a binary-search lookup, and preparing edge
 * lists. The four algorithms are also the subject of the performance study
 * (O(n²) vs O(n log n)) and the correctness section (insertion-sort invariant,
 * merge/quicksort induction).</p>
 */
public final class SortAlgorithms {
    private SortAlgorithms() {}

    /**
     * Selection sort: repeatedly select the minimum of the unsorted suffix and
     * swap it into place.
     * <p><b>Time:</b> O(n²) in all cases (the scan for the min is unavoidable).
     * <b>Space:</b> O(1). <b>Stable:</b> no.</p>
     * <p><b>Invariant:</b> after iteration i, a[0..i] holds the i+1 smallest
     * elements in sorted order.</p>
     */
    public static <T> void selectionSort(T[] a, Comparator<? super T> cmp) {
        for (int i = 0; i < a.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++)
                if (cmp.compare(a[j], a[min]) < 0) min = j; // track smallest so far
            swap(a, i, min);
        }
    }

    /**
     * Insertion sort: grow a sorted prefix by inserting each next element into
     * its correct position within it.
     * <p><b>Time:</b> best O(n) (already sorted), average/worst O(n²).
     * <b>Space:</b> O(1). <b>Stable:</b> yes. Excellent on small or nearly-sorted
     * inputs, which is why quicksort below delegates to it under a cutoff.</p>
     * <p><b>Invariant:</b> at the start of iteration i, a[0..i-1] is a sorted
     * permutation of the original a[0..i-1].</p>
     */
    public static <T> void insertionSort(T[] a, Comparator<? super T> cmp) {
        for (int i = 1; i < a.length; i++) {
            T key = a[i];
            int j = i - 1;
            // shift everything greater than key one slot right, then drop key in
            while (j >= 0 && cmp.compare(a[j], key) > 0) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }

    /**
     * Merge sort: divide the array in half, sort each half, then merge.
     * <p><b>Time:</b> O(n log n) in all cases. <b>Space:</b> O(n) auxiliary.
     * <b>Stable:</b> yes.</p>
     * <p><b>Correctness (induction):</b> a 1-element range is trivially sorted;
     * assuming both halves are sorted, the linear merge produces a sorted whole
     * because it always emits the smaller of the two front elements.</p>
     */
    @SuppressWarnings("unchecked")
    public static <T> void mergeSort(T[] a, Comparator<? super T> cmp) {
        if (a.length < 2) return;
        T[] aux = (T[]) new Object[a.length]; // shared scratch buffer
        mergeSort(a, aux, 0, a.length - 1, cmp);
    }

    private static <T> void mergeSort(T[] a, T[] aux, int lo, int hi, Comparator<? super T> cmp) {
        if (lo >= hi) return;
        int mid = lo + (hi - lo) / 2;
        mergeSort(a, aux, lo, mid, cmp);       // sort left half
        mergeSort(a, aux, mid + 1, hi, cmp);   // sort right half
        merge(a, aux, lo, mid, hi, cmp);       // combine
    }

    private static <T> void merge(T[] a, T[] aux, int lo, int mid, int hi, Comparator<? super T> cmp) {
        for (int k = lo; k <= hi; k++) aux[k] = a[k]; // snapshot the range
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid)                               a[k] = aux[j++]; // left exhausted
            else if (j > hi)                           a[k] = aux[i++]; // right exhausted
            else if (cmp.compare(aux[j], aux[i]) < 0)  a[k] = aux[j++]; // right smaller
            else                                       a[k] = aux[i++]; // left <= right (stable)
        }
    }

    /**
     * Quicksort with median-of-three pivot selection and an insertion-sort
     * cutoff for small subarrays (both standard practical speed-ups).
     * <p><b>Time:</b> best/average O(n log n), worst O(n²) (rare with
     * median-of-three; the pathological case needs an adversarial input).
     * <b>Space:</b> O(log n) average recursion depth. <b>Stable:</b> no.</p>
     * <p><b>Correctness:</b> partition places the pivot at its final sorted index
     * with all smaller elements left and all larger right (the partition
     * invariant); recursively sorting the two sides then sorts the whole.</p>
     */
    public static <T> void quickSort(T[] a, Comparator<? super T> cmp) {
        quickSort(a, 0, a.length - 1, cmp);
    }

    private static final int CUTOFF = 10; // below this, insertion sort is faster

    private static <T> void quickSort(T[] a, int lo, int hi, Comparator<? super T> cmp) {
        while (lo < hi) {
            if (hi - lo + 1 <= CUTOFF) { insertionSortRange(a, lo, hi, cmp); return; }
            int p = partition(a, lo, hi, cmp);
            // Recurse into the smaller side, loop on the larger side: bounds the
            // recursion depth to O(log n) even on skewed partitions.
            if (p - lo < hi - p) { quickSort(a, lo, p - 1, cmp); lo = p + 1; }
            else                 { quickSort(a, p + 1, hi, cmp); hi = p - 1; }
        }
    }

    private static <T> int partition(T[] a, int lo, int hi, Comparator<? super T> cmp) {
        int mid = lo + (hi - lo) / 2;
        // median-of-three: order lo, mid, hi so the median becomes the pivot
        if (cmp.compare(a[mid], a[lo]) < 0) swap(a, lo, mid);
        if (cmp.compare(a[hi], a[lo]) < 0)  swap(a, lo, hi);
        if (cmp.compare(a[hi], a[mid]) < 0) swap(a, mid, hi);
        swap(a, mid, hi - 1);              // stash pivot at hi-1
        T pivot = a[hi - 1];
        int i = lo, j = hi - 1;
        while (true) {
            while (cmp.compare(a[++i], pivot) < 0) { /* find left >= pivot */ }
            while (cmp.compare(pivot, a[--j]) < 0) { /* find right <= pivot */ }
            if (i >= j) break;
            swap(a, i, j);
        }
        swap(a, i, hi - 1);               // restore pivot to its final place
        return i;
    }

    private static <T> void insertionSortRange(T[] a, int lo, int hi, Comparator<? super T> cmp) {
        for (int i = lo + 1; i <= hi; i++) {
            T key = a[i];
            int j = i - 1;
            while (j >= lo && cmp.compare(a[j], key) > 0) { a[j + 1] = a[j]; j--; }
            a[j + 1] = key;
        }
    }

    private static <T> void swap(T[] a, int i, int j) { T t = a[i]; a[i] = a[j]; a[j] = t; }

    /** Utility used by tests: is the array sorted ascending by the comparator? */
    public static <T> boolean isSorted(T[] a, Comparator<? super T> cmp) {
        for (int i = 1; i < a.length; i++) if (cmp.compare(a[i - 1], a[i]) > 0) return false;
        return true;
    }
}
