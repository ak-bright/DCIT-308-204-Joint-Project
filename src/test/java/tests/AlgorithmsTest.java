package tests;

import algorithms.*;
import datastructures.DynamicArray;
import datastructures.Graph;
import model.Resource;
import model.ServiceRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the hand-written algorithms. Covers searching, sorting, the
 * graph algorithms, the greedy dispatcher (including its explicit
 * NON-OPTIMAL counterexample), and the DP selector (including the worked table
 * values and an invalid-precondition case).
 */
class AlgorithmsTest {

    private static final Comparator<Integer> NAT = Comparator.naturalOrder();

    private static Integer[] arr(int... xs) {
        Integer[] a = new Integer[xs.length];
        for (int i = 0; i < xs.length; i++) a[i] = xs[i];
        return a;
    }

    // ---------------- Searching ----------------

    @Test @DisplayName("linearSearch: found / not found (normal + invalid)")
    void linearSearch() {
        Integer[] a = arr(4, 2, 7, 1, 9);
        assertEquals(2, SearchAlgorithms.linearSearch(a, 7, NAT));
        assertEquals(-1, SearchAlgorithms.linearSearch(a, 100, NAT)); // absent
    }

    @Test @DisplayName("binarySearch: found in sorted array + boundary ends")
    void binarySearchNormal() {
        Integer[] a = arr(1, 3, 5, 7, 9, 11);
        assertEquals(0, SearchAlgorithms.binarySearch(a, 1, NAT));   // first
        assertEquals(5, SearchAlgorithms.binarySearch(a, 11, NAT));  // last
        assertEquals(2, SearchAlgorithms.binarySearch(a, 5, NAT));   // middle
        assertEquals(-1, SearchAlgorithms.binarySearch(a, 6, NAT));  // gap
    }

    @Test @DisplayName("binarySearch: UNSORTED input violates precondition and misses (invalid)")
    void binarySearchUnsorted() {
        // Precondition (sorted) is broken on purpose: binary search may miss an
        // element that is definitely present. Linear search still finds it.
        Integer[] unsorted = arr(5, 1, 3, 2, 4);
        assertEquals(1, SearchAlgorithms.linearSearch(unsorted, 1, NAT)); // present at idx 1
        assertEquals(-1, SearchAlgorithms.binarySearch(unsorted, 1, NAT)); // but binary search misses it
    }

    @Test @DisplayName("binarySearch: empty array returns -1 (boundary)")
    void binarySearchEmpty() {
        assertEquals(-1, SearchAlgorithms.binarySearch(new Integer[0], 5, NAT));
    }

    // ---------------- Sorting ----------------

    @Test @DisplayName("all four sorts order a normal array identically")
    void sortsAgree() {
        int[] base = {9, 3, 7, 1, 8, 2, 6, 5, 4, 0, 3, 7};
        Integer[] sel = arr(base), ins = arr(base), mrg = arr(base), qk = arr(base);
        SortAlgorithms.selectionSort(sel, NAT);
        SortAlgorithms.insertionSort(ins, NAT);
        SortAlgorithms.mergeSort(mrg, NAT);
        SortAlgorithms.quickSort(qk, NAT);
        assertTrue(SortAlgorithms.isSorted(sel, NAT));
        assertTrue(SortAlgorithms.isSorted(ins, NAT));
        assertTrue(SortAlgorithms.isSorted(mrg, NAT));
        assertTrue(SortAlgorithms.isSorted(qk, NAT));
        assertArrayEquals(sel, mrg); // same multiset, same order
        assertArrayEquals(mrg, qk);
    }

    @Test @DisplayName("sorts: empty and single-element arrays (boundary)")
    void sortsBoundary() {
        Integer[] empty = new Integer[0];
        Integer[] one = arr(42);
        SortAlgorithms.mergeSort(empty, NAT);   // must not throw
        SortAlgorithms.quickSort(one, NAT);
        assertEquals(42, one[0]);
        SortAlgorithms.selectionSort(one, NAT);
        SortAlgorithms.insertionSort(empty, NAT);
    }

    @Test @DisplayName("sorts: already-sorted and reverse (duplicate/adversarial)")
    void sortsAdversarial() {
        Integer[] sorted = arr(1, 2, 3, 4, 5);
        Integer[] reverse = arr(5, 4, 3, 2, 1);
        SortAlgorithms.quickSort(sorted, NAT);   // median-of-three keeps this fast
        SortAlgorithms.quickSort(reverse, NAT);
        assertTrue(SortAlgorithms.isSorted(sorted, NAT));
        assertTrue(SortAlgorithms.isSorted(reverse, NAT));
    }

    @Test @DisplayName("insertionSort is stable (equal keys keep input order)")
    void insertionStable() {
        // Sort pairs by first component only; equal firsts must keep their order.
        Integer[][] data = { {1, 0}, {1, 1}, {0, 2}, {1, 3} };
        SortAlgorithms.insertionSort(data, Comparator.comparingInt(p -> p[0]));
        // After sorting by key: 0 then the three 1s in original relative order 0,1,3
        assertArrayEquals(new Integer[]{0, 2}, data[0]);
        assertArrayEquals(new Integer[]{1, 0}, data[1]);
        assertArrayEquals(new Integer[]{1, 1}, data[2]);
        assertArrayEquals(new Integer[]{1, 3}, data[3]);
    }

    // ---------------- Graph algorithms ----------------

    private Graph lineGraph() {
        // A-B-C-D chain plus a separate isolated vertex E (disconnected component).
        Graph g = new Graph();
        g.addEdge("A", "B", 1, true);
        g.addEdge("B", "C", 1, true);
        g.addEdge("C", "D", 1, true);
        g.addVertex("E"); // no edges -> unreachable
        return g;
    }

    @Test @DisplayName("BFS/DFS: reach exactly the connected component (normal)")
    void bfsDfsReach() {
        Graph g = lineGraph();
        int a = g.indexOf("A");
        assertEquals(4, GraphAlgorithms.bfs(g, a).size()); // A,B,C,D but not E
        assertEquals(4, GraphAlgorithms.dfs(g, a).size());
    }

    @Test @DisplayName("BFS: single-vertex start (boundary)")
    void bfsSingle() {
        Graph g = new Graph();
        g.addVertex("solo");
        assertEquals(1, GraphAlgorithms.bfs(g, 0).size());
    }

    @Test @DisplayName("Dijkstra: shortest path costs and reconstruction (normal)")
    void dijkstraNormal() {
        Graph g = new Graph();
        g.addEdge("A", "B", 4, true);
        g.addEdge("A", "C", 1, true);
        g.addEdge("C", "B", 2, true); // A->C->B (3) beats A->B (4)
        g.addEdge("B", "D", 1, true);
        GraphAlgorithms.ShortestPaths sp = GraphAlgorithms.dijkstra(g, g.indexOf("A"));
        assertEquals(3.0, sp.dist[g.indexOf("B")]);
        assertEquals(4.0, sp.dist[g.indexOf("D")]);
        DynamicArray<Integer> path = sp.pathTo(g.indexOf("D")); // A,C,B,D
        assertEquals(4, path.size());
        assertEquals("A", g.idOf(path.get(0)));
        assertEquals("D", g.idOf(path.get(3)));
    }

    @Test @DisplayName("Dijkstra: unreachable vertex has INF distance and empty path (invalid)")
    void dijkstraUnreachable() {
        Graph g = lineGraph();
        GraphAlgorithms.ShortestPaths sp = GraphAlgorithms.dijkstra(g, g.indexOf("A"));
        assertEquals(Double.POSITIVE_INFINITY, sp.dist[g.indexOf("E")]);
        assertTrue(sp.pathTo(g.indexOf("E")).isEmpty());
    }

    @Test @DisplayName("Prim and Kruskal agree on MST total weight (normal + correctness)")
    void mstAgrees() {
        Graph g = new Graph();
        g.addEdge("A", "B", 1, true);
        g.addEdge("B", "C", 2, true);
        g.addEdge("A", "C", 3, true); // heaviest edge should be excluded
        g.addEdge("C", "D", 4, true);
        GraphAlgorithms.MST prim = GraphAlgorithms.prim(g);
        GraphAlgorithms.MST krus = GraphAlgorithms.kruskal(g);
        assertEquals(3, prim.edges.size());          // V-1 edges
        assertEquals(3, krus.edges.size());
        assertEquals(7.0, prim.totalWeight);         // 1+2+4
        assertEquals(prim.totalWeight, krus.totalWeight);
    }

    @Test @DisplayName("Kruskal: disconnected graph yields a spanning forest (invalid/boundary)")
    void kruskalForest() {
        Graph g = lineGraph(); // A-B-C-D plus isolated E
        GraphAlgorithms.MST mst = GraphAlgorithms.kruskal(g);
        // 5 vertices, 2 components => 5 - 2 = 3 tree edges (a forest, not a tree).
        assertEquals(3, mst.edges.size());
    }

    // ---------------- Greedy assignment (with counterexample) ----------------

    private ServiceRequest req(String id, int urgency, int serviceMinutes, int value) {
        ServiceRequest r = new ServiceRequest(id, "S", "D", "test", urgency, "", "", "PENDING");
        r.setServiceMinutes(serviceMinutes);
        r.setValue(value);
        return r;
    }

    @Test @DisplayName("Greedy assignment assigns most-urgent-first when capacity allows (normal)")
    void greedyNormal() {
        DynamicArray<ServiceRequest> reqs = new DynamicArray<>();
        reqs.add(req("A", 3, 5, 10));
        reqs.add(req("B", 1, 5, 10));
        DynamicArray<Resource> res = new DynamicArray<>();
        res.add(new Resource("R1", "nurse", "L1", 100, "AVAILABLE"));
        GreedyAssignment.Result r = GreedyAssignment.assign(reqs, res);
        assertEquals(2, r.assignedCount);
        // Most urgent (B, urgency 1) is decided first.
        assertEquals("B", r.assignments.get(0).request.getRequestId());
    }

    @Test @DisplayName("Greedy COUNTEREXAMPLE: urgency-first is NOT throughput-optimal (explicit)")
    void greedyCounterexample() {
        // Single team, capacity 10. A(urg1,10min) blocks B(urg2,5) and C(urg3,5).
        DynamicArray<ServiceRequest> reqs = new DynamicArray<>();
        reqs.add(req("A", 1, 10, 10));
        reqs.add(req("B", 2, 5, 8));
        reqs.add(req("C", 3, 5, 8));
        DynamicArray<Resource> res = new DynamicArray<>();
        res.add(new Resource("TEAM", "team", "L1", 10, "AVAILABLE"));

        GreedyAssignment.Result greedy = GreedyAssignment.assign(reqs, res);
        assertEquals(1, greedy.assignedCount);   // greedy completes only A
        assertEquals(2, greedy.unassignedCount);

        // The DP (throughput/value objective) does strictly better: B + C.
        DynamicSelection.Selection dp = DynamicSelection.selectBest(reqs, 10);
        assertEquals(2, dp.chosen.size());        // two requests instead of one
        assertEquals(16, dp.totalValue);          // 8 + 8 > greedy's single value 10
    }

    @Test @DisplayName("Greedy: OFFLINE resources are ignored (boundary/invalid)")
    void greedyIgnoresUnavailable() {
        DynamicArray<ServiceRequest> reqs = new DynamicArray<>();
        reqs.add(req("A", 1, 5, 10));
        DynamicArray<Resource> res = new DynamicArray<>();
        res.add(new Resource("R1", "nurse", "L1", 100, "OFFLINE"));
        GreedyAssignment.Result r = GreedyAssignment.assign(reqs, res);
        assertEquals(0, r.assignedCount);
        assertEquals(1, r.unassignedCount);
    }

    // ---------------- DP selection (with worked table + invalid) ----------------

    @Test @DisplayName("DP knapsack: reproduces the worked table answer (normal/correctness)")
    void dpWorkedExample() {
        // Exactly the table in DynamicSelection's Javadoc: answer 16 = {B, C}.
        DynamicArray<ServiceRequest> reqs = new DynamicArray<>();
        reqs.add(req("A", 1, 10, 10));
        reqs.add(req("B", 2, 5, 8));
        reqs.add(req("C", 3, 5, 8));
        DynamicSelection.Selection sel = DynamicSelection.selectBest(reqs, 10);
        assertEquals(16, sel.totalValue);
        assertEquals(10, sel.totalWeight);
        assertEquals(2, sel.chosen.size());
    }

    @Test @DisplayName("DP knapsack: zero capacity selects nothing (boundary)")
    void dpZeroCapacity() {
        DynamicArray<ServiceRequest> reqs = new DynamicArray<>();
        reqs.add(req("A", 1, 5, 10));
        DynamicSelection.Selection sel = DynamicSelection.selectBest(reqs, 0);
        assertEquals(0, sel.totalValue);
        assertEquals(0, sel.chosen.size());
    }

    @Test @DisplayName("DP knapsack: negative capacity rejected (invalid precondition)")
    void dpNegativeCapacity() {
        DynamicArray<ServiceRequest> reqs = new DynamicArray<>();
        reqs.add(req("A", 1, 5, 10));
        assertThrows(IllegalArgumentException.class, () -> DynamicSelection.selectBest(reqs, -5));
    }
}
