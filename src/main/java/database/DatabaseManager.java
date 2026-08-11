package database;

import datastructures.DynamicArray;
import model.AlgorithmRun;
import model.AuditEvent;
import model.Location;
import model.Resource;
import model.Route;
import model.ServiceRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * SQLite persistence layer (JDBC) for the whole system. Wraps a single-file
 * database at {@code database/hospital.db}, creates the schema from
 * {@code database/schema.sql} on first use, and provides save/load of the four
 * entity tables plus the two append-only history tables
 * ({@code algorithm_runs}, {@code audit_events}).
 *
 * <p>SQLite is chosen exactly as the brief allows: one file, no server, zero
 * setup. The only third-party jar involved is the JDBC driver — none of the
 * assessed data structures or algorithms use it.</p>
 *
 * <p>The class is {@link AutoCloseable} so callers can use try-with-resources.</p>
 */
public final class DatabaseManager implements AutoCloseable {

    private final Connection conn;

    /** Open (creating if needed) the database file at {@code dbPath}. */
    public DatabaseManager(String dbPath) {
        try {
            // The org.xerial driver auto-registers, but load it explicitly for clarity.
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            try (Statement st = conn.createStatement()) { st.execute("PRAGMA foreign_keys = ON"); }
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Could not open SQLite database at " + dbPath, e);
        }
    }

    /** Create all tables by executing database/schema.sql (idempotent). */
    public void initSchema(String schemaSqlPath) {
        try {
            String raw = Files.readString(Path.of(schemaSqlPath));
            // Strip whole-line "--" comments FIRST so that a statement preceded by
            // a comment block is not accidentally dropped when we split on ';'.
            StringBuilder cleaned = new StringBuilder();
            for (String line : raw.split("\r?\n")) {
                if (line.trim().startsWith("--")) continue; // drop comment-only lines
                cleaned.append(line).append('\n');
            }
            try (Statement st = conn.createStatement()) {
                for (String stmt : cleaned.toString().split(";")) {
                    String s = stmt.trim();
                    if (!s.isEmpty()) st.execute(s); // run each real statement
                }
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Schema initialisation failed", e);
        }
    }

    // ---------------------- save (in-memory -> DB) ----------------------

    /**
     * Persist the entire in-memory {@link Repository} to the database inside one
     * transaction: existing rows in the four entity tables are cleared and
     * re-inserted, so the DB becomes an exact mirror of memory.
     */
    public void saveAll(Repository repo) {
        try {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.execute("DELETE FROM roads");
                st.execute("DELETE FROM service_requests");
                st.execute("DELETE FROM resources");
                st.execute("DELETE FROM locations");
            }
            insertLocations(repo.locations());
            insertRoads(repo.routes());
            insertRequests(repo.requests());
            insertResources(repo.resources());
            conn.commit();
            logAudit(new AuditEvent("SAVE", "Database",
                    "Saved " + repo.locations().size() + " locations, " + repo.routes().size() +
                    " roads, " + repo.requests().size() + " requests, " + repo.resources().size() +
                    " resources", now()));
        } catch (SQLException e) {
            rollbackQuietly();
            throw new RuntimeException("saveAll failed", e);
        } finally {
            setAutoCommitQuietly(true);
        }
    }

    private void insertLocations(DynamicArray<Location> locs) throws SQLException {
        String sql = "INSERT OR REPLACE INTO locations(location_id,name,area,type,notes) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Location l : locs) {
                ps.setString(1, l.getLocationId()); ps.setString(2, l.getName());
                ps.setString(3, l.getArea());       ps.setString(4, l.getType());
                ps.setString(5, l.getNotes());      ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertRoads(DynamicArray<Route> routes) throws SQLException {
        String sql = "INSERT INTO roads(from_id,to_id,distance_m,travel_secs,notes) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Route r : routes) {
                ps.setString(1, r.getFromLocationId()); ps.setString(2, r.getToLocationId());
                ps.setDouble(3, r.getDistance());       ps.setDouble(4, r.getTravelTime());
                ps.setString(5, r.getNotes());          ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertRequests(DynamicArray<ServiceRequest> reqs) throws SQLException {
        String sql = "INSERT OR REPLACE INTO service_requests" +
                "(request_id,source,destination,category,urgency,time_submitted,deadline,status,service_minutes,value_score)" +
                " VALUES(?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ServiceRequest s : reqs) {
                ps.setString(1, s.getRequestId());   ps.setString(2, s.getSource());
                ps.setString(3, s.getDestination()); ps.setString(4, s.getCategory());
                ps.setInt(5, s.getUrgency());        ps.setString(6, s.getTimeSubmitted());
                ps.setString(7, s.getDeadline());    ps.setString(8, s.getStatus());
                ps.setInt(9, s.getServiceMinutes()); ps.setInt(10, s.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertResources(DynamicArray<Resource> res) throws SQLException {
        String sql = "INSERT OR REPLACE INTO resources(resource_id,type,home_location,capacity,availability_status) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Resource r : res) {
                ps.setString(1, r.getResourceId()); ps.setString(2, r.getType());
                ps.setString(3, r.getHomeLocation());ps.setInt(4, r.getCapacity());
                ps.setString(5, r.getAvailabilityStatus()); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ---------------------- load (DB -> in-memory) ----------------------

    /** Load the four entity tables from the DB into a fresh {@link Repository}. */
    public Repository loadAll() {
        Repository repo = new Repository();
        DynamicArray<Location> locs = new DynamicArray<>();
        DynamicArray<Route> routes = new DynamicArray<>();
        DynamicArray<ServiceRequest> reqs = new DynamicArray<>();
        DynamicArray<Resource> res = new DynamicArray<>();
        try (Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT location_id,name,area,type,notes FROM locations")) {
                while (rs.next()) locs.add(new Location(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
            try (ResultSet rs = st.executeQuery("SELECT from_id,to_id,distance_m,travel_secs,notes FROM roads")) {
                while (rs.next()) routes.add(new Route(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getDouble(4), rs.getString(5)));
            }
            try (ResultSet rs = st.executeQuery("SELECT request_id,source,destination,category,urgency,time_submitted,deadline,status FROM service_requests")) {
                while (rs.next()) reqs.add(new ServiceRequest(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getString(6), rs.getString(7), rs.getString(8)));
            }
            try (ResultSet rs = st.executeQuery("SELECT resource_id,type,home_location,capacity,availability_status FROM resources")) {
                while (rs.next()) res.add(new Resource(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5)));
            }
        } catch (SQLException e) {
            throw new RuntimeException("loadAll failed", e);
        }
        repo.replaceAll(locs, routes, reqs, res);
        logAudit(new AuditEvent("LOAD", "Database", "Loaded " + locs.size() + " locations from DB", now()));
        return repo;
    }

    public int countRows(String table) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return -1;
        }
    }

    // ---------------------- history logging ----------------------

    /** Append one timing record to {@code algorithm_runs}. */
    public void logAlgorithmRun(AlgorithmRun run) {
        String sql = "INSERT INTO algorithm_runs(algorithm,input_summary,elapsed_nanos,result_summary,run_at) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, run.getAlgorithm());   ps.setString(2, run.getInputSummary());
            ps.setLong(3, run.getElapsedNanos());  ps.setString(4, run.getResultSummary());
            ps.setString(5, run.getRunAt());       ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("logAlgorithmRun failed", e);
        }
    }

    /** Append one change record to {@code audit_events}. */
    public void logAudit(AuditEvent ev) {
        String sql = "INSERT INTO audit_events(event_type,entity,detail,occurred_at) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ev.getEventType()); ps.setString(2, ev.getEntity());
            ps.setString(3, ev.getDetail());    ps.setString(4, ev.getOccurredAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("logAudit failed", e);
        }
    }

    /** Most recent algorithm runs (for the UI "view run log" option). */
    public DynamicArray<AlgorithmRun> recentRuns(int limit) {
        DynamicArray<AlgorithmRun> out = new DynamicArray<>();
        String sql = "SELECT algorithm,input_summary,elapsed_nanos,result_summary,run_at FROM algorithm_runs ORDER BY run_id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(new AlgorithmRun(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getString(5)));
            }
        } catch (SQLException e) {
            throw new RuntimeException("recentRuns failed", e);
        }
        return out;
    }

    public static String now() { return LocalDateTime.now().toString(); }

    @Override public void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); } catch (SQLException ignored) { }
    }

    private void rollbackQuietly()       { try { conn.rollback(); } catch (SQLException ignored) {} }
    private void setAutoCommitQuietly(boolean v) { try { conn.setAutoCommit(v); } catch (SQLException ignored) {} }
}
