# 5. Data-Structure Implementation

All 13 structures are hand-written in the `datastructures` package. None uses
`java.util.HashMap`, `TreeMap`, `PriorityQueue`, `Stack`, `ArrayDeque` or any
built-in equivalent — only plain arrays and (inside a couple of them) `java.util.List`/`Iterator`
as backing storage, as the brief permits. Each subsection says what it is, how it
works, its costs, and where the system uses it.

## 5.1 DynamicArray — `DynamicArray.java`
A resizable array. It keeps a plain `Object[]` block and a size; when full it
doubles, when a quarter full it halves. Operations: `add` (amortised O(1)),
`insert`/`remove` at an index (O(n) for the shift), `get`/`set` (O(1)).
**Used as** the workhorse backing store everywhere: the in-memory entity tables,
the graph's adjacency lists, and every result list the algorithms return.

## 5.2 DoublyLinkedList — `DoublyLinkedList.java`
Nodes with `prev`/`next` pointers and head/tail. `addFirst`/`addLast`/`insertAfter`
splice in O(1) once the node is found; `remove` unlinks in O(1). Iterable for
for-each. **Used as** the bucket chains inside the hash table (separate chaining),
and anywhere cheap end-insertion is wanted.

## 5.3 ArrayStack — `ArrayStack.java`
A LIFO stack on top of `DynamicArray`. `push`/`pop`/`peek` are O(1) at the array's
end; `isEmpty`. **Used by** the iterative Depth-First Search, so deep hospital
graphs cannot overflow the JVM call stack, and to reverse a Dijkstra path.

## 5.4 CircularQueue — `CircularQueue.java`
A ring buffer with `head`/`tail` indices that wrap using modular arithmetic;
it grows and re-linearises when full. `enqueue`/`dequeue` are O(1) with no element
shifting. **Used by** Breadth-First Search as its frontier queue.

## 5.5 LinkedQueue — `LinkedQueue.java`
The linked-list realisation of a FIFO queue (companion to the ring buffer), built
on `DoublyLinkedList`. `enqueue`/`dequeue`/`peek` O(1). **Used for** general
arrival-order buffering of requests before they are prioritised into the heap.

## 5.6 Deque — `Deque.java`
A double-ended queue as a wrap-around array: `addFront`/`addRear`/`removeFront`/
`removeRear` all O(1). **Used where** work may be pushed back to the front as well
as the back; it also demonstrates that one ring generalises both a stack and a
queue.

## 5.7 BinaryHeap (priority queue) — `BinaryHeap.java`
A complete binary tree embedded in an array (parent `i`, children `2i+1`/`2i+2`).
`insert` sifts up, `extractTop` sifts down (both O(log n)); a bottom-up
constructor **heapifies** an array in O(n). Ordered by a supplied comparator, so a
natural comparator gives a min-heap (extractMin) and a reversed one a max-heap
(extractMax). **Used as** the request dispatcher (min-heap on urgency) and inside
Dijkstra and Prim.

## 5.8 BinarySearchTree — `BinarySearchTree.java`
An ordered map of comparable keys → values. `insert`/`search` follow the
left-smaller/right-larger rule (average O(log n)); `inorderKeys` returns keys in
ascending order. It is deliberately **unbalanced** so the study can show it
degenerating to height n−1 on sorted input. **Used for** ordered/alphabetical
look-up of locations by name.

## 5.9 AVLTree — `AVLTree.java`
A self-balancing BST. After each insert it updates node heights and applies the
four rotations (LL, RR, LR, RL) wherever a subtree's balance factor leaves
{−1,0,+1}, guaranteeing height ≈ 1.44·log₂n and O(log n) search. **Used as** the
balanced counterpart to the plain BST — the benchmark inserts the same sorted keys
into both and measures the height/search-time gap.

## 5.10 BTree — `BTree.java`
A CLRS-style B-tree of minimum degree *t*: every node holds *t−1…2t−1* keys and the
tree stays perfectly height-balanced. `insert` splits a full child *before*
descending (proactive splitting), so only the root ever raises the height;
`search` is O(logₜ n). **Used as** a disk-style ordered index of records by id, and
it ties the project to the SQLite layer, whose own indexes are B-trees.

## 5.11 HashTable — `HashTable.java`
A hash table with **separate chaining**: an array of buckets, each a
`DoublyLinkedList` of `(key,value)` entries. A spread function scrambles poor
hash codes; the table resizes and rehashes when the load factor passes 0.75.
`put`/`get`/`remove` are average O(1). **Used as** the primary id→entity indexes
(locations, requests, resources) and the id→vertex-index map inside the graph.

## 5.12 HashMap & HashSet — `HashMap.java`, `HashSet.java`
Thin, clearly-named wrappers **built on our own `HashTable`** (not `java.util`).
`HashMap` gives map semantics (`put`/`get`/`remove`/`keys`); `HashSet` stores
membership as keys against a sentinel (`add`/`contains`/`remove`). **Used by** the
graph's id↔index map (HashMap) and the "visited"/"seen" sets in BFS/DFS and while
de-duplicating ids (HashSet).

## 5.13 DisjointSet (union-find) — `DisjointSet.java`
Integer union-find with **path compression** in `find` and **union by rank**,
giving near-constant O(α(n)) amortised operations. **Used by** Kruskal's MST: it
answers "are these two locations already connected?" so an edge is added only when
it joins two different components (no cycle).

## 5.14 Graph — `Graph.java`
A weighted graph keeping **both** representations in sync: an *adjacency list*
(`DynamicArray` of edges per vertex, space O(V+E), ideal for the sparse corridor
network and traversals) and an *adjacency matrix* (V×V weights, O(1) "is there a
direct corridor A→B?"). External string ids map to dense integer indices via our
`HashMap`. Undirected corridors are stored as two directed edges. **Used as** the
whole road network — the `Repository` builds one time-weighted and one
distance-weighted instance from the same routes.

---

*Correctness evidence (trace tables, invariants, rotation/split walk-throughs) for
the trees, heap and union-find appears in section 7; measured costs in section 8.*
