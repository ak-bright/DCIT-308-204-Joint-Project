package datastructures;

/**
 * Hash table implemented from scratch with <b>separate chaining</b> for
 * collision handling: the backing store is an array of buckets, and each bucket
 * is one of our own {@link DoublyLinkedList}s of {@code (key, value)} entries.
 *
 * <p><b>Required operations:</b> put, get, remove, plus automatic resize when
 * the load factor (entries ÷ buckets) exceeds a threshold, at which point every
 * entry is rehashed into a larger table.</p>
 *
 * <p><b>Where it is used in the system:</b> this is the primary <b>index</b> of
 * the whole application. The repository keeps hash tables from id → Location,
 * id → ServiceRequest, id → Resource, and (locationId → vertex index) inside the
 * graph, giving average O(1) "look up by ID" for the UI. The performance study
 * measures how put/get behave as the load factor rises.</p>
 *
 * @param <K> key type (uses {@code hashCode}/{@code equals})
 * @param <V> value type
 */
public class HashTable<K, V> {

    /** One key/value pair stored inside a bucket list. */
    public static final class Entry<K, V> {
        final K key;
        V value;
        Entry(K key, V value) { this.key = key; this.value = value; }
        public K getKey()   { return key; }
        public V getValue() { return value; }
    }

    private DoublyLinkedList<Entry<K, V>>[] buckets;
    private int size;
    private final double maxLoadFactor;

    public HashTable() { this(16, 0.75); }

    @SuppressWarnings("unchecked")
    public HashTable(int initialCapacity, double maxLoadFactor) {
        if (initialCapacity < 1) initialCapacity = 1;
        this.maxLoadFactor = maxLoadFactor;
        this.buckets = (DoublyLinkedList<Entry<K, V>>[]) new DoublyLinkedList[initialCapacity];
        this.size = 0;
    }

    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }
    public int bucketCount() { return buckets.length; }
    public double loadFactor(){ return (double) size / buckets.length; }

    /**
     * Spread the raw hashCode so that poor hashes (e.g. small sequential ints)
     * don't all land in the low buckets. Same idea as java.util.HashMap's spread.
     */
    private int indexFor(K key, int cap) {
        int h = (key == null) ? 0 : key.hashCode();
        h ^= (h >>> 16);
        return (h & 0x7fffffff) % cap; // mask sign bit, then fold into range
    }

    /** Insert or update. Average O(1); triggers a resize past the load factor. */
    public void put(K key, V value) {
        int i = indexFor(key, buckets.length);
        if (buckets[i] == null) buckets[i] = new DoublyLinkedList<>();
        for (Entry<K, V> e : buckets[i]) {
            if (eq(e.key, key)) { e.value = value; return; } // key exists: overwrite
        }
        buckets[i].addLast(new Entry<>(key, value));
        size++;
        if (loadFactor() > maxLoadFactor) resize(buckets.length * 2);
    }

    /** Look up a value by key. Average O(1). Returns null if absent. */
    public V get(K key) {
        int i = indexFor(key, buckets.length);
        if (buckets[i] == null) return null;
        for (Entry<K, V> e : buckets[i]) if (eq(e.key, key)) return e.value;
        return null;
    }

    public boolean containsKey(K key) {
        int i = indexFor(key, buckets.length);
        if (buckets[i] == null) return false;
        for (Entry<K, V> e : buckets[i]) if (eq(e.key, key)) return true;
        return false;
    }

    /** Remove a key. Returns the old value or null. Average O(1). */
    public V remove(K key) {
        int i = indexFor(key, buckets.length);
        if (buckets[i] == null) return null;
        for (Entry<K, V> e : buckets[i]) {
            if (eq(e.key, key)) {
                buckets[i].remove(e);
                size--;
                return e.value;
            }
        }
        return null;
    }

    /** Grow the table and rehash every entry into fresh buckets. O(n). */
    @SuppressWarnings("unchecked")
    private void resize(int newCap) {
        DoublyLinkedList<Entry<K, V>>[] old = buckets;
        buckets = (DoublyLinkedList<Entry<K, V>>[]) new DoublyLinkedList[newCap];
        for (DoublyLinkedList<Entry<K, V>> bucket : old) {
            if (bucket == null) continue;
            for (Entry<K, V> e : bucket) {
                int i = indexFor(e.key, newCap);
                if (buckets[i] == null) buckets[i] = new DoublyLinkedList<>();
                buckets[i].addLast(e); // reuse the same Entry object
            }
        }
    }

    /** Collect all keys into our own {@link DynamicArray} (order unspecified). */
    public DynamicArray<K> keys() {
        DynamicArray<K> out = new DynamicArray<>(Math.max(8, size));
        for (DoublyLinkedList<Entry<K, V>> bucket : buckets) {
            if (bucket == null) continue;
            for (Entry<K, V> e : bucket) out.add(e.key);
        }
        return out;
    }

    /** Collect all values into our own {@link DynamicArray}. */
    public DynamicArray<V> values() {
        DynamicArray<V> out = new DynamicArray<>(Math.max(8, size));
        for (DoublyLinkedList<Entry<K, V>> bucket : buckets) {
            if (bucket == null) continue;
            for (Entry<K, V> e : bucket) out.add(e.value);
        }
        return out;
    }

    private boolean eq(K a, K b) { return a == null ? b == null : a.equals(b); }
}
