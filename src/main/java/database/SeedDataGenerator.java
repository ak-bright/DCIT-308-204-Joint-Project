package database;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Builds the project's operational dataset for a Ghanaian hospital / clinic
 * network and writes it to {@code database/seed-data/} as four CSV files whose
 * columns match the raw templates in {@code /data/}.
 *
 * <p>The dataset is <b>synthetic but coherent</b>: real departments are placed in
 * sensible blocks, every corridor is a real-looking walkway, and the numbers stay
 * in believable ranges (metres, seconds, minutes). It is generated from a fixed
 * random seed so re-running always produces the exact same files — the report and
 * the benchmarks therefore describe a stable, reproducible dataset.</p>
 *
 * <p>Sizes: 60 locations, ~150 routes (guaranteed connected), 320 service
 * requests, 36 resources.</p>
 *
 * <p>Run with: {@code java -cp out/main database.SeedDataGenerator}</p>
 */
public final class SeedDataGenerator {

    /**
     * The 36 named clinical / support units, each pinned to a realistic block and
     * given a short human note. Columns: name, area (block), type, note.
     * Locations L001..L036 are taken from this table in order; the rest are wards.
     */
    private static final String[][] DEPARTMENTS = {
        {"Emergency Department",   "Emergency Block",      "department", "24-hour walk-in and ambulance intake"},
        {"Outpatient Department",  "OPD Block",            "department", "General outpatient consulting rooms"},
        {"Radiology",              "Diagnostics Block",    "department", "X-ray and imaging reception"},
        {"Pharmacy",               "Main Block",           "department", "Main drug dispensary"},
        {"Laboratory",             "Diagnostics Block",    "department", "Clinical and pathology laboratory"},
        {"Theatre",                "Surgical Block",       "unit",       "Main operating theatres"},
        {"Maternity",              "Maternity Block",      "department", "Delivery suite and postnatal ward"},
        {"Antenatal Clinic",       "Maternity Block",      "department", "Antenatal consulting rooms"},
        {"Paediatrics",            "Main Block",           "department", "Children's ward and clinic"},
        {"Physiotherapy",          "OPD Block",            "department", "Rehabilitation gym"},
        {"Dental Clinic",          "OPD Block",            "department", "Dental surgery"},
        {"Eye Clinic",             "OPD Block",            "department", "Ophthalmology consulting"},
        {"ENT Clinic",             "OPD Block",            "department", "Ear, nose and throat clinic"},
        {"Dialysis Unit",          "Main Block",           "unit",       "Renal dialysis stations"},
        {"Blood Bank",             "Diagnostics Block",    "unit",       "Blood storage and cross-matching"},
        {"Mortuary",               "Support Services",     "unit",       "Body storage and release"},
        {"Records Office",         "Administration Block", "department", "Patient folders and registry"},
        {"Admissions",             "Administration Block", "department", "Inpatient admissions desk"},
        {"Cashier / Billing",      "Administration Block", "department", "Payments and NHIS claims"},
        {"Intensive Care Unit",    "Surgical Block",       "unit",       "Critical-care monitored beds"},
        {"Coronary Care Unit",     "Surgical Block",       "unit",       "Cardiac monitoring beds"},
        {"Burns Unit",             "Surgical Block",       "unit",       "Burns and plastics ward"},
        {"Oncology",               "Main Block",           "department", "Cancer treatment clinic"},
        {"Chemotherapy Suite",     "Main Block",           "unit",       "Day chemotherapy chairs"},
        {"Endoscopy",              "Diagnostics Block",    "unit",       "Endoscopy suite"},
        {"Ultrasound",             "Diagnostics Block",    "unit",       "Ultrasound scanning rooms"},
        {"CT Scan Suite",          "Diagnostics Block",    "unit",       "Computed-tomography suite"},
        {"MRI Suite",              "Diagnostics Block",    "unit",       "Magnetic-resonance imaging suite"},
        {"Central Sterile Supply", "Support Services",     "unit",       "Instrument sterilisation"},
        {"Kitchen / Catering",     "Support Services",     "department", "Patient and staff meals"},
        {"Laundry",                "Support Services",     "department", "Linen washing and distribution"},
        {"Chapel",                 "Support Services",     "department", "Multi-faith prayer room"},
        {"Staff Canteen",          "Support Services",     "department", "Staff dining"},
        {"Pharmacy Store",         "Support Services",     "unit",       "Bulk drug storage"},
        {"Oxygen Plant",           "Support Services",     "unit",       "Medical oxygen generation"},
        {"Generator House",        "Support Services",     "unit",       "Backup power supply"}
    };

    /** Ward name stems; combined with a running number to make unique ward names. */
    private static final String[] WARD_TYPES = {
        "Male Medical Ward", "Female Medical Ward", "Paediatric Ward", "Surgical Ward",
        "Maternity Ward", "Recovery Bay", "Isolation Room", "Amenity Ward"
    };
    /** Residential blocks a ward can sit in. */
    private static final String[] WARD_BLOCKS = {
        "Main Block", "Maternity Block", "Surgical Block", "Emergency Block"
    };
    /** Short descriptions attached to a corridor/route. */
    private static final String[] CORRIDOR_NOTES = {
        "covered walkway", "main corridor", "link corridor", "service road",
        "external path", "ramp corridor"
    };
    private static final String[] REQUEST_CATEGORIES = {
        "patient-transfer", "lab-sample", "equipment-delivery", "medication-run",
        "cleaning", "porter-escort", "blood-delivery", "specimen-collection"
    };
    /** People-type resources (larger shift windows). */
    private static final String[] STAFF_TYPES = { "nurse", "porter", "doctor", "lab-tech", "cleaner" };
    /** Equipment-type resources (shorter availability windows). */
    private static final String[] EQUIPMENT_TYPES = { "ambulance", "wheelchair", "trolley", "oxygen-cylinder" };
    /** Availability pool weighted towards AVAILABLE (3 in 5). */
    private static final String[] AVAIL = { "AVAILABLE", "AVAILABLE", "AVAILABLE", "BUSY", "OFFLINE" };

    public static void main(String[] args) throws IOException {
        Path outDir = Path.of("database", "seed-data");
        Files.createDirectories(outDir);
        Random rng = new Random(20260811L); // fixed seed => the dataset is reproducible

        int nLocations = 60;
        int nResources = 36;
        int nRequests  = 320;

        String[] locIds = writeLocations(outDir, nLocations, rng);
        writeRoutes(outDir, locIds, rng);
        writeResources(outDir, locIds, nResources, rng);
        writeRequests(outDir, locIds, nRequests, rng);

        System.out.println("Seed data written to " + outDir.toAbsolutePath());
    }

    private static String[] writeLocations(Path dir, int n, Random rng) throws IOException {
        String[] ids = new String[n];
        int wardCounter = 0;
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve("locations.csv")))) {
            w.println("locationId,name,area,type,notes");
            for (int i = 0; i < n; i++) {
                String id = String.format("L%03d", i + 1);
                ids[i] = id;
                String name, area, type, note;
                if (i < DEPARTMENTS.length) {                 // named departments/units
                    name = DEPARTMENTS[i][0];
                    area = DEPARTMENTS[i][1];
                    type = DEPARTMENTS[i][2];
                    note = DEPARTMENTS[i][3];
                } else {                                      // numbered inpatient wards
                    String stem = WARD_TYPES[wardCounter % WARD_TYPES.length];
                    int number  = wardCounter / WARD_TYPES.length + 1; // 1, 2, 3, ...
                    wardCounter++;
                    name = stem + " " + number;               // unique, e.g. "Surgical Ward 2"
                    area = WARD_BLOCKS[rng.nextInt(WARD_BLOCKS.length)];
                    type = "ward";
                    note = (12 + rng.nextInt(24)) + "-bed inpatient ward"; // 12..35 beds
                }
                w.printf("%s,%s,%s,%s,%s%n", id, csv(name), csv(area), type, csv(note));
            }
        }
        return ids;
    }

    private static void writeRoutes(Path dir, String[] locIds, Random rng) throws IOException {
        int n = locIds.length;
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve("routes.csv")))) {
            w.println("fromLocationId,toLocationId,distance,travelTime,notes");
            boolean[][] seen = new boolean[n][n];
            int edges = 0;
            // 1) A spanning chain guarantees the whole network is connected.
            for (int i = 1; i < n; i++) {
                int j = rng.nextInt(i);           // link i to some earlier node
                writeEdge(w, locIds, i, j, seen, rng);
                edges++;
            }
            // 2) Extra random corridors up to ~150 edges, adding alternate routes.
            while (edges < 150) {
                int a = rng.nextInt(n), b = rng.nextInt(n);
                if (a == b || seen[a][b]) continue; // no self-loops or duplicates
                writeEdge(w, locIds, a, b, seen, rng);
                edges++;
            }
        }
    }

    private static void writeEdge(PrintWriter w, String[] locIds, int a, int b,
                                  boolean[][] seen, Random rng) {
        seen[a][b] = seen[b][a] = true;
        int distance = 20 + rng.nextInt(400);               // metres between the two points
        int travel   = distance / 2 + 10 + rng.nextInt(60); // seconds, roughly walking pace
        String note  = CORRIDOR_NOTES[rng.nextInt(CORRIDOR_NOTES.length)];
        w.printf("%s,%s,%d,%d,%s%n", locIds[a], locIds[b], distance, travel, csv(note));
    }

    private static void writeResources(Path dir, String[] locIds, int n, Random rng) throws IOException {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve("resources.csv")))) {
            w.println("resourceId,type,homeLocation,capacity,availabilityStatus");
            for (int i = 0; i < n; i++) {
                String id = String.format("R%03d", i + 1);
                String type; int capacity;
                // Alternate staff and equipment so both kinds appear in the data.
                if (i % 2 == 0) {
                    type = STAFF_TYPES[rng.nextInt(STAFF_TYPES.length)];
                    capacity = 60 + rng.nextInt(121);       // 60..180 minutes of shift time
                } else {
                    type = EQUIPMENT_TYPES[rng.nextInt(EQUIPMENT_TYPES.length)];
                    capacity = 20 + rng.nextInt(71);        // 20..90 minutes of availability
                }
                String home   = locIds[rng.nextInt(locIds.length)];
                String status = AVAIL[rng.nextInt(AVAIL.length)];
                w.printf("%s,%s,%s,%d,%s%n", id, type, home, capacity, status);
            }
        }
    }

    private static void writeRequests(Path dir, String[] locIds, int n, Random rng) throws IOException {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve("service-requests.csv")))) {
            w.println("requestId,source,destination,category,urgency,timeSubmitted,deadline,status");
            for (int i = 0; i < n; i++) {
                String id = String.format("SR%04d", i + 1);
                String src = locIds[rng.nextInt(locIds.length)];
                String dst = locIds[rng.nextInt(locIds.length)];
                while (dst.equals(src)) dst = locIds[rng.nextInt(locIds.length)]; // src != dst
                String cat = REQUEST_CATEGORIES[rng.nextInt(REQUEST_CATEGORIES.length)];
                int urgency = 1 + rng.nextInt(5);           // 1 (critical) .. 5 (routine)
                int hour = 6 + rng.nextInt(14);             // submitted between 06:00 and 19:00
                int min  = rng.nextInt(60);
                String submitted = String.format("2026-08-11T%02d:%02d", hour, min);
                int leadMin = 15 + rng.nextInt(180);        // deadline 15..194 minutes later
                int dHour = hour + (min + leadMin) / 60, dMin = (min + leadMin) % 60;
                if (dHour > 23) dHour = 23;                 // clamp to the same day
                String deadline = String.format("2026-08-11T%02d:%02d", dHour, dMin);
                w.printf("%s,%s,%s,%s,%d,%s,%s,%s%n", id, src, dst, cat, urgency, submitted, deadline, "PENDING");
            }
        }
    }

    /** Quote a CSV field only if it contains a comma. */
    private static String csv(String s) { return s.contains(",") ? "\"" + s + "\"" : s; }

    private SeedDataGenerator() {}
}
