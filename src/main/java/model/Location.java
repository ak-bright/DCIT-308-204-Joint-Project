package model;

/**
 * A single physical point in the hospital/clinic network — a department, ward,
 * or service point (e.g. "Emergency Department", "Radiology", "Ward B3").
 *
 * <p>Locations are the <b>vertices</b> of the {@link datastructures.Graph} used
 * for routing. They are stored in our own hash table (by id) and BST (by name)
 * so the UI can look one up quickly either way.</p>
 *
 * <p>This is a plain data holder (POJO): no behaviour beyond field access, so it
 * stays easy for the non-technical team to read against the CSV columns.</p>
 */
public class Location {
    private final String locationId;   // stable primary key, e.g. "L001"
    private String name;               // human name, e.g. "Emergency Department"
    private String area;               // block/zone, e.g. "Main Block"
    private String type;               // ward | department | service-point | ...
    private String notes;              // free text (may be empty)

    public Location(String locationId, String name, String area, String type, String notes) {
        this.locationId = locationId;
        this.name = name;
        this.area = area;
        this.type = type;
        this.notes = notes;
    }

    public String getLocationId() { return locationId; }
    public String getName()       { return name; }
    public String getArea()       { return area; }
    public String getType()       { return type; }
    public String getNotes()      { return notes; }

    public void setName(String name)   { this.name = name; }
    public void setArea(String area)   { this.area = area; }
    public void setType(String type)   { this.type = type; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return String.format("%s | %-28s | %-14s | %s", locationId, name, area, type);
    }
}
