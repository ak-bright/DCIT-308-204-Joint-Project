package datastructures;

/**
 * Weighted graph implemented from scratch, keeping <b>both</b> representations
 * required by the brief in sync:
 * <ul>
 *   <li><b>Adjacency list</b> — a {@link DynamicArray} per vertex of outgoing
 *       {@link Edge}s. Space O(V+E); ideal for the sparse hospital corridor
 *       network and for BFS/DFS/Dijkstra/Prim.</li>
 *   <li><b>Adjacency matrix</b> — a V×V array of weights ({@code INF} where no
 *       edge exists). Space O(V²); gives O(1) "is there a direct corridor A→B?"
 *       and is what the performance study contrasts against the list.</li>
 * </ul>
 *
 * <p>Vertices are external string ids (e.g. locationIds) mapped to dense integer
 * indices via our own {@link HashMap}, so algorithms can use fast int-indexed
 * arrays internally while callers speak in location ids.</p>
 *
 * <p><b>Where it is used in the system:</b> it is the road network. One graph is
 * built with edge weight = travel time (for "fastest route" via Dijkstra and
 * "reachable from here" via BFS/DFS); another with edge weight = distance (for
 * the "cheapest network" MST via Prim/Kruskal).</p>
 */
public class Graph {

    /** A weighted directed edge (undirected corridors are added as two edges). */
    public static final class Edge {
        public final int from;
        public final int to;
        public final double weight;
        public Edge(int from, int to, double weight) { this.from = from; this.to = to; this.weight = weight; }
    }

    /** Sentinel "no edge" weight for the adjacency matrix. */
    public static final double INF = Double.POSITIVE_INFINITY;

    private final DynamicArray<String> indexToId = new DynamicArray<>();   // vertex index -> id
    private final HashMap<String, Integer> idToIndex = new HashMap<>();    // id -> vertex index
    private final DynamicArray<DynamicArray<Edge>> adjList = new DynamicArray<>(); // list form
    private double[][] adjMatrix;   // matrix form, lazily (re)built as vertices are added
    private int vertexCount;
    private int edgeCount;

    public Graph() { adjMatrix = new double[0][0]; }

    public int vertexCount() { return vertexCount; }
    public int edgeCount()   { return edgeCount; }

    /** Add a vertex by id if not already present; returns its index. */
    public int addVertex(String id) {
        Integer existing = idToIndex.get(id);
        if (existing != null) return existing;
        int index = vertexCount++;
        idToIndex.put(id, index);
        indexToId.add(id);
        adjList.add(new DynamicArray<>());
        growMatrix(vertexCount); // keep the matrix square and in step with the list
        return index;
    }

    /** Index of an id, or -1 if the id is unknown. */
    public int indexOf(String id) {
        Integer i = idToIndex.get(id);
        return i == null ? -1 : i;
    }

    /** Id of a vertex index. */
    public String idOf(int index) { return indexToId.get(index); }

    /**
     * Add an edge between two ids with the given weight. Vertices are created on
     * demand. When {@code undirected} is true we insert both directions, which is
     * how physical corridors (walkable both ways) are modelled.
     */
    public void addEdge(String fromId, String toId, double weight, boolean undirected) {
        int u = addVertex(fromId);
        int v = addVertex(toId);
        addDirected(u, v, weight);
        if (undirected) addDirected(v, u, weight);
    }

    private void addDirected(int u, int v, double weight) {
        adjList.get(u).add(new Edge(u, v, weight));
        adjMatrix[u][v] = weight; // matrix stays in sync with the list
        edgeCount++;
    }

    /** Outgoing edges of a vertex (adjacency-list view). */
    public DynamicArray<Edge> neighbors(int vertex) { return adjList.get(vertex); }

    /** Direct-edge weight from u to v, or {@link #INF} if none (matrix view). */
    public double weight(int u, int v) { return adjMatrix[u][v]; }

    /** The full V×V weight matrix (matrix view). Do not mutate. */
    public double[][] matrix() { return adjMatrix; }

    /** All edges of the graph as a flat list (used by Kruskal's MST). */
    public DynamicArray<Edge> allEdges() {
        DynamicArray<Edge> edges = new DynamicArray<>();
        for (int u = 0; u < vertexCount; u++)
            for (Edge e : adjList.get(u)) edges.add(e);
        return edges;
    }

    public DynamicArray<String> vertexIds() { return indexToId; }

    /** Grow (rebuild) the adjacency matrix to hold {@code n} vertices. */
    private void growMatrix(int n) {
        double[][] bigger = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                bigger[i][j] = (i < adjMatrix.length && j < adjMatrix.length) ? adjMatrix[i][j] : INF;
        // diagonal stays INF (no self-loops modelled); copied values preserved
        adjMatrix = bigger;
    }
}
