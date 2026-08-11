#!/usr/bin/env python3
"""
OPTIONAL matplotlib plotter — an alternative to the SVG charts the Java
benchmark already generates in performance/graphs/.

The project does NOT need this script: BenchmarkRunner writes ready-to-view
.svg charts with no dependencies. This file is provided only for the team in
case they would rather regenerate the graphs as .png with matplotlib for the
Word report.

Usage (needs Python 3 + matplotlib installed):
    pip install matplotlib
    python performance/plot.py

It reads every CSV in performance/experiment-results/ and writes a matching
.png into performance/graphs/.
"""
import csv
import os

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
except ImportError:
    raise SystemExit("matplotlib not installed. Run: pip install matplotlib "
                     "(or just use the .svg charts already in performance/graphs/)")

HERE = os.path.dirname(os.path.abspath(__file__))
RESULTS = os.path.join(HERE, "experiment-results")
GRAPHS = os.path.join(HERE, "graphs")
os.makedirs(GRAPHS, exist_ok=True)


def load(path):
    with open(path, newline="") as f:
        rows = list(csv.reader(f))
    header, data = rows[0], rows[1:]
    cols = list(zip(*[[float(v) for v in r] for r in data]))
    return header, cols


def plot_file(name, title, xlabel, ylabel):
    path = os.path.join(RESULTS, name)
    if not os.path.exists(path):
        return
    header, cols = load(path)
    x = cols[0]
    plt.figure(figsize=(8, 5))
    for i in range(1, len(cols)):
        plt.plot(x, cols[i], marker="o", label=header[i])
    plt.title(title)
    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.legend()
    plt.grid(True, alpha=0.3)
    out = os.path.join(GRAPHS, name.replace(".csv", ".png"))
    plt.savefig(out, dpi=120, bbox_inches="tight")
    plt.close()
    print("wrote", out)


plot_file("sorting.csv",   "Sorting: time vs input size",              "n (elements)", "time (ms)")
plot_file("searching.csv", "Searching: ns per query vs input size",    "n (elements)", "ns per query")
plot_file("hashtable.csv", "Hash table: ns per op vs load factor",     "load factor",  "ns per op")
plot_file("trees.csv",     "BST vs AVL (height & search)",             "n (keys)",     "value")
plot_file("heap.csv",      "Binary heap: build & dispatch",            "n (elements)", "time (ms)")
plot_file("graph.csv",     "Graph algorithms: time vs |V|",            "|V|",          "time (ms)")
print("Done.")
