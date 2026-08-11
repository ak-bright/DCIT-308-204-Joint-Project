package datastructures;

/**
 * Set abstraction built on our own {@link HashTable} — <b>not</b>
 * {@code java.util.HashSet}. Membership is stored as keys mapping to a constant
 * present-marker, so add/contains/remove are all average O(1).
 *
 * <p><b>Required operations:</b> add, contains, remove, size.</p>
 *
 * <p><b>Where it is used in the system:</b> the "visited" set inside BFS/DFS and
 * the "seen" set when deduplicating ids while loading data. Using our own set
 * keeps the assessed traversal logic free of {@code java.util} collections.</p>
 *
 * @param <T> element type
 */
public class HashSet<T> {
    private static final Object PRESENT = new Object(); // sentinel value
    private final HashTable<T, Object> table = new HashTable<>();

    /** Add an element; returns true if it was newly added. */
    public boolean add(T value) {
        if (table.containsKey(value)) return false;
        table.put(value, PRESENT);
        return true;
    }

    public boolean contains(T value) { return table.containsKey(value); }

    /** Remove an element; returns true if it was present. */
    public boolean remove(T value) {
        boolean had = table.containsKey(value);
        table.remove(value);
        return had;
    }

    public int size()             { return table.size(); }
    public boolean isEmpty()      { return table.isEmpty(); }
    public DynamicArray<T> toArray() { return table.keys(); }
}
