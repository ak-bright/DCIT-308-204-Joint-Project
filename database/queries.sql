-- ============================================================================
-- queries.sql — worked examples against the campus database
-- ============================================================================
-- Run with:   sqlite3 -header -column campus.db < queries.sql
--
-- These are the human-readable counterparts of the assertions in
-- test_database.py. They are the queries quoted in the final report.
-- ============================================================================

.print '--- Q1. How many places of each kind are on campus? ---'
SELECT category, count(*) AS places
  FROM locations
 GROUP BY category
 ORDER BY places DESC, category;


.print ''
.print '--- Q2. The road network as an adjacency list (first 10 edges) ---'
SELECT r.road_code,
       a.name AS from_place,
       b.name AS to_place,
       r.length_m,
       r.road_type
  FROM roads r
  JOIN locations a ON a.location_id = r.from_location_id
  JOIN locations b ON b.location_id = r.to_location_id
 WHERE r.is_active = 1
 ORDER BY r.length_m
 LIMIT 10;


.print ''
.print '--- Q3. Best-connected locations (highest degree in the graph) ---'
SELECT l.name AS hub,
       count(*) AS degree,
       round(avg(r.length_m), 1) AS avg_edge_m
  FROM roads r
  JOIN locations l
    ON l.location_id IN (r.from_location_id, r.to_location_id)
 WHERE r.is_active = 1
 GROUP BY l.location_id
 ORDER BY degree DESC, hub
 LIMIT 5;


.print ''
.print '--- Q4. Outstanding service requests, most urgent first ---'
SELECT sr.request_code,
       l.name AS place,
       sr.category,
       sr.priority,
       sr.status,
       COALESCE(res.name, '(unassigned)') AS handler
  FROM service_requests sr
  JOIN locations l   ON l.location_id  = sr.location_id
  LEFT JOIN resources res ON res.resource_id = sr.assigned_resource_id
 WHERE sr.status IN ('open', 'assigned', 'in_progress')
 ORDER BY CASE sr.priority
            WHEN 'critical' THEN 1 WHEN 'high' THEN 2
            WHEN 'medium'   THEN 3 ELSE 4 END,
          sr.reported_at
 LIMIT 10;


.print ''
.print '--- Q5. Median-ish response time by request category (resolved only) ---'
SELECT category,
       count(*) AS resolved,
       round(avg((julianday(resolved_at) - julianday(reported_at)) * 24), 1)
           AS avg_hours_to_resolve
  FROM service_requests
 WHERE status = 'resolved' AND resolved_at IS NOT NULL
 GROUP BY category
 ORDER BY avg_hours_to_resolve DESC;


.print ''
.print '--- Q6. Algorithm benchmark summary ---'
SELECT algorithm,
       purpose,
       count(*) AS runs,
       round(avg(runtime_ms), 2) AS avg_ms,
       round(max(runtime_ms), 2) AS worst_ms,
       sum(status = 'success') AS succeeded
  FROM algorithm_runs
 GROUP BY algorithm, purpose
 ORDER BY runs DESC;


.print ''
.print '--- Q7. Longest routes actually computed ---'
SELECT ar.run_code,
       ar.algorithm,
       s.name AS source,
       t.name AS target,
       round(ar.total_cost_m) AS route_m,
       json_array_length(ar.path_json) AS hops
  FROM algorithm_runs ar
  JOIN locations s ON s.location_id = ar.source_location_id
  JOIN locations t ON t.location_id = ar.target_location_id
 WHERE ar.status = 'success' AND ar.purpose = 'shortest_path'
 ORDER BY ar.total_cost_m DESC
 LIMIT 5;


.print ''
.print '--- Q8. Resource utilisation: how loaded is each unit? ---'
SELECT res.resource_code,
       res.name,
       res.resource_type,
       res.status,
       COALESCE(base.name, '(no base)') AS based_at,
       count(sr.request_id) AS jobs_assigned
  FROM resources res
  LEFT JOIN locations base ON base.location_id = res.base_location_id
  LEFT JOIN service_requests sr ON sr.assigned_resource_id = res.resource_id
 GROUP BY res.resource_id
 ORDER BY jobs_assigned DESC
 LIMIT 10;


.print ''
.print '--- Q9. Audit trail for the busiest entity ---'
SELECT ae.occurred_at,
       ae.entity_table,
       ae.action,
       ae.actor,
       ae.actor_role,
       COALESCE(ae.notes, '') AS notes
  FROM audit_events ae
 ORDER BY ae.occurred_at DESC
 LIMIT 10;


.print ''
.print '--- Q10. Data-quality sweep: anything that should not exist ---'
SELECT 'orphan roads'          AS check_name,
       count(*) AS offending_rows FROM roads r
  WHERE NOT EXISTS (SELECT 1 FROM locations l WHERE l.location_id = r.from_location_id)
UNION ALL
SELECT 'requests on inactive locations', count(*)
  FROM service_requests sr JOIN locations l ON l.location_id = sr.location_id
 WHERE l.is_active = 0
UNION ALL
SELECT 'assigned but no resource', count(*)
  FROM service_requests
 WHERE status IN ('assigned','in_progress') AND assigned_resource_id IS NULL
UNION ALL
SELECT 'resolved but no timestamp', count(*)
  FROM service_requests WHERE status = 'resolved' AND resolved_at IS NULL
UNION ALL
SELECT 'runs with impossible duration', count(*)
  FROM algorithm_runs WHERE finished_at IS NOT NULL AND finished_at < started_at;
