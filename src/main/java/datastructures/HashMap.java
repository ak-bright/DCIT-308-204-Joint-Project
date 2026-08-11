package datastructures;

/**
 * Map abstraction built on our own {@link HashTable} — <b>not</b>
 * {@code java.util.HashMap}. It is a thin, clearly-named wrapper so the rest of
 * the code can talk in "map" terms while all storage and collision handling
 * remain our hand-written table.
 *
 * <p><b>Required operations:</b> put, get, remove, containsKey, keys.</p>
 *
 * <p><b>Where it is used in the system:</b> anywhere a keyed association is
 * needed but the caller wants "map" semantics — e.g. mapping a locationId to its
 * integer vertex index, or an algorithm-run label to a timing.</p>
 *
 * @param <K> key type
 * @param <V> value type
 */
public class HashMap<K, V> {
    private final HashTable<K, V> table = new HashTable<>();

    public void put(K key, V value)   { table.put(key, value); }
    public V get(K key)               { return table.get(key); }
    public V remove(K key)            { return table.remove(key); }
    public boolean containsKey(K key) { return table.containsKey(key); }
    public int size()                 { return table.size(); }
    public boolean isEmpty()          { return table.isEmpty(); }
    public DynamicArray<K> keys()     { return table.keys(); }
    public DynamicArray<V> values()   { return table.values(); }
}
