package datastructures;

/**
 * Self-balancing AVL tree implemented from scratch (the brief allows red-black
 * OR AVL — AVL is chosen for its simpler, easier-to-explain balance rule).
 *
 * <p>An AVL tree keeps, at every node, the invariant that the heights of the two
 * child subtrees differ by at most 1 (the <b>balance factor</b> is in
 * {-1, 0, +1}). After each insert we walk back up the path, update heights, and
 * apply the four standard rotations (LL, RR, LR, RL) wherever the invariant is
 * broken. This guarantees height O(log n) and therefore O(log n) search.</p>
 *
 * <p><b>Required operations:</b> insert with rotation/rebalancing, plus search
 * and inorder traversal.</p>
 *
 * <p><b>Where it is used in the system:</b> it is the balanced counterpart to
 * {@link BinarySearchTree} in the performance study — inserting the same sorted
 * keys, the plain BST becomes a degenerate list (height ≈ n) while this AVL tree
 * stays at height ≈ log₂n, which the benchmark measures directly. It can back
 * the by-name location index when guaranteed fast lookup matters.</p>
 *
 * @param <K> comparable key type
 * @param <V> value type
 */
public class AVLTree<K extends Comparable<K>, V> {

    private static final class Node<K, V> {
        K key; V value;
        Node<K, V> left, right;
        int height; // height of this node (leaf = 0)
        Node(K key, V value) { this.key = key; this.value = value; this.height = 0; }
    }

    private Node<K, V> root;
    private int size;

    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }
    public int height()      { return height(root); }

    private int height(Node<K, V> n) { return n == null ? -1 : n.height; }

    /** Balance factor = height(left) - height(right). */
    private int balanceFactor(Node<K, V> n) { return n == null ? 0 : height(n.left) - height(n.right); }

    private void updateHeight(Node<K, V> n) {
        n.height = 1 + Math.max(height(n.left), height(n.right));
    }

    /** Insert or update; rebalances on the way back up. O(log n). */
    public void insert(K key, V value) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        root = insert(root, key, value);
    }

    private Node<K, V> insert(Node<K, V> node, K key, V value) {
        if (node == null) { size++; return new Node<>(key, value); }
        int c = key.compareTo(node.key);
        if (c < 0)      node.left  = insert(node.left, key, value);
        else if (c > 0) node.right = insert(node.right, key, value);
        else { node.value = value; return node; } // duplicate key: update, no rebalance
        updateHeight(node);
        return rebalance(node);
    }

    /** Restore the AVL invariant at {@code node} via the four rotation cases. */
    private Node<K, V> rebalance(Node<K, V> node) {
        int bf = balanceFactor(node);
        // Left-heavy
        if (bf > 1) {
            if (balanceFactor(node.left) < 0) node.left = rotateLeft(node.left); // LR case
            return rotateRight(node);                                            // LL case
        }
        // Right-heavy
        if (bf < -1) {
            if (balanceFactor(node.right) > 0) node.right = rotateRight(node.right); // RL case
            return rotateLeft(node);                                                 // RR case
        }
        return node; // already balanced
    }

    /** Right rotation around {@code y} (fixes a left-left imbalance). */
    private Node<K, V> rotateRight(Node<K, V> y) {
        Node<K, V> x = y.left;
        Node<K, V> t2 = x.right;
        x.right = y;      // x becomes the new subtree root
        y.left  = t2;     // y adopts x's old right child
        updateHeight(y);
        updateHeight(x);
        return x;
    }

    /** Left rotation around {@code x} (fixes a right-right imbalance). */
    private Node<K, V> rotateLeft(Node<K, V> x) {
        Node<K, V> y = x.right;
        Node<K, V> t2 = y.left;
        y.left  = x;      // y becomes the new subtree root
        x.right = t2;     // x adopts y's old left child
        updateHeight(x);
        updateHeight(y);
        return y;
    }

    /** Search for a key; returns its value or {@code null}. O(log n). */
    public V search(K key) {
        Node<K, V> cur = root;
        while (cur != null) {
            int c = key.compareTo(cur.key);
            if (c < 0)      cur = cur.left;
            else if (c > 0) cur = cur.right;
            else            return cur.value;
        }
        return null;
    }

    public boolean contains(K key) { return search(key) != null; }

    /** Inorder traversal into our own {@link DynamicArray} (ascending keys). */
    public DynamicArray<K> inorderKeys() {
        DynamicArray<K> out = new DynamicArray<>();
        inorder(root, out);
        return out;
    }

    private void inorder(Node<K, V> n, DynamicArray<K> out) {
        if (n == null) return;
        inorder(n.left, out);
        out.add(n.key);
        inorder(n.right, out);
    }
}
