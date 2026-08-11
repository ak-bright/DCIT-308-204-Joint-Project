package algorithms;

import datastructures.ArrayStack;
import datastructures.BinaryHeap;
import datastructures.CircularQueue;
import datastructures.DisjointSet;
import datastructures.DynamicArray;
import datastructures.Graph;

import java.util.Comparator;

/**
 * Graph algorithms implemented from scratch on our own {@link Graph}, using our
 * own {@link CircularQueue} (BFS), {@link ArrayStack} (DFS),
 * {@link BinaryHeap} (Dijkstra, Prim) and {@link DisjointSet} (Kruskal).
 *
 * <p><b>Where used in the system:</b> the routing and network-planning menu
 * options — "reachable from a dispatch point" (BFS/DFS), "fastest route between
 * two locations" (Dijkstra on travel time), and "cheapest network connecting all
 * locations" (Prim/Kruskal on distance).</p>
 */
public final class GraphAlgorithms {
    private GraphAlgorithms() {}

    // ---------------------------------------------------------------------
    // Breadth-First Search
    // ---------------------------------------------------------------------
    /**
     * BFS from a source vertex, returning vertices in the order first visited.
     * <p><b>Time:</b> O(V + E). <b>Space:</b> O(V).</p>
     * <p><b>Correctness:</b> a FIFO queue visits vertices in non-decreasing order
     * of hop-distance from the source, so every reachable vertex is enqueued
     * exactly once — the returned list is precisely the reachable set.</p>
     */
    public static DynamicArray<Integer> bfs(Graph g, int source) {
        boolean[] visited = new boolean[g.vertexCount()];
        DynamicArray<Integer> order = new DynamicArray<>();
        CircularQueue<Integer> queue = new CircularQueue<>();
        visited[source] = true;
        queue.enqueue(source);
        while (!queue.isEmpty()) {
            int u = queue.dequeue();
            order.add(u);
            for (Graph.Edge e : g.neighbors(u)) {
                if (!visited[e.to]) {         // enqueue each neighbour once
                    visited[e.to] = true;
                    queue.enqueue(e.to);
                }
            }
        }
        return order;
    }

    // ---------------------------------------------------------------------
    // Depth-First Search (iterative, using our stack)
    // ---------------------------------------------------------------------
    /**
     * Iterative DFS from a source vertex, returning vertices in visit order.
     * An explicit {@link ArrayStack} is used instead of recursion so deep graphs
     * cannot overflow the JVM call stack.
     * <p><b>Time:</b> O(V + E). <b>Space:</b> O(V).</p>
     */
    public static DynamicArray<Integer> dfs(Graph g, int source) {
        boolean[] visited = new boolean[g.vertexCount()];
        DynamicArray<Integer> order = new DynamicArray<>();
        ArrayStack<Integer> stack = new ArrayStack<>();
        stack.push(source);
        while (!stack.isEmpty()) {
            int u = stack.pop();
            if (visited[u]) continue;   // may have been pushed more than once
            visited[u] = true;
            order.add(u);
            // push neighbours; they will be popped (and visited) LIFO
            for (Graph.Edge e : g.neighbors(u)) if (!visited[e.to]) stack.push(e.to);
        }
        return order;
    }

    /** The set of vertices reachable from source (via BFS), as indices. */
    public static DynamicArray<Integer> reachableFrom(Graph g, int source) {
        return bfs(g, source);
    }

    // ---------------------------------------------------------------------
    // Dijkstra's shortest path
    // ---------------------------------------------------------------------
    /** Result of a Dijkstra run: distances and predecessors for path rebuild. */
    public static final class ShortestPaths {
        public final double[] dist;  // dist[v] = cost of the cheapest source->v path
        public final int[] prev;     // prev[v] = predecessor of v on that path (-1 if none)
        public final int source;
        ShortestPaths(double[] dist, int[] prev, int source) { this.dist = dist; this.prev = prev; this.source = source; }

        /** Rebuild the vertex path source..target, or empty if unreachable. */
        public DynamicArray<Integer> pathTo(int target) {
            DynamicArray<Integer> path = new DynamicArray<>();
            if (dist[target] == Double.POSITIVE_INFINITY) return path; // unreachable
            ArrayStack<Integer> s = new ArrayStack<>();
            for (int at = target; at != -1; at = prev[at]) s.push(at);
            while (!s.isEmpty()) path.add(s.pop()); // reverse into source->target order
            return path;
        }
    }

    /** Heap entry for Dijkstra/Prim: a vertex tagged with a tentative key. */
    private static final class VD {
        final int v; final double d;
        VD(int v, double d) { this.v = v; this.d = d; }
    }
    private static final Comparator<VD> BY_D = (x, y) -> Double.compare(x.d, y.d);

    /**
     * Dijkstra's shortest paths from {@code source} over non-negative edge
     * weights (travel times here). Uses a binary min-heap with lazy deletion
     * (stale entries are skipped rather than decreased in place).
     * <p><b>Time:</b> O((V + E) log V). <b>Space:</b> O(V).</p>
     * <p><b>Correctness:</b> when a vertex is first popped, its recorded distance
     * is final — because all edge weights are ≥ 0, no later path through
     * higher-distance vertices can improve it (the standard greedy exchange
     * argument). This is why Dijkstra requires non-negative weights.</p>
     */
    public static ShortestPaths dijkstra(Graph g, int source) {
        int n = g.vertexCount();
        double[] dist = new double[n];
        int[] prev = new int[n];
        boolean[] settled = new boolean[n];
        for (int i = 0; i < n; i++) { dist[i] = Double.POSITIVE_INFINITY; prev[i] = -1; }
        dist[source] = 0.0;
        BinaryHeap<VD> pq = new BinaryHeap<>(BY_D);
        pq.insert(new VD(source, 0.0));
        while (!pq.isEmpty()) {
            VD top = pq.extractTop();
            int u = top.v;
            if (settled[u]) continue;         // lazy-deleted stale entry
            settled[u] = true;
            for (Graph.Edge e : g.neighbors(u)) {
                double nd = dist[u] + e.weight; // relax edge u->e.to
                if (nd < dist[e.to]) {
                    dist[e.to] = nd;
                    prev[e.to] = u;
                    pq.insert(new VD(e.to, nd)); // push improved estimate
                }
            }
        }
        return new ShortestPaths(dist, prev, source);
    }

    // ---------------------------------------------------------------------
    // Minimum Spanning Tree — result holder shared by Prim and Kruskal
    // ---------------------------------------------------------------------
    public static final class MST {
        public final DynamicArray<Graph.Edge> edges = new DynamicArray<>();
        public double totalWeight = 0.0;
    }

    /**
     * Prim's MST grown from vertex 0 using a binary min-heap of crossing edges.
     * Assumes an undirected, connected graph; on a disconnected graph it returns
     * the MST of the component containing vertex 0.
     * <p><b>Time:</b> O(E log V). <b>Space:</b> O(V + E).</p>
     * <p><b>Correctness (cut property):</b> repeatedly adding the cheapest edge
     * crossing the cut between the tree and the rest is always safe — that edge
     * belongs to some MST — so the greedy growth yields a minimum spanning tree.</p>
     */
    public static MST prim(Graph g) {
        int n = g.vertexCount();
        MST mst = new MST();
        if (n == 0) return mst;
        boolean[] inTree = new boolean[n];
        BinaryHeap<Graph.Edge> pq = new BinaryHeap<>((a, b) -> Double.compare(a.weight, b.weight));
        inTree[0] = true;
        for (Graph.Edge e : g.neighbors(0)) pq.insert(e);
        int joined = 1;
        while (!pq.isEmpty() && joined < n) {
            Graph.Edge e = pq.extractTop();  // cheapest edge leaving the tree
            if (inTree[e.to]) continue;      // both endpoints already in tree
            inTree[e.to] = true;
            joined++;
            mst.edges.add(e);
            mst.totalWeight += e.weight;
            for (Graph.Edge next : g.neighbors(e.to)) if (!inTree[next.to]) pq.insert(next);
        }
        return mst;
    }

    /**
     * Kruskal's MST: sort all edges ascending, then add each edge whose endpoints
     * are in different components (tested with our {@link DisjointSet}).
     * <p><b>Time:</b> O(E log E) dominated by the sort. <b>Space:</b> O(V + E).</p>
     * <p><b>Correctness (cycle property):</b> considering edges cheapest-first and
     * skipping any that would close a cycle keeps the safe cheapest edges only;
     * union-find detects a cycle exactly when both endpoints already share a
     * component, giving a minimum spanning forest (a tree if connected).</p>
     */
    public static MST kruskal(Graph g) {
        MST mst = new MST();
        int n = g.vertexCount();
        if (n == 0) return mst;
        // Copy edges into a plain array and sort with our own merge sort.
        DynamicArray<Graph.Edge> all = g.allEdges();
        Graph.Edge[] edges = new Graph.Edge[all.size()];
        for (int i = 0; i < all.size(); i++) edges[i] = all.get(i);
        SortAlgorithms.mergeSort(edges, (a, b) -> Double.compare(a.weight, b.weight));

        DisjointSet dsu = new DisjointSet(n);
        for (Graph.Edge e : edges) {
            if (dsu.union(e.from, e.to)) {   // union returns false if it forms a cycle
                mst.edges.add(e);
                mst.totalWeight += e.weight;
                if (mst.edges.size() == n - 1) break; // a spanning tree is complete
            }
        }
        return mst;
    }
}
