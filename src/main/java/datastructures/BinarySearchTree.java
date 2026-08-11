package datastructures;

/**
 * Unbalanced binary search tree (BST) implemented from scratch, mapping
 * {@code Comparable} keys to values.
 *
 * <p><b>Required operations:</b> insert, search, and inorder traversal (which
 * visits keys in ascending order).</p>
 *
 * <p><b>Where it is used in the system:</b> it provides ordered lookup of
 * {@code Location}s by name, so the UI can list locations alphabetically and do
 * name searches. It is deliberately the <em>unbalanced</em> tree so the
 * performance study can contrast it with the self-balancing {@link AVLTree}: on
 * already-sorted input this BST degrades to a linked list (height n), which the
 * benchmark makes visible.</p>
 *
 * @param <K> comparable key type
 * @param <V> value type
 */
public class BinarySearchTree<K extends Comparable<K>, V> {

    private static final class Node<K, V> {
        K key; V value;
        Node<K, V> left, right;
        Node(K key, V value) { this.key = key; this.value = value; }
    }

    private Node<K, V> root;
    private int size;

    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }

    /**
     * Insert or update. If the key already exists its value is overwritten.
     * Average O(log n); worst case O(n) on sorted input (degenerate tree).
     */
    public void insert(K key, V value) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        root = insert(root, key, value);
    }

    private Node<K, V> insert(Node<K, V> node, K key, V value) {
        if (node == null) { size++; return new Node<>(key, value); }
        int c = key.compareTo(node.key);
        if (c < 0)      node.left  = insert(node.left, key, value);
        else if (c > 0) node.right = insert(node.right, key, value);
        else            node.value = value; // key present: update in place
        return node;
    }

    /** Search for a key; returns its value or {@code null} if absent. O(h). */
    public V search(K key) {
        Node<K, V> cur = root;
        while (cur != null) {
            int c = key.compareTo(cur.key);
            if (c < 0)      cur = cur.left;
            else if (c > 0) cur = cur.right;
            else            return cur.value; // found
        }
        return null;
    }

    public boolean contains(K key) { return search(key) != null; }

    /** Height of the tree (empty tree = -1). Used by the benchmark vs. AVL. */
    public int height() { return height(root); }
    private int height(Node<K, V> n) {
        if (n == null) return -1;
        return 1 + Math.max(height(n.left), height(n.right));
    }

    /**
     * Inorder traversal: appends keys in ascending order into {@code out}.
     * We use our own {@link DynamicArray} for the result rather than java.util.
     */
    public DynamicArray<K> inorderKeys() {
        DynamicArray<K> out = new DynamicArray<>();
        inorder(root, out);
        return out;
    }

    private void inorder(Node<K, V> n, DynamicArray<K> out) {
        if (n == null) return;
        inorder(n.left, out);   // left subtree  (smaller keys)
        out.add(n.key);         // node itself
        inorder(n.right, out);  // right subtree (larger keys)
    }
}
