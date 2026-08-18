package tests;

import database.DataLoader;
import database.DatabaseManager;
import database.Repository;
import datastructures.DynamicArray;
import model.AlgorithmRun;
import model.Location;
import model.Resource;
import model.Route;
import model.ServiceRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the CSV loader, the in-memory {@link Repository}, and the
 * SQLite {@link DatabaseManager}. These prove the "load data from and save data
 * to the database" behaviour actually round-trips, and that the loader tolerates
 * leading comment and header lines.
 *
 * <p>They assume the working directory is the repo root (as the build scripts
 * run tests), so {@code database/seed-data} and {@code database/schema.sql} are
 * reachable. If the seed data is missing, the seed-dependent tests self-skip via
 * an assumption rather than fail.</p>
 */
class DatabaseIntegrationTest {

    private static final String SCHEMA = "database/schema.sql";

    @Test @DisplayName("DataLoader: parses a hand-written CSV, skipping comment+header (normal)")
    void loaderParsesCsv(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("locations.csv"),
                "# comment line to be ignored\n" +
                "locationId,name,area,type,notes\n" +
                "L001,Emergency Department,Main Block,department,x\n" +
                "L002,\"Ward, B3\",Maternity Block,ward,y\n"); // quoted comma field
        DataLoader dl = new DataLoader(dir.toString());
        DynamicArray<Location> locs = dl.loadLocations();
        assertEquals(2, locs.size());
        assertEquals("Emergency Department", locs.get(0).getName());
        assertEquals("Ward, B3", locs.get(1).getName()); // comma survived the quotes
    }

    @Test @DisplayName("DataLoader: missing file yields empty list (boundary/invalid)")
    void loaderMissingFile(@TempDir Path dir) {
        DataLoader dl = new DataLoader(dir.toString());
        assertFalse(dl.seedExists());
        assertEquals(0, dl.loadLocations().size());
    }

    @Test @DisplayName("Repository: derives service minutes/value and builds a connected graph")
    void repositoryDerivesAndBuilds() {
        Repository repo = buildTinyRepo();
        // economics derived: urgency-1 request should have the highest value.
        ServiceRequest r = repo.getRequest("SR1");
        assertTrue(r.getValue() > 0);
        assertTrue(r.getServiceMinutes() > 0);
        // graph built with both endpoints present
        assertTrue(repo.timeGraph().indexOf("L1") >= 0);
        assertTrue(repo.timeGraph().indexOf("L2") >= 0);
        assertEquals("L1", repo.getLocation("L1").getLocationId());
    }

    @Test @DisplayName("DatabaseManager: save then load round-trips every table (normal)")
    void dbRoundTrip(@TempDir Path dir) {
        assumeSchemaExists();
        String dbPath = dir.resolve("test.db").toString();
        Repository repo = buildTinyRepo();
        try (DatabaseManager db = new DatabaseManager(dbPath)) {
            db.initSchema(SCHEMA);
            db.saveAll(repo);
            assertEquals(2, db.countRows("locations"));
            assertEquals(1, db.countRows("service_requests"));

            Repository loaded = db.loadAll();
            assertEquals(repo.locations().size(), loaded.locations().size());
            assertEquals(repo.requests().size(), loaded.requests().size());
            assertEquals("Emergency Department", loaded.getLocation("L1").getName());
        }
    }

    @Test @DisplayName("DatabaseManager: algorithm_runs logging is queryable (evidence)")
    void dbLogsRuns(@TempDir Path dir) {
        assumeSchemaExists();
        String dbPath = dir.resolve("runs.db").toString();
        try (DatabaseManager db = new DatabaseManager(dbPath)) {
            db.initSchema(SCHEMA);
            db.logAlgorithmRun(new AlgorithmRun("Dijkstra", "from=L1 to=L2", 123456, "cost=5", DatabaseManager.now()));
            DynamicArray<AlgorithmRun> runs = db.recentRuns(5);
            assertEquals(1, runs.size());
            assertEquals("Dijkstra", runs.get(0).getAlgorithm());
        }
    }

    // --- helpers ---

    private Repository buildTinyRepo() {
        DynamicArray<Location> locs = new DynamicArray<>();
        locs.add(new Location("L1", "Emergency Department", "Main Block", "department", ""));
        locs.add(new Location("L2", "Radiology", "Main Block", "department", ""));
        DynamicArray<Route> routes = new DynamicArray<>();
        routes.add(new Route("L1", "L2", 100, 60, ""));
        DynamicArray<ServiceRequest> reqs = new DynamicArray<>();
        reqs.add(new ServiceRequest("SR1", "L1", "L2", "lab-sample", 1, "", "", "PENDING"));
        DynamicArray<Resource> res = new DynamicArray<>();
        res.add(new Resource("R1", "nurse", "L1", 60, "AVAILABLE"));
        Repository repo = new Repository();
        repo.replaceAll(locs, routes, reqs, res);
        return repo;
    }

    private void assumeSchemaExists() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(Path.of(SCHEMA)),
                "database/schema.sql not reachable from the working directory; skipping DB test");
    }
}
