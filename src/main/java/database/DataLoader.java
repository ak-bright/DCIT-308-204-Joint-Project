package database;

import datastructures.DynamicArray;
import model.Location;
import model.Resource;
import model.Route;
import model.ServiceRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and writes the seed-data CSV files in {@code database/seed-data/}.
 *
 * <p>The loader is deliberately tolerant: it skips blank lines and comment lines
 * that begin with {@code #} (so any hand-written notes at the top of a file are
 * ignored), and it skips the header row. Fields are split with a small CSV
 * parser that understands double-quoted values containing commas.</p>
 *
 * <p>All results are returned in our own {@link DynamicArray} rather than a
 * {@code java.util} collection, keeping the data-path consistent with the rest
 * of the project.</p>
 */
public final class DataLoader {

    private final Path seedDir;

    public DataLoader(String seedDir) { this.seedDir = Path.of(seedDir); }
    public DataLoader(Path seedDir)   { this.seedDir = seedDir; }

    public boolean seedExists() {
        return Files.exists(seedDir.resolve("locations.csv"));
    }

    // ------------------------- readers -----------------------------------

    public DynamicArray<Location> loadLocations() {
        DynamicArray<Location> out = new DynamicArray<>();
        for (String[] f : rows("locations.csv", 5)) {
            out.add(new Location(f[0], f[1], f[2], f[3], f[4]));
        }
        return out;
    }

    public DynamicArray<Route> loadRoutes() {
        DynamicArray<Route> out = new DynamicArray<>();
        for (String[] f : rows("routes.csv", 5)) {
            out.add(new Route(f[0], f[1], parseD(f[2]), parseD(f[3]), f[4]));
        }
        return out;
    }

    public DynamicArray<ServiceRequest> loadRequests() {
        DynamicArray<ServiceRequest> out = new DynamicArray<>();
        for (String[] f : rows("service-requests.csv", 8)) {
            out.add(new ServiceRequest(f[0], f[1], f[2], f[3], parseI(f[4]), f[5], f[6], f[7]));
        }
        return out;
    }

    public DynamicArray<Resource> loadResources() {
        DynamicArray<Resource> out = new DynamicArray<>();
        for (String[] f : rows("resources.csv", 5)) {
            out.add(new Resource(f[0], f[1], f[2], parseI(f[3]), f[4]));
        }
        return out;
    }

    // ------------------------- writers -----------------------------------

    /** Overwrite the four CSVs from the current in-memory state (Save to CSV). */
    public void saveAll(DynamicArray<Location> locs, DynamicArray<Route> routes,
                        DynamicArray<ServiceRequest> reqs, DynamicArray<Resource> res) {
        writeLocations(locs);
        writeRoutes(routes);
        writeRequests(reqs);
        writeResources(res);
    }

    private void writeLocations(DynamicArray<Location> locs) {
        StringBuilder sb = new StringBuilder("locationId,name,area,type,notes\n");
        for (Location l : locs)
            sb.append(join(l.getLocationId(), l.getName(), l.getArea(), l.getType(), l.getNotes())).append('\n');
        write("locations.csv", sb);
    }

    private void writeRoutes(DynamicArray<Route> routes) {
        StringBuilder sb = new StringBuilder("fromLocationId,toLocationId,distance,travelTime,notes\n");
        for (Route r : routes)
            sb.append(join(r.getFromLocationId(), r.getToLocationId(),
                    fmt(r.getDistance()), fmt(r.getTravelTime()), r.getNotes())).append('\n');
        write("routes.csv", sb);
    }

    private void writeRequests(DynamicArray<ServiceRequest> reqs) {
        StringBuilder sb = new StringBuilder(
                "requestId,source,destination,category,urgency,timeSubmitted,deadline,status\n");
        for (ServiceRequest r : reqs)
            sb.append(join(r.getRequestId(), r.getSource(), r.getDestination(), r.getCategory(),
                    String.valueOf(r.getUrgency()), r.getTimeSubmitted(), r.getDeadline(), r.getStatus())).append('\n');
        write("service-requests.csv", sb);
    }

    private void writeResources(DynamicArray<Resource> res) {
        StringBuilder sb = new StringBuilder("resourceId,type,homeLocation,capacity,availabilityStatus\n");
        for (Resource r : res)
            sb.append(join(r.getResourceId(), r.getType(), r.getHomeLocation(),
                    String.valueOf(r.getCapacity()), r.getAvailabilityStatus())).append('\n');
        write("resources.csv", sb);
    }

    // ------------------------- helpers -----------------------------------

    /**
     * Read a CSV file into rows of trimmed fields, skipping comment/blank/header
     * lines. Each returned row is padded/truncated to {@code expectedCols}.
     */
    private DynamicArray<String[]> rows(String file, int expectedCols) {
        DynamicArray<String[]> out = new DynamicArray<>();
        Path p = seedDir.resolve(file);
        if (!Files.exists(p)) return out; // nothing to load
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line; boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue; // banner/comment
                if (!headerSkipped) { headerSkipped = true; continue; }     // column header row
                String[] parsed = parseCsvLine(line);
                String[] fixed = new String[expectedCols];
                for (int i = 0; i < expectedCols; i++) fixed[i] = i < parsed.length ? parsed[i].trim() : "";
                out.add(fixed);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + p, e);
        }
        return out;
    }

    /** Minimal CSV splitter: handles double-quoted fields containing commas. */
    private String[] parseCsvLine(String line) {
        DynamicArray<String> fields = new DynamicArray<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;               // toggle quote state
            } else if (c == ',' && !inQuotes) {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        String[] arr = new String[fields.size()];
        for (int i = 0; i < fields.size(); i++) arr[i] = fields.get(i);
        return arr;
    }

    private void write(String file, StringBuilder sb) {
        try {
            Files.createDirectories(seedDir);
            Files.writeString(seedDir.resolve(file), sb.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed writing " + file, e);
        }
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(quoteIfNeeded(parts[i] == null ? "" : parts[i]));
        }
        return sb.toString();
    }

    private static String quoteIfNeeded(String s) { return s.contains(",") ? "\"" + s + "\"" : s; }
    private static String fmt(double d) { return (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d); }
    private static double parseD(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; } }
    private static int parseI(String s)    { try { return Integer.parseInt(s.trim()); }    catch (Exception e) { return 0; } }
}
