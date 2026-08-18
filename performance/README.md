# performance/

Empirical performance evidence for the data structures and algorithms.

- `experiment-results/` — raw timing CSVs, one per family (searching, sorting,
  trees, heap, hashtable, graph), produced by `performance.BenchmarkRunner`.
- `graphs/`             — SVG charts drawn from those CSVs by `performance.SvgChart`.
- `interpretation.md`   — a short write-up comparing the theoretical Big-O of each
  structure/algorithm against the measured timings.
- `plot.py`             — an optional matplotlib script for anyone who prefers PNGs;
  it is not required (the SVG charts are generated straight from Java).

Regenerate everything with `bash build.sh bench` (or `.\build.ps1 bench`).
