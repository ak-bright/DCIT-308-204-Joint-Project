package datastructures;

/**
 * Plain FIFO queue built on our own {@link DoublyLinkedList}.
 *
 * <p><b>Required operations:</b> enqueue, dequeue, peek, isEmpty. This is the
 * "unbounded linked" companion to {@link CircularQueue}; the brief asks for a
 * queue and a circular queue, and this one shows the linked-list realisation
 * while {@link CircularQueue} shows the ring-buffer realisation.</p>
 *
 * <p><b>Where it is used in the system:</b> general FIFO buffering where a fixed
 * ring is not wanted — e.g. queuing service requests in arrival order before
 * they are prioritised into the heap.</p>
 *
 * @param <T> element type
 */
public class LinkedQueue<T> {
    private final DoublyLinkedList<T> list = new DoublyLinkedList<>();

    public boolean isEmpty() { return list.isEmpty(); }
    public int size()        { return list.size(); }

    /** Add to the back. O(1). */
    public void enqueue(T value) { list.addLast(value); }

    /** Remove and return the front. O(1). */
    public T dequeue() {
        T front = list.getFirst();   // throws NoSuchElementException if empty
        list.remove(front);
        return front;
    }

    /** Look at the front without removing it. O(1). */
    public T peek() { return list.getFirst(); }
}
