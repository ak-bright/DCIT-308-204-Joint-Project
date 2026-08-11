# 6. Algorithm Implementation

All algorithms are hand-written in the `algorithms` package. For each one below:
a short plain-language description, pseudocode, a key Java snippet, and the Big-O.

## 6.1 Searching — `SearchAlgorithms.java`

**Linear search** — scan left to right. **Binary search** — repeatedly halve a
sorted interval.

```
binarySearch(a, target):
    lo ← 0; hi ← n-1
    while lo ≤ hi:
        mid ← lo + (hi-lo)/2
        if a[mid] = target: return mid
        else if a[mid] < target: lo ← mid+1
        else: hi ← mid-1
    return -1
```
```java
int mid = lo + (hi - lo) / 2;      // avoids integer overflow
int c = cmp.compare(a[mid], target);
if (c == 0) return mid;
else if (c < 0) lo = mid + 1; else hi = mid - 1;
```
**Big-O:** linear O(n); binary best O(1), average/worst O(log n). Binary requires
sorted input (see the invalid case in section 7).

## 6.2 Sorting — `SortAlgorithms.java`

| Algorithm | Idea | Best | Average | Worst | Stable |
|---|---|---|---|---|---|
| Selection | pick min of the suffix, swap into place | O(n²) | O(n²) | O(n²) | no |
| Insertion | grow a sorted prefix | O(n) | O(n²) | O(n²) | yes |
| Merge | divide, sort halves, merge | O(n log n) | O(n log n) | O(n log n) | yes |
| Quick | median-of-3 pivot, partition | O(n log n) | O(n log n) | O(n²) | no |

```
mergeSort(a, lo, hi):
    if lo ≥ hi: return
    mid ← (lo+hi)/2
    mergeSort(a, lo, mid); mergeSort(a, mid+1, hi)
    merge(a, lo, mid, hi)          # emit the smaller front element each step
```
```java
// merge step: pick the smaller of the two run fronts (ties take left => stable)
if (i > mid) a[k] = aux[j++];
else if (j > hi) a[k] = aux[i++];
else if (cmp.compare(aux[j], aux[i]) < 0) a[k] = aux[j++];
else a[k] = aux[i++];
```
Quicksort uses **median-of-three** pivoting and an **insertion-sort cutoff** for
small ranges, and recurses into the smaller side (looping on the larger) to bound
recursion depth to O(log n).

## 6.3 Graph traversal — BFS & DFS — `GraphAlgorithms.java`

BFS explores in rings using our `CircularQueue`; DFS dives deep using our
`ArrayStack` (iterative, not recursive). Both mark visited once.
```
BFS(g, s):
    visited[s] ← true; queue.enqueue(s)
    while queue not empty:
        u ← queue.dequeue(); output u
        for each edge (u→w): if not visited[w]: visited[w] ← true; queue.enqueue(w)
```
**Big-O:** O(V + E) time, O(V) space. The set BFS/DFS outputs *is* the set of
locations reachable from the dispatch point.

## 6.4 Dijkstra's shortest path — `GraphAlgorithms.dijkstra`

Greedily settle the closest unsettled vertex, relaxing its edges, using our
`BinaryHeap` as the priority queue (lazy deletion of stale entries).
```
dijkstra(g, s):
    dist[*] ← ∞; dist[s] ← 0; pq.insert((s,0))
    while pq not empty:
        (u,d) ← pq.extractMin(); if settled[u]: continue; settled[u] ← true
        for each edge (u→w, weight): if dist[u]+weight < dist[w]:
            dist[w] ← dist[u]+weight; prev[w] ← u; pq.insert((w, dist[w]))
```
**Big-O:** O((V + E) log V). Requires non-negative weights (travel times). Path is
rebuilt by following `prev[]` back from the target (empty if unreachable).

## 6.5 Minimum spanning tree — Prim & Kruskal

**Prim** grows a tree from vertex 0, always adding the cheapest edge that leaves
the current tree (min-heap of crossing edges) — O(E log V).
**Kruskal** sorts all edges and adds each one whose endpoints are in different
components, tested with our `DisjointSet` — O(E log E).
```java
// Kruskal core: union returns false if the two ends are already connected (cycle)
for (Graph.Edge e : sortedEdges)
    if (dsu.union(e.from, e.to)) { mst.add(e); if (mst.size()==V-1) break; }
```
Both compute the same minimum total weight (verified by a unit test); on a
disconnected graph they yield a spanning *forest*.

## 6.6 Greedy staff assignment — `GreedyAssignment.java`

Handle requests **most-urgent-first** (min-heap on urgency); give each the
available resource with the most remaining capacity that still fits it (best-fit),
subtracting the service time.
```
for each request in order of increasing urgency:
    pick AVAILABLE resource with max remaining capacity ≥ request.serviceMinutes
    if found: assign, capacity -= serviceMinutes   else: leave unassigned
```
**Big-O:** O(R log R + R·K) for R requests, K resources. Section 7 gives a worked
counterexample proving this greedy is **not** optimal for throughput.

## 6.7 Dynamic-programming request selection — `DynamicSelection.java`

Choose the subset of requests of maximum total value within a staff-minute budget
— the **0/1 knapsack**, solved by bottom-up tabulation.
```
dp[0][c] = 0
dp[i][c] = dp[i-1][c]                               if w_i > c
         = max(dp[i-1][c], v_i + dp[i-1][c - w_i])  otherwise
```
```java
dp[i][c] = dp[i - 1][c];                 // skip request i
if (w <= c) dp[i][c] = Math.max(dp[i][c], v + dp[i - 1][c - w]); // take it
```
**Big-O:** O(n·C) time and space (the full table is kept so the chosen set can be
reconstructed). A worked table appears in section 7; because the DP considers
*combinations* it beats the greedy dispatcher on the counterexample.
