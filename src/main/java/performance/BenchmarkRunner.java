package performance;

import algorithms.GraphAlgorithms;
import algorithms.SearchAlgorithms;
import algorithms.SortAlgorithms;
import datastructures.AVLTree;
import datastructures.BinaryHeap;
import datastructures.BinarySearchTree;
import datastructures.Graph;
import datastructures.HashTable;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Random;

/**
 * Performance benchmark harness. Times the project's own data structures and
 * algorithms across a range of input sizes, each measurement <b>averaged over 3
 * trials</b>, and:
 * <ol>
 *   <li>writes the raw numbers as CSV into {@code performance/experiment-results/}, and</li>
 *   <li>renders simple line charts (via {@link SvgChart}) into {@code performance/graphs/}.</li>
 * </ol>
 *
 * <p>Experiments (brief §6): search algorithms; sort algorithms; hash-table
 * operations at varying load factors; BST vs. balanced (AVL) tree height and
 * search time; heap build/dispatch; and the graph algorithms. Input sizes are
 * 100/500/1,000/5,000/10,000, and 50/100/200/500 for the graph experiments.</p>
 *
 * <p>Run with: {@code bash build.sh bench} (or {@code java -cp out/main performance.BenchmarkRunner}).</p>
 */
public final class BenchmarkRunner {

    private static final int[] SIZES = { 100, 500, 1_000, 5_000, 10_000 };
    private static final int[] GRAPH_SIZES = { 50, 100, 200, 500 };
    private static final int TRIALS = 3;
    private static final long SEED = 42L;

    private static final Path RESULTS = Path.of("performance", "experiment-results");
    private static final Path GRAPHS  = Path.of("performance", "graphs");

    public static void main(String[] args) throws IOException {
        Files.createDirectories(RESULTS);
        Files.createDirectories(GRAPHS);
        System.out.println("Running benchmarks (" + TRIALS + " trials each). This may take a minute...");

        warmup(); // trigger JIT compilation first so the first real measurement isn't an outlier
        benchmarkSorting();
        benchmarkSearching();
        benchmarkHashTable();
        benchmarkTrees();
        benchmarkHeap();
        benchmarkGraphs();

        System.out.println("Done. CSVs in " + RESULTS + ", charts in " + GRAPHS);
    }

    // ---------------------------------------------------------------
    // 1. Sorting: O(n^2) selection/insertion vs O(n log n) merge/quick
    // ---------------------------------------------------------------
    private static void benchmarkSorting() throws IOException {
        double[] xs = new double[SIZES.length];
        double[] sel = new double[SIZES.length], ins = new double[SIZES.length];
        double[] mrg = new double[SIZES.length], qk = new double[SIZES.length];
        try (PrintWriter w = csv("sorting.csv")) {
            w.println("n,selection_ms,insertion_ms,merge_ms,quick_ms");
            for (int i = 0; i < SIZES.length; i++) {
                int n = SIZES[i]; xs[i] = n;
                double s = 0, in = 0, m = 0, q = 0;
                for (int t = 0; t < TRIALS; t++) {
                    Integer[] base = randomInts(n, SEED + t);
                    s  += ms(() -> SortAlgorithms.selectionSort(copy(base), NAT));
                    in += ms(() -> SortAlgorithms.insertionSort(copy(base), NAT));
                    m  += ms(() -> SortAlgorithms.mergeSort(copy(base), NAT));
                    q  += ms(() -> SortAlgorithms.quickSort(copy(base), NAT));
                }
                sel[i] = s / TRIALS; ins[i] = in / TRIALS; mrg[i] = m / TRIALS; qk[i] = q / TRIALS;
                w.printf("%d,%.4f,%.4f,%.4f,%.4f%n", n, sel[i], ins[i], mrg[i], qk[i]);
            }
        }
        new SvgChart("Sorting: time vs input size", "n (elements)", "time (ms)")
            .addSeries("selection O(n^2)", xs, sel)
            .addSeries("insertion O(n^2)", xs, ins)
            .addSeries("merge O(n log n)", xs, mrg)
            .addSeries("quick O(n log n)", xs, qk)
            .write(GRAPHS.resolve("sorting.svg"));
        System.out.println("  [1/6] sorting done");
    }

    // ---------------------------------------------------------------
    // 2. Searching: linear O(n) vs binary O(log n) on a sorted array
    // ---------------------------------------------------------------
    private static void benchmarkSearching() throws IOException {
        final int QUERIES = 5_000;
        double[] xs = new double[SIZES.length];
        double[] lin = new double[SIZES.length], bin = new double[SIZES.length];
        try (PrintWriter w = csv("searching.csv")) {
            w.println("n,linear_ns_per_query,binary_ns_per_query");
            for (int i = 0; i < SIZES.length; i++) {
                int n = SIZES[i]; xs[i] = n;
                Integer[] sorted = new Integer[n];
                for (int k = 0; k < n; k++) sorted[k] = k * 2; // sorted, even numbers
                Random rng = new Random(SEED);
                Integer[] targets = new Integer[QUERIES];
                for (int k = 0; k < QUERIES; k++) targets[k] = rng.nextInt(n * 2 + 1); // mix hits/misses
                double l = 0, b = 0;
                for (int t = 0; t < TRIALS; t++) {
                    l += ns(() -> { for (Integer q : targets) SearchAlgorithms.linearSearch(sorted, q, NAT); }) / (double) QUERIES;
                    b += ns(() -> { for (Integer q : targets) SearchAlgorithms.binarySearch(sorted, q, NAT); }) / (double) QUERIES;
                }
                lin[i] = l / TRIALS; bin[i] = b / TRIALS;
                w.printf("%d,%.2f,%.2f%n", n, lin[i], bin[i]);
            }
        }
        new SvgChart("Searching: ns per query vs input size", "n (elements)", "ns per query")
            .addSeries("linear O(n)", xs, lin)
            .addSeries("binary O(log n)", xs, bin)
            .write(GRAPHS.resolve("searching.svg"));
        System.out.println("  [2/6] searching done");
    }

    // ---------------------------------------------------------------
    // 3. Hash table get/put at varying load factors (fixed bucket count)
    // ---------------------------------------------------------------
    private static void benchmarkHashTable() throws IOException {
        double[] loadFactors = { 0.25, 0.5, 0.75, 1.0, 2.0, 4.0 };
        final int BUCKETS = 4_096;
        double[] xs = new double[loadFactors.length];
        double[] put = new double[loadFactors.length], get = new double[loadFactors.length];
        try (PrintWriter w = csv("hashtable.csv")) {
            w.println("load_factor,put_ns,get_ns");
            for (int i = 0; i < loadFactors.length; i++) {
                double lf = loadFactors[i]; xs[i] = lf;
                int n = (int) (lf * BUCKETS);
                double p = 0, g = 0;
                for (int t = 0; t < TRIALS; t++) {
                    // maxLoadFactor huge => the table never auto-resizes, so LF is honoured.
                    HashTable<Integer, Integer> h = new HashTable<>(BUCKETS, Double.MAX_VALUE);
                    p += ns(() -> { for (int k = 0; k < n; k++) h.put(k, k); }) / (double) n;
                    g += ns(() -> { for (int k = 0; k < n; k++) h.get(k); }) / (double) n;
                }
                put[i] = p / TRIALS; get[i] = g / TRIALS;
                w.printf("%.2f,%.2f,%.2f%n", lf, put[i], get[i]);
            }
        }
        new SvgChart("Hash table: ns per operation vs load factor", "load factor (n / buckets)", "ns per op")
            .addSeries("put", xs, put)
            .addSeries("get", xs, get)
            .write(GRAPHS.resolve("hashtable.svg"));
        System.out.println("  [3/6] hash table done");
    }

    // ---------------------------------------------------------------
    // 4. BST vs AVL: height and search time on SORTED (worst-case) input
    // ---------------------------------------------------------------
    private static void benchmarkTrees() throws IOException {
        double[] xs = new double[SIZES.length];
        double[] bstH = new double[SIZES.length], avlH = new double[SIZES.length];
        double[] bstS = new double[SIZES.length], avlS = new double[SIZES.length];
        try (PrintWriter w = csv("trees.csv")) {
            w.println("n,bst_height,avl_height,bst_search_ns,avl_search_ns");
            for (int i = 0; i < SIZES.length; i++) {
                int n = SIZES[i]; xs[i] = n;
                int sample = Math.min(n, 2_000); // bound the degenerate-BST search cost
                double bh = 0, ah = 0, bs = 0, as = 0;
                for (int t = 0; t < TRIALS; t++) {
                    BinarySearchTree<Integer, Integer> bst = new BinarySearchTree<>();
                    AVLTree<Integer, Integer> avl = new AVLTree<>();
                    for (int k = 0; k < n; k++) { bst.insert(k, k); avl.insert(k, k); } // sorted inserts
                    bh += bst.height(); ah += avl.height();
                    bs += ns(() -> { for (int k = 0; k < sample; k++) bst.search(k); }) / (double) sample;
                    as += ns(() -> { for (int k = 0; k < sample; k++) avl.search(k); }) / (double) sample;
                }
                bstH[i] = bh / TRIALS; avlH[i] = ah / TRIALS;
                bstS[i] = bs / TRIALS; avlS[i] = as / TRIALS;
                w.printf("%d,%.1f,%.1f,%.2f,%.2f%n", n, bstH[i], avlH[i], bstS[i], avlS[i]);
            }
        }
        new SvgChart("Tree height vs input size (sorted insert)", "n (keys)", "height")
            .addSeries("BST (unbalanced)", xs, bstH)
            .addSeries("AVL (balanced)", xs, avlH)
            .write(GRAPHS.resolve("tree-height.svg"));
        new SvgChart("Tree search time vs input size", "n (keys)", "ns per search")
            .addSeries("BST (unbalanced)", xs, bstS)
            .addSeries("AVL (balanced)", xs, avlS)
            .write(GRAPHS.resolve("tree-search.svg"));
        System.out.println("  [4/6] trees done");
    }

    // ---------------------------------------------------------------
    // 5. Binary heap: build (n inserts) and dispatch (n extracts)
    // ---------------------------------------------------------------
    private static void benchmarkHeap() throws IOException {
        double[] xs = new double[SIZES.length];
        double[] build = new double[SIZES.length], dispatch = new double[SIZES.length];
        try (PrintWriter w = csv("heap.csv")) {
            w.println("n,build_ms,dispatch_ms");
            for (int i = 0; i < SIZES.length; i++) {
                int n = SIZES[i]; xs[i] = n;
                double bd = 0, ds = 0;
                for (int t = 0; t < TRIALS; t++) {
                    Integer[] vals = randomInts(n, SEED + t);
                    BinaryHeap<Integer> h = new BinaryHeap<>(NAT);
                    bd += ms(() -> { for (Integer v : vals) h.insert(v); });
                    ds += ms(() -> { while (!h.isEmpty()) h.extractTop(); });
                }
                build[i] = bd / TRIALS; dispatch[i] = ds / TRIALS;
                w.printf("%d,%.4f,%.4f%n", n, build[i], dispatch[i]);
            }
        }
        new SvgChart("Binary heap: build & dispatch vs input size", "n (elements)", "time (ms)")
            .addSeries("build (n inserts)", xs, build)
            .addSeries("dispatch (n extracts)", xs, dispatch)
            .write(GRAPHS.resolve("heap.svg"));
        System.out.println("  [5/6] heap done");
    }

    // ---------------------------------------------------------------
    // 6. Graph algorithms on random connected graphs (E ~ 6V)
    // ---------------------------------------------------------------
    private static void benchmarkGraphs() throws IOException {
        double[] xs = new double[GRAPH_SIZES.length];
        double[] bfs = new double[GRAPH_SIZES.length], dfs = new double[GRAPH_SIZES.length];
        double[] dij = new double[GRAPH_SIZES.length], prim = new double[GRAPH_SIZES.length], kru = new double[GRAPH_SIZES.length];
        try (PrintWriter w = csv("graph.csv")) {
            w.println("v,e,bfs_ms,dfs_ms,dijkstra_ms,prim_ms,kruskal_ms");
            for (int i = 0; i < GRAPH_SIZES.length; i++) {
                int v = GRAPH_SIZES[i]; xs[i] = v;
                double b = 0, d = 0, dj = 0, pr = 0, kr = 0; int edges = 0;
                for (int t = 0; t < TRIALS; t++) {
                    Graph g = randomConnectedGraph(v, 6, SEED + t);
                    edges = g.edgeCount();
                    b  += ms(() -> GraphAlgorithms.bfs(g, 0));
                    d  += ms(() -> GraphAlgorithms.dfs(g, 0));
                    dj += ms(() -> GraphAlgorithms.dijkstra(g, 0));
                    pr += ms(() -> GraphAlgorithms.prim(g));
                    kr += ms(() -> GraphAlgorithms.kruskal(g));
                }
                bfs[i] = b / TRIALS; dfs[i] = d / TRIALS; dij[i] = dj / TRIALS; prim[i] = pr / TRIALS; kru[i] = kr / TRIALS;
                w.printf("%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f%n", v, edges, bfs[i], dfs[i], dij[i], prim[i], kru[i]);
            }
        }
        new SvgChart("Graph algorithms: time vs |V| (E ~ 6V)", "|V| (vertices)", "time (ms)")
            .addSeries("BFS", xs, bfs)
            .addSeries("DFS", xs, dfs)
            .addSeries("Dijkstra", xs, dij)
            .addSeries("Prim", xs, prim)
            .addSeries("Kruskal", xs, kru)
            .write(GRAPHS.resolve("graph.svg"));
        System.out.println("  [6/6] graph done");
    }

    // ---------------------------- helpers ----------------------------

    private static final Comparator<Integer> NAT = Comparator.naturalOrder();

    /**
     * Exercise every structure/algorithm a few times before timing, so the JVM's
     * just-in-time compiler has optimised the hot paths. Without this, the very
     * first measurement of each experiment is inflated by compilation cost.
     */
    private static void warmup() {
        for (int r = 0; r < 3; r++) {
            Integer[] a = randomInts(3_000, r);
            SortAlgorithms.mergeSort(copy(a), NAT);
            SortAlgorithms.quickSort(copy(a), NAT);
            SortAlgorithms.insertionSort(copy(a), NAT);
            SortAlgorithms.selectionSort(copy(a), NAT);
            for (int k = 0; k < 3_000; k++) { SearchAlgorithms.linearSearch(a, k, NAT); SearchAlgorithms.binarySearch(a, k, NAT); }
            HashTable<Integer, Integer> h = new HashTable<>(1_024, Double.MAX_VALUE);
            for (int k = 0; k < 2_000; k++) h.put(k, k);
            for (int k = 0; k < 2_000; k++) h.get(k);
            BinaryHeap<Integer> heap = new BinaryHeap<>(NAT);
            for (Integer v : a) heap.insert(v);
            while (!heap.isEmpty()) heap.extractTop();
            BinarySearchTree<Integer, Integer> bst = new BinarySearchTree<>();
            AVLTree<Integer, Integer> avl = new AVLTree<>();
            for (int k = 0; k < 1_000; k++) { bst.insert(k, k); avl.insert(k, k); }
            Graph g = randomConnectedGraph(200, 6, r);
            GraphAlgorithms.bfs(g, 0); GraphAlgorithms.dfs(g, 0);
            GraphAlgorithms.dijkstra(g, 0); GraphAlgorithms.prim(g); GraphAlgorithms.kruskal(g);
        }
        System.out.println("  (warmup complete)");
    }

    /** Time a task in milliseconds. */
    private static double ms(Runnable r) { long t0 = System.nanoTime(); r.run(); return (System.nanoTime() - t0) / 1_000_000.0; }
    /** Time a task in nanoseconds (total). */
    private static long ns(Runnable r)   { long t0 = System.nanoTime(); r.run(); return System.nanoTime() - t0; }

    private static Integer[] randomInts(int n, long seed) {
        Random rng = new Random(seed);
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) a[i] = rng.nextInt();
        return a;
    }

    private static Integer[] copy(Integer[] src) {
        Integer[] c = new Integer[src.length];
        System.arraycopy(src, 0, c, 0, src.length);
        return c;
    }

    /** Random undirected connected graph: a spanning chain plus extra edges to ~degree. */
    private static Graph randomConnectedGraph(int v, int avgDegree, long seed) {
        Random rng = new Random(seed);
        Graph g = new Graph();
        for (int i = 0; i < v; i++) g.addVertex("V" + i);
        for (int i = 1; i < v; i++) g.addEdge("V" + i, "V" + rng.nextInt(i), 1 + rng.nextInt(100), true);
        int extra = avgDegree * v / 2;
        for (int e = 0; e < extra; e++) {
            int a = rng.nextInt(v), b = rng.nextInt(v);
            if (a != b) g.addEdge("V" + a, "V" + b, 1 + rng.nextInt(100), true);
        }
        return g;
    }

    private static PrintWriter csv(String name) throws IOException {
        return new PrintWriter(Files.newBufferedWriter(RESULTS.resolve(name)));
    }

    private BenchmarkRunner() {}
}
