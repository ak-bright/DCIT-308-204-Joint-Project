package datastructures;

import java.util.NoSuchElementException;

/**
 * Double-ended queue (deque) implemented from scratch as a wrap-around array,
 * so both ends support O(1) insertion and removal.
 *
 * <p><b>Required operations:</b> addFront, addRear, removeFront, removeRear.</p>
 *
 * <p><b>Where it is used in the system:</b> it is available for algorithms that
 * need to push work back to the front as well as the back (for example a
 * "0-1 BFS" style relaxation, or peeking at both the newest and oldest pending
 * request). It also demonstrates that a single ring buffer generalises both a
 * stack and a queue.</p>
 *
 * @param <T> element type
 */
public class Deque<T> {
    private Object[] ring;
    private int head;  // index of the current front element
    private int tail;  // index one past the current rear element
    private int size;

    public Deque() { this(8); }

    public Deque(int capacity) {
        if (capacity < 1) capacity = 1;
        ring = new Object[capacity];
    }

    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }

    /** Insert at the front, moving head backwards (with wrap). Amortised O(1). */
    public void addFront(T value) {
        if (size == ring.length) grow();
        head = (head - 1 + ring.length) % ring.length; // step back, wrapping
        ring[head] = value;
        size++;
    }

    /** Insert at the rear, moving tail forwards (with wrap). Amortised O(1). */
    public void addRear(T value) {
        if (size == ring.length) grow();
        ring[tail] = value;
        tail = (tail + 1) % ring.length;               // step forward, wrapping
        size++;
    }

    /** Remove and return the front element. O(1). */
    @SuppressWarnings("unchecked")
    public T removeFront() {
        if (isEmpty()) throw new NoSuchElementException("deque is empty");
        T v = (T) ring[head];
        ring[head] = null;
        head = (head + 1) % ring.length;
        size--;
        return v;
    }

    /** Remove and return the rear element. O(1). */
    @SuppressWarnings("unchecked")
    public T removeRear() {
        if (isEmpty()) throw new NoSuchElementException("deque is empty");
        tail = (tail - 1 + ring.length) % ring.length;
        T v = (T) ring[tail];
        ring[tail] = null;
        size--;
        return v;
    }

    @SuppressWarnings("unchecked")
    public T peekFront() {
        if (isEmpty()) throw new NoSuchElementException("deque is empty");
        return (T) ring[head];
    }

    @SuppressWarnings("unchecked")
    public T peekRear() {
        if (isEmpty()) throw new NoSuchElementException("deque is empty");
        return (T) ring[(tail - 1 + ring.length) % ring.length];
    }

    /** Double capacity and re-linearise from head. */
    private void grow() {
        Object[] bigger = new Object[ring.length * 2];
        for (int i = 0; i < size; i++) bigger[i] = ring[(head + i) % ring.length];
        ring = bigger;
        head = 0;
        tail = size;
    }
}
