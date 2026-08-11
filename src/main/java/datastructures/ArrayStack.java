package datastructures;

import java.util.EmptyStackException;

/**
 * Stack (LIFO) implemented from scratch on top of our own {@link DynamicArray}.
 *
 * <p><b>Required operations:</b> push, pop, peek, isEmpty.</p>
 *
 * <p><b>Where it is used in the system:</b> the iterative Depth-First Search in
 * {@code algorithms.GraphAlgorithms} uses an explicit stack instead of
 * recursion, so very large or deep hospital graphs cannot blow the JVM call
 * stack. It is also handy for any "undo"-style traversal of visited nodes.</p>
 *
 * @param <T> element type
 */
public class ArrayStack<T> {
    private final DynamicArray<T> data = new DynamicArray<>();

    public boolean isEmpty() { return data.isEmpty(); }
    public int size()        { return data.size(); }

    /** Push onto the top. Amortised O(1). */
    public void push(T value) { data.add(value); }

    /** Remove and return the top. O(1). Throws if empty. */
    public T pop() {
        if (isEmpty()) throw new EmptyStackException();
        return data.remove(data.size() - 1); // last element is the top
    }

    /** Look at the top without removing it. O(1). Throws if empty. */
    public T peek() {
        if (isEmpty()) throw new EmptyStackException();
        return data.get(data.size() - 1);
    }
}
