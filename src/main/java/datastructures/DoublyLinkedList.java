package datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Doubly linked list implemented from scratch.
 *
 * <p><b>Required operations:</b> addFirst, addLast, insertAfter, remove, and an
 * iterator. Each node keeps {@code prev}/{@code next} pointers so we can splice
 * in O(1) once we hold a node.</p>
 *
 * <p><b>Where it is used in the system:</b> it backs the separate-chaining
 * buckets of {@link HashTable} (each bucket is a small list of colliding
 * entries), and it is offered as a general list where cheap front/back
 * insertion matters. Keeping it doubly linked makes {@code remove} of an
 * interior node O(1) given the node reference.</p>
 *
 * @param <T> element type
 */
public class DoublyLinkedList<T> implements Iterable<T> {

    /** Internal node. Kept package-private so {@link HashTable} can reuse it. */
    static final class Node<T> {
        T value;
        Node<T> prev;
        Node<T> next;
        Node(T value) { this.value = value; }
    }

    private Node<T> head; // first node, or null when empty
    private Node<T> tail; // last node,  or null when empty
    private int size;

    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }

    /** Insert at the front. O(1). */
    public void addFirst(T value) {
        Node<T> n = new Node<>(value);
        if (head == null) {          // empty list: n is both head and tail
            head = tail = n;
        } else {
            n.next = head;
            head.prev = n;
            head = n;
        }
        size++;
    }

    /** Insert at the back. O(1). */
    public void addLast(T value) {
        Node<T> n = new Node<>(value);
        if (tail == null) {
            head = tail = n;
        } else {
            n.prev = tail;
            tail.next = n;
            tail = n;
        }
        size++;
    }

    /**
     * Insert {@code value} immediately after the first node whose value equals
     * {@code afterValue}. Returns false if {@code afterValue} is not present.
     * O(n) to find, O(1) to splice.
     */
    public boolean insertAfter(T afterValue, T value) {
        Node<T> cur = head;
        while (cur != null && !equalsVal(cur.value, afterValue)) cur = cur.next;
        if (cur == null) return false;
        Node<T> n = new Node<>(value);
        n.prev = cur;
        n.next = cur.next;
        if (cur.next != null) cur.next.prev = n; else tail = n; // inserting after tail
        cur.next = n;
        size++;
        return true;
    }

    public T getFirst() {
        if (head == null) throw new NoSuchElementException("list is empty");
        return head.value;
    }

    public T getLast() {
        if (tail == null) throw new NoSuchElementException("list is empty");
        return tail.value;
    }

    /** Remove the first node equal to {@code value}. Returns true if removed. */
    public boolean remove(T value) {
        Node<T> cur = head;
        while (cur != null && !equalsVal(cur.value, value)) cur = cur.next;
        if (cur == null) return false;
        unlink(cur);
        return true;
    }

    /** Detach a node from the chain and fix its neighbours' pointers. O(1). */
    private void unlink(Node<T> n) {
        if (n.prev != null) n.prev.next = n.next; else head = n.next;
        if (n.next != null) n.next.prev = n.prev; else tail = n.prev;
        n.prev = n.next = null;
        size--;
    }

    public boolean contains(T value) {
        for (Node<T> c = head; c != null; c = c.next) if (equalsVal(c.value, value)) return true;
        return false;
    }

    public void clear() { head = tail = null; size = 0; }

    private boolean equalsVal(T a, T b) { return a == null ? b == null : a.equals(b); }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> cur = head;
            @Override public boolean hasNext() { return cur != null; }
            @Override public T next() {
                if (cur == null) throw new NoSuchElementException();
                T v = cur.value;
                cur = cur.next;
                return v;
            }
        };
    }
}
