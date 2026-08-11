package model;

/**
 * A lightweight audit-trail entry for data-changing actions (add/update/delete a
 * location, route, request, or resource; load/save). Persisted to the
 * {@code audit_events} table so the report can show the system keeps a history.
 */
public class AuditEvent {
    private long id;
    private final String eventType;  // CREATE | UPDATE | DELETE | LOAD | SAVE
    private final String entity;     // Location | Route | ServiceRequest | Resource | Database
    private final String detail;     // human-readable description
    private final String occurredAt; // ISO timestamp string

    public AuditEvent(String eventType, String entity, String detail, String occurredAt) {
        this.eventType = eventType;
        this.entity = entity;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public long   getId()         { return id; }
    public void   setId(long id)  { this.id = id; }
    public String getEventType()  { return eventType; }
    public String getEntity()     { return entity; }
    public String getDetail()     { return detail; }
    public String getOccurredAt() { return occurredAt; }

    @Override
    public String toString() {
        return String.format("[%s] %s %s — %s", occurredAt, eventType, entity, detail);
    }
}
