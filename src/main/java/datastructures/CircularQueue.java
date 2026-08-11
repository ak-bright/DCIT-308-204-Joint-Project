package datastructures;

import java.util.NoSuchElementException;

/**
 * Circular queue (ring buffer) implemented from scratch on a fixed-style array
 * with head/tail indices that <b>wrap around</b> using modular arithmetic.
 *
 * <p><b>Required operations:</b> enqueue, dequeue, and wrap-around behaviour.
 * When the buffer fills we grow it (×2) and re-linearise the elements, so the
 * queue is effectively unbounded while still demonstrating the classic
 * wrap-around ring.</p>
 *
 * <p><b>Where it is used in the system:</b> Breadth-First Search
 * ({@code algorithms.GraphAlgorithms}) uses this as its frontier queue when
 * computing which locations are reachable from a dispatch point. The ring layout
 * means enqueue/dequeue are O(1) without shifting elements.</p>
 *
 * @param <T> element type
 */
public class CircularQueue<T> {
    private Object[] ring;
    private int head;  // index of the front element
    private int tail;  // index where the next element will be written
    private int size;  // number of live elements

    public CircularQueue() { this(8); }

    public CircularQueue(int capacity) {
        if (capacity < 1) capacity = 1;
        ring = new Object[capacity];
        head = tail = size = 0;
    }

    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }

    /** Add to the back, wrapping the tail index. Amortised O(1). */
    public void enqueue(T value) {
        if (size == ring.length) grow();       // full: expand before writing
        ring[tail] = value;
        tail = (tail + 1) % ring.length;       // wrap-around
        size++;
    }

    /** Remove and return the front, wrapping the head index. O(1). */
    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (isEmpty()) throw new NoSuchElementException("queue is empty");
        T v = (T) ring[head];
        ring[head] = null;                     // release reference
        head = (head + 1) % ring.length;       // wrap-around
        size--;
        return v;
    }

    /** Look at the front without removing it. O(1). */
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) throw new NoSuchElementException("queue is empty");
        return (T) ring[head];
    }

    /** Double the ring and copy elements back in logical (front-to-back) order. */
    private void grow() {
        Object[] bigger = new Object[ring.length * 2];
        for (int i = 0; i < size; i++) bigger[i] = ring[(head + i) % ring.length];
        ring = bigger;
        head = 0;
        tail = size; // after re-linearising, tail sits just past the last element
    }
}
