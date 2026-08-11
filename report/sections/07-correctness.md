# 7. Correctness Evidence

This section gives the trace tables, loop invariants, proof sketches and
counterexamples required by the brief. Every claim here is also backed by an
automated test in `src/test/java/tests/` (60 tests, all passing).

## 7.1 Binary search — loop invariant + trace

**Invariant:** at the top of each loop, *if* `target` is in the array then its
index lies within `[lo, hi]`. Each step discards a half that cannot contain the
target, so the invariant is preserved and the interval strictly shrinks →
termination with a hit or `lo > hi`.

**Trace** — array `[1,3,5,7,9,11]`, target `9`:

| step | lo | hi | mid | a[mid] | action |
|---|---|---|---|---|---|
| 1 | 0 | 5 | 2 | 5 | 5 < 9 → lo = 3 |
| 2 | 3 | 5 | 4 | 9 | hit → return 4 |

## 7.2 Insertion sort — loop invariant + trace

**Invariant:** before processing index `i`, the prefix `a[0..i-1]` is a sorted
permutation of the original prefix. When the loop ends (`i = n`) the whole array
is sorted.

**Trace** — `[5, 2, 4, 1]`:

| i | key | array after inserting key |
|---|---|---|
| 1 | 2 | `[2, 5, 4, 1]` |
| 2 | 4 | `[2, 4, 5, 1]` |
| 3 | 1 | `[1, 2, 4, 5]` |

## 7.3 Merge sort — induction

**Base:** a range of length ≤ 1 is sorted. **Step:** assume both halves are sorted;
the linear merge always emits the smaller of the two front elements, so the output
is sorted and contains exactly the two halves' elements. By induction the whole
array is sorted. Merge is **stable** because ties take the left element first.

## 7.4 Quicksort — partition invariant

**Invariant (after partition around pivot p at final index k):** every element in
`a[lo..k-1] ≤ p ≤ a[k+1..hi]`, and `p` is at its final sorted position `k`.
Recursively sorting the two sides therefore sorts the whole. Median-of-three
pivoting makes the worst-case O(n²) input very unlikely, and recursing into the
smaller side keeps stack depth O(log n). *(Test `sortsAgree` confirms all four
sorts produce identical output; `sortsAdversarial` checks sorted/reverse input.)*

## 7.5 Dijkstra — greedy exchange argument + trace

**Why it is correct:** when a vertex `u` is first extracted from the min-heap, its
recorded distance is final. Suppose not — then some shorter path to `u` exists via
an unsettled vertex `x`; but `x` would have distance ≤ `dist[u]` and would have
been extracted first (all weights ≥ 0), a contradiction. Non-negative weights are
essential.

**Trace** — edges A–B 4, A–C 1, C–B 2, B–D 1; source A:

| settle | dist updates | frontier |
|---|---|---|
| A(0) | B←4, C←1 | {C:1, B:4} |
| C(1) | B←min(4, 1+2)=3 | {B:3, B:4(stale)} |
| B(3) | D←4 | {D:4, B:4(stale)} |
| D(4) | — | done |

Result: dist(D)=4, path A→C→B→D. *(Test `dijkstraNormal`; `dijkstraUnreachable`
checks an isolated vertex gets ∞ and an empty path.)*

## 7.6 Kruskal / Prim — cut & cycle properties

**Prim (cut property):** the cheapest edge crossing the cut between the growing
tree and the rest is always safe (belongs to some MST); adding such edges V−1
times yields an MST. **Kruskal (cycle property):** taking edges cheapest-first and
skipping any that would close a cycle (detected when union-find finds both ends in
one component) keeps only safe edges → an MST (a forest if disconnected).

**Trace (Kruskal)** — edges sorted: A-B 1, B-C 2, A-C 3, C-D 4:

| edge | ends already connected? | action | tree weight |
|---|---|---|---|
| A-B (1) | no | add | 1 |
| B-C (2) | no | add | 3 |
| A-C (3) | **yes** (A,C in {A,B,C}) | skip (cycle) | 3 |
| C-D (4) | no | add | 7 |

MST weight = 7. *(Test `mstAgrees` confirms Prim and Kruskal both give 7;
`kruskalForest` checks a disconnected graph gives a forest of V−component edges.)*

## 7.7 Dynamic programming — the knapsack table

Capacity 10; requests A(w10,v10), B(w5,v8), C(w5,v8):

| after \\ capacity c → | 0 | 5 | 10 |
|---|---|---|---|
| i=0 (none) | 0 | 0 | 0 |
| i=1 (A) | 0 | 0 | 10 |
| i=2 (B) | 0 | 8 | 10 |
| i=3 (C) | 0 | 8 | **16** |

`dp[3][10] = max(dp[2][10]=10, v_C + dp[2][5] = 8+8 = 16) = 16`. Back-tracking the
table selects **{B, C}**. *(Test `dpWorkedExample` asserts value 16, weight 10, two
items; `dpZeroCapacity` and `dpNegativeCapacity` cover the boundary/invalid cases.)*

## 7.8 Counterexample 1 — greedy is NOT throughput-optimal

Same numbers, objective = *complete as many requests as possible* with a single
team of capacity 10:

- **Greedy (urgency-first):** takes A (urgency 1, needs all 10 minutes) → B and C
  can't fit → **1 request completed**.
- **Optimal / DP:** takes B and C (5 + 5 = 10) → **2 requests completed**, value 16
  vs greedy's 10.

There is no exchange that lets urgency-first recover the second completion once A
is chosen, so the greedy choice is genuinely sub-optimal here. *(Test
`greedyCounterexample` asserts greedy completes 1 while the DP selects 2.)* Section
10 explains why we still, deliberately, use urgency-first for live triage.

## 7.9 Counterexample 2 — binary search on unsorted input (broken precondition)

Binary search's correctness depends on the array being sorted. On the **unsorted**
array `[5,1,3,2,4]`, searching for `1` (which is present at index 1):

| step | lo | hi | mid | a[mid] | action |
|---|---|---|---|---|---|
| 1 | 0 | 4 | 2 | 3 | 3 > 1 → hi = 1 |
| 2 | 0 | 1 | 0 | 5 | 5 > 1 → hi = −1 |

Loop ends → returns **−1 (not found)** even though `1` is present — linear search
finds it at index 1. This shows the precondition is not optional. *(Test
`binarySearchUnsorted` asserts linear finds it but binary misses it.)*
