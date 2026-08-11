package datastructures;

import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * Binary heap / priority queue implemented from scratch as an array-embedded
 * complete binary tree (parent at {@code i}, children at {@code 2i+1} / {@code 2i+2}).
 *
 * <p><b>Required operations:</b> insert (sift-up), extractTop (sift-down),
 * peek, and bottom-up heapify to build a heap from an existing array in O(n).</p>
 *
 * <p>The heap is ordered by a supplied {@link Comparator}. Passing a natural
 * comparator makes it a <b>min-heap</b> (extractTop == extractMin); passing a
 * reversed comparator makes it a <b>max-heap</b> (extractTop == extractMax) —
 * satisfying the "extractMin/Max" requirement with one implementation.</p>
 *
 * <p><b>Where it is used in the system:</b></p>
 * <ul>
 *   <li>As the <b>service-request dispatcher</b>: a min-heap keyed on
 *       {@code urgency} always yields the most urgent pending request next.</li>
 *   <li>Inside <b>Dijkstra</b> and <b>Prim</b> as the priority queue of tentative
 *       distances / edge weights.</li>
 * </ul>
 *
 * @param <T> element type
 */
public class BinaryHeap<T> {
    private Object[] heap;
    private int size;
    private final Comparator<? super T> cmp;

    /** Empty heap ordered by {@code comparator} (min-heap if natural order). */
    public BinaryHeap(Comparator<? super T> comparator) {
        this.cmp = comparator;
        this.heap = new Object[16];
        this.size = 0;
    }

    /**
     * Build a heap directly from an array of items in O(n) using bottom-up
     * heapify (Floyd's method): every internal node is sifted down, starting
     * from the last parent. This is asymptotically faster than n inserts.
     */
    public BinaryHeap(T[] items, Comparator<? super T> comparator) {
        this.cmp = comparator;
        this.heap = new Object[Math.max(16, items.length)];
        this.size = items.length;
        for (int i = 0; i < items.length; i++) heap[i] = items[i];
        // Start at the last non-leaf node and sift each one down.
        for (int i = (size / 2) - 1; i >= 0; i--) siftDown(i);
    }

    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }

    /** Insert a new element and restore the heap property by sifting up. O(log n). */
    public void insert(T value) {
        if (size == heap.length) grow();
        heap[size] = value;
        siftUp(size);
        size++;
    }

    /** Return (without removing) the top element (min or max per comparator). O(1). */
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) throw new NoSuchElementException("heap is empty");
        return (T) heap[0];
    }

    /** Remove and return the top element, then re-heapify. O(log n). */
    @SuppressWarnings("unchecked")
    public T extractTop() {
        if (isEmpty()) throw new NoSuchElementException("heap is empty");
        T top = (T) heap[0];
        heap[0] = heap[--size];   // move last element to the root
        heap[size] = null;
        if (size > 0) siftDown(0); // and let it sink to its correct place
        return top;
    }

    // --- internal heap mechanics -------------------------------------------

    /** Move the element at {@code i} up while it violates order with its parent. */
    @SuppressWarnings("unchecked")
    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (cmp.compare((T) heap[i], (T) heap[parent]) < 0) {
                swap(i, parent);
                i = parent;
            } else break;
        }
    }

    /** Move the element at {@code i} down while a child comes before it. */
    @SuppressWarnings("unchecked")
    private void siftDown(int i) {
        while (true) {
            int left = 2 * i + 1, right = 2 * i + 2, best = i;
            if (left  < size && cmp.compare((T) heap[left],  (T) heap[best]) < 0) best = left;
            if (right < size && cmp.compare((T) heap[right], (T) heap[best]) < 0) best = right;
            if (best == i) break;      // heap property restored
            swap(i, best);
            i = best;
        }
    }

    private void swap(int a, int b) { Object t = heap[a]; heap[a] = heap[b]; heap[b] = t; }

    private void grow() {
        Object[] bigger = new Object[heap.length * 2];
        for (int i = 0; i < size; i++) bigger[i] = heap[i];
        heap = bigger;
    }
}
