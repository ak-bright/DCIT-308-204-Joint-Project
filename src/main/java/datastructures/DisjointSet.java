package datastructures;

/**
 * Disjoint-set / union-find implemented from scratch over integer elements
 * {@code 0 … n-1}, with both standard optimisations:
 * <ul>
 *   <li><b>Path compression</b> in {@code find}: every node visited on the way to
 *       the root is re-pointed straight at the root.</li>
 *   <li><b>Union by rank</b>: the shorter tree is hung under the taller one.</li>
 * </ul>
 * Together these give an almost-constant amortised cost of O(α(n)) per operation,
 * where α is the inverse-Ackermann function (≤ 4 for any realistic n).
 *
 * <p><b>Required operations:</b> makeSet, find (with path compression), union
 * (by rank).</p>
 *
 * <p><b>Where it is used in the system:</b> it powers <b>Kruskal's MST</b> in
 * {@code algorithms.GraphAlgorithms} — as each candidate corridor is considered,
 * union-find answers "are these two locations already connected?" in near O(1),
 * so we add an edge only when it joins two different components (no cycle).</p>
 */
public class DisjointSet {
    private final int[] parent; // parent[i] = i means i is a root
    private final int[] rank;   // upper bound on the height of the tree rooted at i
    private int components;     // number of disjoint sets currently

    /** Create n singleton sets: makeSet is applied to every element up front. */
    public DisjointSet(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) makeSet(i);
        components = n;
    }

    /** Initialise {@code x} as its own singleton set. */
    private void makeSet(int x) { parent[x] = x; rank[x] = 0; }

    public int count() { return components; } // how many separate components remain

    /** Find the representative of x's set, compressing the path as we go. */
    public int find(int x) {
        checkBounds(x);
        // Iterative two-pass compression: find root, then point everyone at it.
        int root = x;
        while (parent[root] != root) root = parent[root];
        while (parent[x] != root) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    /**
     * Merge the sets containing a and b. Returns false if they were already in
     * the same set (which, in Kruskal, means adding this edge would form a cycle).
     */
    public boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;        // already connected
        // attach the lower-rank root under the higher-rank one
        if (rank[ra] < rank[rb])      parent[ra] = rb;
        else if (rank[ra] > rank[rb]) parent[rb] = ra;
        else { parent[rb] = ra; rank[ra]++; } // equal ranks: pick one, bump its rank
        components--;
        return true;
    }

    /** True if a and b are in the same set. */
    public boolean connected(int a, int b) { return find(a) == find(b); }

    private void checkBounds(int x) {
        if (x < 0 || x >= parent.length)
            throw new IndexOutOfBoundsException("element " + x + " out of range 0.." + (parent.length - 1));
    }
}
