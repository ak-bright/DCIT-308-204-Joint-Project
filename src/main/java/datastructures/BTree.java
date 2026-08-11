package datastructures;

/**
 * B-tree implemented from scratch (CLRS-style, parameterised by minimum degree
 * {@code t}). Every node other than the root holds between {@code t-1} and
 * {@code 2t-1} keys and stays perfectly height-balanced, so search, and insert
 * are all O(t · logₜ n) — i.e. O(log n) with a small, cache-friendly fan-out.
 *
 * <p><b>Required operations:</b> search, and insert with proactive node
 * <b>splitting</b> (a full child is split before we descend into it, so the root
 * is the only node that ever grows the tree's height).</p>
 *
 * <p><b>Where it is used in the system:</b> B-trees are exactly how real
 * databases index rows on disk, so this structure is used as a disk-style
 * ordered index of records by id. It ties the project back to the SQLite layer:
 * the report explains that the same shape underlies the database's own indexes.</p>
 *
 * @param <K> comparable key type
 * @param <V> value type
 */
public class BTree<K extends Comparable<K>, V> {

    private final int t; // minimum degree (t >= 2)

    /** A B-tree node: parallel key/value arrays plus child pointers. */
    private final class Node {
        int n;                 // current number of keys
        boolean leaf;          // true if node has no children
        final Object[] keys;   // up to 2t-1 keys
        final Object[] vals;   // values paired with keys
        final Object[] child;  // up to 2t children (each a Node)

        Node(boolean leaf) {
            this.leaf = leaf;
            this.keys  = new Object[2 * t - 1];
            this.vals  = new Object[2 * t - 1];
            this.child = new Object[2 * t];
            this.n = 0;
        }
        @SuppressWarnings("unchecked") K key(int i)   { return (K) keys[i]; }
        @SuppressWarnings("unchecked") Node child(int i) { return (Node) child[i]; }
    }

    private Node root;
    private int size;

    /** @param minimumDegree the B-tree order parameter t (must be >= 2). */
    public BTree(int minimumDegree) {
        if (minimumDegree < 2) throw new IllegalArgumentException("minimum degree t must be >= 2");
        this.t = minimumDegree;
        this.root = new Node(true);
    }

    /** Convenience: a reasonable default degree. */
    public BTree() { this(3); }

    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }

    /** Search for a key across the balanced tree. O(logₜ n). */
    public V search(K key) { return search(root, key); }

    private V search(Node x, K key) {
        int i = 0;
        while (i < x.n && key.compareTo(x.key(i)) > 0) i++; // find first key >= target
        if (i < x.n && key.compareTo(x.key(i)) == 0) {
            @SuppressWarnings("unchecked") V v = (V) x.vals[i];
            return v; // found in this node
        }
        if (x.leaf) return null;      // nowhere left to look
        return search(x.child(i), key); // descend into the right child
    }

    public boolean contains(K key) { return search(key) != null; }

    /**
     * Insert or update. Uses the proactive-split strategy: if the root is full
     * we grow the tree upward first, then insert into a guaranteed non-full node.
     */
    public void insert(K key, V value) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        Node r = root;
        if (r.n == 2 * t - 1) {           // root full: split to grow height by 1
            Node s = new Node(false);
            s.child[0] = r;
            splitChild(s, 0);
            root = s;
            insertNonFull(s, key, value);
        } else {
            insertNonFull(r, key, value);
        }
    }

    /** Insert into a node known to have room, splitting full children en route. */
    private void insertNonFull(Node x, K key, V value) {
        int i = x.n - 1;
        if (x.leaf) {
            // If the key already exists here, update in place.
            int j = 0;
            while (j < x.n && key.compareTo(x.key(j)) > 0) j++;
            if (j < x.n && key.compareTo(x.key(j)) == 0) { x.vals[j] = value; return; }
            // Otherwise shift larger keys right and drop the new key in.
            while (i >= 0 && key.compareTo(x.key(i)) < 0) {
                x.keys[i + 1] = x.keys[i];
                x.vals[i + 1] = x.vals[i];
                i--;
            }
            x.keys[i + 1] = key;
            x.vals[i + 1] = value;
            x.n++;
            size++;
        } else {
            // Find the child that should receive the key.
            while (i >= 0 && key.compareTo(x.key(i)) < 0) i--;
            i++;
            // If it exists at this internal level, update and stop.
            if (i - 1 >= 0 && i - 1 < x.n && key.compareTo(x.key(i - 1)) == 0) { x.vals[i - 1] = value; return; }
            if (x.child(i).n == 2 * t - 1) {      // split the full child first
                splitChild(x, i);
                if (key.compareTo(x.key(i)) > 0) i++;      // decide which half to descend
                else if (key.compareTo(x.key(i)) == 0) { x.vals[i] = value; return; }
            }
            insertNonFull(x.child(i), key, value);
        }
    }

    /**
     * Split the full child {@code x.child[i]} (which has 2t-1 keys) about its
     * median: the median moves up into {@code x}, and the child is divided into
     * two nodes of t-1 keys each.
     */
    private void splitChild(Node x, int i) {
        Node y = x.child(i);            // the full child
        Node z = new Node(y.leaf);      // new node for y's upper half
        z.n = t - 1;
        // copy the top t-1 keys/values of y into z
        for (int j = 0; j < t - 1; j++) {
            z.keys[j] = y.keys[j + t];
            z.vals[j] = y.vals[j + t];
            y.keys[j + t] = null; y.vals[j + t] = null;
        }
        // copy the top t children of y into z (if internal)
        if (!y.leaf) {
            for (int j = 0; j < t; j++) { z.child[j] = y.child[j + t]; y.child[j + t] = null; }
        }
        y.n = t - 1;
        // make room in x for the new child pointer and median key
        for (int j = x.n; j >= i + 1; j--) x.child[j + 1] = x.child[j];
        x.child[i + 1] = z;
        for (int j = x.n - 1; j >= i; j--) { x.keys[j + 1] = x.keys[j]; x.vals[j + 1] = x.vals[j]; }
        x.keys[i] = y.keys[t - 1];      // median key rises into x
        x.vals[i] = y.vals[t - 1];
        y.keys[t - 1] = null; y.vals[t - 1] = null;
        x.n++;
    }

    /** Inorder key listing (ascending) into our own {@link DynamicArray}. */
    public DynamicArray<K> inorderKeys() {
        DynamicArray<K> out = new DynamicArray<>();
        inorder(root, out);
        return out;
    }

    private void inorder(Node x, DynamicArray<K> out) {
        if (x == null) return;
        for (int i = 0; i < x.n; i++) {
            if (!x.leaf) inorder(x.child(i), out);
            out.add(x.key(i));
        }
        if (!x.leaf) inorder(x.child(x.n), out); // rightmost child
    }
}
