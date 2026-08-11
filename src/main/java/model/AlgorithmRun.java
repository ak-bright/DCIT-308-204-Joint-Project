package model;

/**
 * An audit record of one execution of an algorithm through the UI or benchmark,
 * persisted to the {@code algorithm_runs} table (see database/schema.sql).
 *
 * <p>Recording the inputs, wall-clock timing, and a short result summary gives
 * the report a real, reproducible performance trail rather than invented numbers.</p>
 */
public class AlgorithmRun {
    private long id;               // assigned by the database (autoincrement)
    private final String algorithm;      // e.g. "Dijkstra", "Kruskal", "GreedyAssign"
    private final String inputSummary;   // e.g. "from=L001 to=L042, |V|=50 |E|=120"
    private final long   elapsedNanos;   // measured wall-clock time
    private final String resultSummary;  // e.g. "path length 7, cost 843s"
    private final String runAt;          // ISO timestamp string

    public AlgorithmRun(String algorithm, String inputSummary, long elapsedNanos,
                        String resultSummary, String runAt) {
        this.algorithm = algorithm;
        this.inputSummary = inputSummary;
        this.elapsedNanos = elapsedNanos;
        this.resultSummary = resultSummary;
        this.runAt = runAt;
    }

    public long   getId()            { return id; }
    public void   setId(long id)     { this.id = id; }
    public String getAlgorithm()     { return algorithm; }
    public String getInputSummary()  { return inputSummary; }
    public long   getElapsedNanos()  { return elapsedNanos; }
    public String getResultSummary() { return resultSummary; }
    public String getRunAt()         { return runAt; }

    public double getElapsedMillis() { return elapsedNanos / 1_000_000.0; }

    @Override
    public String toString() {
        return String.format("%s | %s | %.3f ms | %s", algorithm, inputSummary, getElapsedMillis(), resultSummary);
    }
}
