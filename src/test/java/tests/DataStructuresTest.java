package tests;

import datastructures.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the hand-written data structures. Every structure is checked
 * with a NORMAL case, a BOUNDARY case (empty / single / duplicate), and an
 * INVALID case (out-of-range access, popping empty, etc.), as the brief requires.
 */
class DataStructuresTest {

    // ---------------- DynamicArray ----------------

    @Test @DisplayName("DynamicArray: add/get/set/insert/remove (normal)")
    void dynamicArrayNormal() {
        DynamicArray<String> a = new DynamicArray<>(2);
        a.add("a"); a.add("b"); a.add("c");     // triggers a resize past capacity 2
        assertEquals(3, a.size());
        assertEquals("b", a.get(1));
        a.set(1, "B");
        assertEquals("B", a.get(1));
        a.insert(1, "x");                        // a, x, B, c
        assertEquals("x", a.get(1));
        assertEquals("a", a.remove(0));          // shift left
        assertEquals("x", a.get(0));
        assertEquals(3, a.size());
    }

    @Test @DisplayName("DynamicArray: empty is boundary, contains/indexOf work")
    void dynamicArrayBoundary() {
        DynamicArray<Integer> a = new DynamicArray<>();
        assertTrue(a.isEmpty());
        a.add(7);
        assertEquals(0, a.indexOf(7));
        assertEquals(-1, a.indexOf(99));
        assertTrue(a.contains(7));
    }

    @Test @DisplayName("DynamicArray: out-of-range get throws (invalid)")
    void dynamicArrayInvalid() {
        DynamicArray<Integer> a = new DynamicArray<>();
        a.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> a.get(5));
        assertThrows(IndexOutOfBoundsException.class, () -> a.remove(-1));
    }

    // ---------------- DoublyLinkedList ----------------

    @Test @DisplayName("DoublyLinkedList: addFirst/addLast/insertAfter/remove (normal)")
    void linkedListNormal() {
        DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
        l.addLast(2); l.addFirst(1); l.addLast(3);      // 1,2,3
        assertEquals(1, l.getFirst());
        assertEquals(3, l.getLast());
        assertTrue(l.insertAfter(2, 99));               // 1,2,99,3
        assertTrue(l.contains(99));
        assertTrue(l.remove(99));
        assertEquals(3, l.size());
    }

    @Test @DisplayName("DoublyLinkedList: insertAfter missing value returns false (boundary)")
    void linkedListInsertAfterMissing() {
        DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
        l.addLast(1);
        assertFalse(l.insertAfter(42, 100)); // 42 not present
        assertEquals(1, l.size());
    }

    @Test @DisplayName("DoublyLinkedList: getFirst on empty throws (invalid)")
    void linkedListInvalid() {
        DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
        assertThrows(NoSuchElementException.class, l::getFirst);
        assertFalse(l.remove(5));
    }

    // ---------------- ArrayStack ----------------

    @Test @DisplayName("ArrayStack: push/pop/peek LIFO (normal)")
    void stackNormal() {
        ArrayStack<Integer> s = new ArrayStack<>();
        s.push(1); s.push(2); s.push(3);
        assertEquals(3, s.peek());
        assertEquals(3, s.pop());
        assertEquals(2, s.pop());
        assertFalse(s.isEmpty());
    }

    @Test @DisplayName("ArrayStack: pop on empty throws (invalid/boundary)")
    void stackInvalid() {
        ArrayStack<Integer> s = new ArrayStack<>();
        assertTrue(s.isEmpty());
        assertThrows(java.util.EmptyStackException.class, s::pop);
    }

    // ---------------- CircularQueue ----------------

    @Test @DisplayName("CircularQueue: enqueue/dequeue with wrap-around (normal)")
    void circularQueueWrap() {
        CircularQueue<Integer> q = new CircularQueue<>(4);
        for (int i = 1; i <= 4; i++) q.enqueue(i);   // fill
        assertEquals(1, q.dequeue());
        assertEquals(2, q.dequeue());
        q.enqueue(5); q.enqueue(6);                   // wrap head/tail around
        assertEquals(3, q.dequeue());
        assertEquals(4, q.dequeue());
        assertEquals(5, q.dequeue());
        assertEquals(6, q.dequeue());
        assertTrue(q.isEmpty());
    }

    @Test @DisplayName("CircularQueue: growth beyond initial capacity (boundary)")
    void circularQueueGrows() {
        CircularQueue<Integer> q = new CircularQueue<>(2);
        for (int i = 0; i < 10; i++) q.enqueue(i);
        assertEquals(10, q.size());
        for (int i = 0; i < 10; i++) assertEquals(i, q.dequeue());
    }

    @Test @DisplayName("CircularQueue: dequeue empty throws (invalid)")
    void circularQueueInvalid() {
        assertThrows(NoSuchElementException.class, () -> new CircularQueue<Integer>().dequeue());
    }

    // ---------------- Deque ----------------

    @Test @DisplayName("Deque: add/remove both ends (normal)")
    void dequeNormal() {
        Deque<Integer> d = new Deque<>();
        d.addRear(2); d.addFront(1); d.addRear(3);   // 1,2,3
        assertEquals(1, d.peekFront());
        assertEquals(3, d.peekRear());
        assertEquals(1, d.removeFront());
        assertEquals(3, d.removeRear());
        assertEquals(2, d.removeFront());
        assertTrue(d.isEmpty());
    }

    @Test @DisplayName("Deque: removeFront on empty throws (invalid)")
    void dequeInvalid() {
        assertThrows(NoSuchElementException.class, () -> new Deque<Integer>().removeFront());
    }

    // ---------------- BinaryHeap ----------------

    @Test @DisplayName("BinaryHeap: min-heap extractTop ascending (normal)")
    void heapMin() {
        BinaryHeap<Integer> h = new BinaryHeap<>(Comparator.naturalOrder());
        int[] vals = {5, 3, 8, 1, 9, 2};
        for (int v : vals) h.insert(v);
        assertEquals(1, h.peek());
        int prev = Integer.MIN_VALUE;
        while (!h.isEmpty()) { int x = h.extractTop(); assertTrue(x >= prev); prev = x; }
    }

    @Test @DisplayName("BinaryHeap: bottom-up heapify build + duplicates (boundary)")
    void heapHeapifyDuplicates() {
        Integer[] items = {4, 4, 4, 1, 1, 9};
        BinaryHeap<Integer> h = new BinaryHeap<>(items, Comparator.naturalOrder());
        assertEquals(1, h.extractTop());
        assertEquals(1, h.extractTop());
        assertEquals(4, h.extractTop());
    }

    @Test @DisplayName("BinaryHeap: max-heap via reversed comparator; extract empty throws (invalid)")
    void heapMaxAndInvalid() {
        BinaryHeap<Integer> h = new BinaryHeap<>(Comparator.reverseOrder());
        h.insert(1); h.insert(7); h.insert(3);
        assertEquals(7, h.extractTop());   // max first
        h.extractTop(); h.extractTop();
        assertThrows(NoSuchElementException.class, h::extractTop);
    }

    // ---------------- BinarySearchTree ----------------

    @Test @DisplayName("BST: insert/search/inorder ascending (normal)")
    void bstNormal() {
        BinarySearchTree<Integer, String> t = new BinarySearchTree<>();
        int[] keys = {5, 3, 8, 1, 4};
        for (int k : keys) t.insert(k, "v" + k);
        assertEquals("v4", t.search(4));
        assertNull(t.search(99));
        DynamicArray<Integer> in = t.inorderKeys();
        for (int i = 1; i < in.size(); i++) assertTrue(in.get(i - 1) < in.get(i)); // sorted
    }

    @Test @DisplayName("BST: duplicate key updates value (boundary)")
    void bstDuplicate() {
        BinarySearchTree<Integer, String> t = new BinarySearchTree<>();
        t.insert(1, "a"); t.insert(1, "b");
        assertEquals("b", t.search(1));
        assertEquals(1, t.size());
    }

    @Test @DisplayName("BST: null key insert throws; degenerate height (invalid/boundary)")
    void bstInvalidAndDegenerate() {
        BinarySearchTree<Integer, String> t = new BinarySearchTree<>();
        assertThrows(IllegalArgumentException.class, () -> t.insert(null, "x"));
        for (int i = 1; i <= 8; i++) t.insert(i, "v"); // sorted input -> degenerate list
        assertEquals(7, t.height()); // height == n-1 proves the unbalanced worst case
    }

    // ---------------- AVLTree ----------------

    @Test @DisplayName("AVL: stays balanced on sorted input (normal vs BST)")
    void avlBalanced() {
        AVLTree<Integer, String> t = new AVLTree<>();
        for (int i = 1; i <= 1000; i++) t.insert(i, "v"); // adversarial sorted insert
        // Balanced height must be <= 1.44*log2(n) ~ 15 for n=1000, never 999.
        assertTrue(t.height() <= 15, "AVL height was " + t.height());
        assertEquals("v", t.search(777));
        assertNull(t.search(2000));
    }

    @Test @DisplayName("AVL: single element and duplicate update (boundary)")
    void avlBoundary() {
        AVLTree<String, Integer> t = new AVLTree<>();
        t.insert("only", 1);
        assertEquals(0, t.height()); // single node has height 0
        t.insert("only", 2);
        assertEquals(2, t.search("only"));
        assertEquals(1, t.size());
    }

    @Test @DisplayName("AVL: inorder is sorted after rotations (correctness)")
    void avlInorderSorted() {
        AVLTree<Integer, Integer> t = new AVLTree<>();
        int[] ks = {10, 20, 30, 40, 50, 25}; // forces LL/RR/RL rotations
        for (int k : ks) t.insert(k, k);
        DynamicArray<Integer> in = t.inorderKeys();
        for (int i = 1; i < in.size(); i++) assertTrue(in.get(i - 1) < in.get(i));
    }

    // ---------------- BTree ----------------

    @Test @DisplayName("BTree: insert with node splitting + search (normal)")
    void btreeNormal() {
        BTree<Integer, String> t = new BTree<>(3);
        for (int i = 1; i <= 100; i++) t.insert(i, "v" + i); // forces many splits
        assertEquals("v50", t.search(50));
        assertEquals("v1", t.search(1));
        assertEquals("v100", t.search(100));
        assertNull(t.search(101));
        assertEquals(100, t.size());
    }

    @Test @DisplayName("BTree: inorder sorted + duplicate update (boundary/correctness)")
    void btreeInorderAndDuplicate() {
        BTree<Integer, String> t = new BTree<>(2);
        int[] ks = {9, 3, 7, 1, 5, 8, 2, 6, 4};
        for (int k : ks) t.insert(k, "v");
        t.insert(5, "updated");
        assertEquals("updated", t.search(5));
        assertEquals(9, t.size()); // duplicate did not grow size
        DynamicArray<Integer> in = t.inorderKeys();
        for (int i = 1; i < in.size(); i++) assertTrue(in.get(i - 1) < in.get(i));
    }

    @Test @DisplayName("BTree: degree < 2 rejected (invalid)")
    void btreeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new BTree<Integer, String>(1));
    }

    // ---------------- HashTable / HashMap / HashSet ----------------

    @Test @DisplayName("HashTable: put/get/remove with resize (normal)")
    void hashTableNormal() {
        HashTable<String, Integer> h = new HashTable<>(4, 0.75);
        for (int i = 0; i < 100; i++) h.put("k" + i, i);   // forces several resizes
        assertEquals(50, h.get("k50"));
        assertEquals(100, h.size());
        assertEquals(Integer.valueOf(50), h.remove("k50"));
        assertNull(h.get("k50"));
        assertEquals(99, h.size());
    }

    @Test @DisplayName("HashTable: heavy collisions still retrievable (boundary)")
    void hashTableCollisions() {
        // Tiny fixed table with many keys => long chains, exercising chaining.
        HashTable<Integer, Integer> h = new HashTable<>(2, 1000.0); // won't resize
        for (int i = 0; i < 200; i++) h.put(i, i * 10);
        for (int i = 0; i < 200; i++) assertEquals(Integer.valueOf(i * 10), h.get(i));
        assertTrue(h.bucketCount() <= 2); // proves they collided into few buckets
    }

    @Test @DisplayName("HashTable: get/remove absent key returns null (invalid)")
    void hashTableAbsent() {
        HashTable<String, Integer> h = new HashTable<>();
        assertNull(h.get("nope"));
        assertNull(h.remove("nope"));
        assertFalse(h.containsKey("nope"));
    }

    @Test @DisplayName("HashMap/HashSet wrappers behave (normal + duplicate)")
    void mapSetWrappers() {
        HashMap<String, Integer> m = new HashMap<>();
        m.put("a", 1); m.put("a", 2); // overwrite
        assertEquals(2, m.get("a"));
        assertEquals(1, m.size());

        HashSet<String> s = new HashSet<>();
        assertTrue(s.add("x"));
        assertFalse(s.add("x")); // duplicate rejected
        assertTrue(s.contains("x"));
        assertTrue(s.remove("x"));
        assertFalse(s.contains("x"));
    }

    // ---------------- DisjointSet ----------------

    @Test @DisplayName("DisjointSet: union/find/connected with path compression (normal)")
    void disjointNormal() {
        DisjointSet ds = new DisjointSet(6);
        assertEquals(6, ds.count());
        ds.union(0, 1); ds.union(1, 2); ds.union(3, 4);
        assertTrue(ds.connected(0, 2));
        assertFalse(ds.connected(0, 3));
        assertEquals(3, ds.count()); // {0,1,2} {3,4} {5}
    }

    @Test @DisplayName("DisjointSet: union of already-joined returns false (boundary)")
    void disjointAlreadyJoined() {
        DisjointSet ds = new DisjointSet(3);
        assertTrue(ds.union(0, 1));
        assertFalse(ds.union(1, 0)); // already connected -> no merge (cycle in Kruskal)
    }

    @Test @DisplayName("DisjointSet: out-of-range element throws (invalid)")
    void disjointInvalid() {
        DisjointSet ds = new DisjointSet(3);
        assertThrows(IndexOutOfBoundsException.class, () -> ds.find(5));
    }

    // ---------------- Graph ----------------

    @Test @DisplayName("Graph: adjacency list AND matrix stay in sync (normal)")
    void graphBothViews() {
        Graph g = new Graph();
        g.addEdge("A", "B", 5, true);
        g.addEdge("B", "C", 2, true);
        int a = g.indexOf("A"), b = g.indexOf("B"), c = g.indexOf("C");
        assertEquals(3, g.vertexCount());
        assertEquals(5.0, g.weight(a, b));         // matrix view
        assertEquals(5.0, g.weight(b, a));         // undirected -> symmetric
        assertEquals(Graph.INF, g.weight(a, c));   // no direct A-C edge
        // list view: B has neighbours A and C
        int seen = 0;
        for (Graph.Edge e : g.neighbors(b)) seen++;
        assertEquals(2, seen);
    }

    @Test @DisplayName("Graph: adding a vertex twice is idempotent (boundary)")
    void graphDuplicateVertex() {
        Graph g = new Graph();
        int first = g.addVertex("X");
        int again = g.addVertex("X");
        assertEquals(first, again);
        assertEquals(1, g.vertexCount());
    }

    @Test @DisplayName("Graph: unknown id lookup returns -1 (invalid)")
    void graphUnknownId() {
        Graph g = new Graph();
        g.addVertex("A");
        assertEquals(-1, g.indexOf("ZZZ"));
    }
}
