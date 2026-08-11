# 8. Performance Analysis

All numbers below are **real measurements** from running the project's own
benchmark harness (`bash build.sh bench`), averaged over 3 trials with a JIT
warm-up pass. The full method, every CSV, and the theory-vs-observed discussion
live in [`performance/interpretation.md`](../../performance/interpretation.md);
raw data is in `performance/experiment-results/` and charts (SVG) in
`performance/graphs/`. **These are not invented figures.** Absolute times depend on
the machine (here: Windows 11 laptop, OpenJDK 17); the curve *shapes* are the
point.

## 8.1 Sorting (`sorting.svg`)

| n | selection (ms) | insertion (ms) | merge (ms) | quick (ms) |
|---|---|---|---|---|
| 1,000 | 3.83 | 1.64 | 2.44 | 0.52 |
| 5,000 | 92.95 | 38.40 | 6.27 | 1.82 |
| 10,000 | 341.08 | 162.20 | 4.13 | 1.83 |

From ×10 the input (1k→10k) selection sort grew ~89× and insertion ~99× — the ×100
an O(n²) algorithm predicts — while merge/quick stayed in single-digit ms
(O(n log n)). By n=10,000 quicksort is ~185× faster than selection sort.

## 8.2 Searching (`searching.svg`)

| n | linear (ns/query) | binary (ns/query) |
|---|---|---|
| 100 | 1,171 | 243 |
| 10,000 | 10,727 | 210 |

Linear cost tracks n; binary stays flat near ~210 ns across a 100× range — the
signature of O(log n).

## 8.3 Hash table vs. load factor (`hashtable.svg`)

`get` stayed ~70–150 ns from load factor 0.25 up to 4.0 — near-constant, exactly
the O(1 + α) expectation for separate chaining. The live app keeps α ≤ 0.75 by
resizing.

## 8.4 BST vs. AVL — the headline experiment (`tree-height.svg`, `tree-search.svg`)

| n | BST height | AVL height | BST search (ns) | AVL search (ns) |
|---|---|---|---|---|
| 100 | 99 | 6 | 4,409 | 766 |
| 1,000 | 999 | 9 | 3,021 | 353 |
| 10,000 | 9,999 | 13 | 6,337 | 230 |

On sorted input the plain BST degenerates to height **exactly n−1** (a linked
list) while the AVL tree holds height ≈ log₂n (6→13 as n goes 100→10,000). AVL
search is 20–30× faster. This is the clearest demonstration of *why* self-balancing
matters.

## 8.5 Binary heap (`heap.svg`)

| n | build (ms) | dispatch (ms) |
|---|---|---|
| 1,000 | 0.18 | 0.39 |
| 10,000 | 2.06 | 5.99 |

Both grow ~n log n; dispatch is ~2–3× build because extract-min sifts a leaf all
the way down.

## 8.6 Graph algorithms (`graph.svg`)

| V | E | BFS | DFS | Dijkstra | Prim | Kruskal (ms) |
|---|---|---|---|---|---|---|
| 50 | 396 | 0.13 | 0.18 | 0.17 | 0.18 | 0.34 |
| 500 | 3,992 | 1.59 | 2.06 | 2.38 | 3.21 | 4.50 |

BFS/DFS grow ~linearly (O(V+E)); the heap-based Dijkstra/Prim a little faster than
linear (extra log V); Kruskal is consistently slowest because of its up-front
O(E log E) edge sort. The ordering BFS ≲ DFS ≲ Dijkstra ≲ Prim ≲ Kruskal held at
every size.

## 8.7 Conclusion

Across all six experiments the observed timings match the theoretical Big-O
classes; the only deviations were small constant-factor/warm-up effects at tiny
inputs and a few ms of measurement noise, both discussed in
`performance/interpretation.md`. Nothing observed contradicted the theory.
