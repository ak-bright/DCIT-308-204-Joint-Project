package datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Dynamic array (resizable array list) implemented from scratch.
 *
 * <p><b>Required operations:</b> insert (append and at-index), get, set, remove,
 * and automatic resize (grow ×2 when full, shrink ×2 when a quarter full).</p>
 *
 * <p><b>Where it is used in the system:</b> this is the workhorse backing store
 * for almost everything — the in-memory tables of {@code Location},
 * {@code Route}, {@code ServiceRequest} and {@code Resource} objects held by the
 * repository, the adjacency lists inside {@link Graph}, and the result lists
 * returned by the search/sort algorithms. We use a plain Java array as the
 * backing block (allowed by the brief) and manage growth ourselves so we never
 * lean on {@code java.util.ArrayList} for the assessed logic.</p>
 *
 * @param <T> element type
 */
public class DynamicArray<T> implements Iterable<T> {
    private Object[] data;   // backing block; capacity == data.length
    private int size;        // number of live elements (size <= capacity)

    /** Create an empty array with a small default capacity. */
    public DynamicArray() { this(8); }

    public DynamicArray(int initialCapacity) {
        if (initialCapacity < 1) initialCapacity = 1;
        data = new Object[initialCapacity];
        size = 0;
    }

    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity()    { return data.length; }

    /** Append to the end. Amortised O(1). */
    public void add(T value) {
        if (size == data.length) resize(data.length * 2); // grow when full
        data[size++] = value;
    }

    /** Insert at an index, shifting the tail right by one. O(n). */
    public void insert(int index, T value) {
        checkIndexForAdd(index);
        if (size == data.length) resize(data.length * 2);
        // shift everything from index..size-1 one slot to the right
        for (int i = size; i > index; i--) data[i] = data[i - 1];
        data[index] = value;
        size++;
    }

    /** Random access read. O(1). */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        checkIndex(index);
        return (T) data[index];
    }

    /** Overwrite the value at an index, returning the old one. O(1). */
    @SuppressWarnings("unchecked")
    public T set(int index, T value) {
        checkIndex(index);
        T old = (T) data[index];
        data[index] = value;
        return old;
    }

    /** Remove by index, shifting the tail left. O(n). Shrinks if sparse. */
    @SuppressWarnings("unchecked")
    public T remove(int index) {
        checkIndex(index);
        T removed = (T) data[index];
        for (int i = index; i < size - 1; i++) data[i] = data[i + 1];
        data[--size] = null; // avoid loitering reference
        // shrink to save memory once the array is only a quarter full
        if (size > 0 && size == data.length / 4) resize(data.length / 2);
        return removed;
    }

    /** Remove the first element equal to {@code value}. Returns true if removed. */
    public boolean removeValue(T value) {
        int idx = indexOf(value);
        if (idx < 0) return false;
        remove(idx);
        return true;
    }

    /** Linear scan for the first index of {@code value}, or -1. O(n). */
    public int indexOf(T value) {
        for (int i = 0; i < size; i++) {
            if (value == null ? data[i] == null : value.equals(data[i])) return i;
        }
        return -1;
    }

    public boolean contains(T value) { return indexOf(value) >= 0; }

    public void clear() {
        for (int i = 0; i < size; i++) data[i] = null;
        size = 0;
    }

    /** Reallocate the backing block to {@code newCapacity} and copy elements. */
    private void resize(int newCapacity) {
        if (newCapacity < 1) newCapacity = 1;
        Object[] bigger = new Object[newCapacity];
        for (int i = 0; i < size; i++) bigger[i] = data[i];
        data = bigger;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("index " + index + " out of bounds for size " + size);
    }

    private void checkIndexForAdd(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("index " + index + " out of bounds for size " + size);
    }

    /** Simple forward iterator so the structure works in for-each loops. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int cursor = 0;
            @Override public boolean hasNext() { return cursor < size; }
            @SuppressWarnings("unchecked")
            @Override public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                return (T) data[cursor++];
            }
        };
    }
}
