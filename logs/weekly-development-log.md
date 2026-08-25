# Weekly Development Log

**Project:** Campus Service Routing & Resource Allocation Engine
**Team index number:** 22268310
**Log period:** Week ending 2026-08-01
**Data snapshot:** `service_requests.csv`, `resources.csv`, `locations.csv` (pulled 2026-08-01 18:00)
**Author:** Engineering team

---

## 1. Summary

This week's work was split between (a) locking in the index-derived algorithm parameters and verifying their arithmetic, and (b) running the first full profiling pass of the three seed datasets against those parameters. All three parameters were confirmed correct against the derivation formulas. Profiling surfaced two structural problems that will shape next sprint: a **geographic mismatch between where shuttles idle and where shuttle demand originates**, and a **micro-cluster problem** where 35% of all requests come from location groups whose members are less than 21 m apart — far below the routing threshold and therefore invisible to the current routing logic.

Capacity itself is not the bottleneck. Every open queue clears in a single algorithm cycle. The bottleneck is routing quality and resource placement.

---

## 2. Dataset ingest

Three CSVs were ingested and validated. No malformed rows, no missing fields, no orphaned foreign keys — every `location_id` in `service_requests.csv` and `resources.csv` resolves to a row in `locations.csv`.

| File | Rows | Key | Notes |
|---|---|---|---|
| `service_requests.csv` | 350 | `request_id` | 2026-07-03 07:22 → 2026-08-01 17:58 (29-day window) |
| `resources.csv` | 35 | `resource_id` | All carry `batch_capacity = 20`, matching Parameter 1 |
| `locations.csv` | 55 | `location_id` | 28 Lecture Venue, 18 Facility, 9 Hall |

`resources.csv` already ships with `batch_capacity` hard-coded to 20 across all 35 rows, which agrees with our independently derived Parameter 1. That agreement is a useful sanity check — it means the seed data was generated against the same index number we are tuning to, and we do not need a migration to align the two.

---

## 3. Parameter verification

All three parameters from `indexnumberparameters.md` were re-derived from base index **22268310** and confirmed. Full working below, since the modulo results are non-obvious and one of them produces a zero remainder that is easy to mistake for an error.

### Parameter 1 — Request Batch Size

```
22268310 mod 25 = 10
10 + 10        = 20
```

22268310 = 25 × 890732 + 10 → remainder **10**, so batch size is **20**. ✅ Matches the spec.

### Parameter 2 — Route Optimization Threshold

```
22268310 mod 500 = 310
310 + 200        = 510
```

22268310 = 500 × 44536 + 310 → remainder **310**, so threshold is **510 m**. ✅ Matches the spec.

### Parameter 3 — Resource Allocation Timeout

```
22268310 mod 30 = 0
0 + 15          = 15
```

22268310 = 30 × 742277 + 0 → remainder **0**, so timeout is **15 s**. ✅ Matches the spec.

**Note for reviewers:** Parameter 3's remainder is exactly zero (22268310 is divisible by 30, since it is divisible by 2, 3, and 5). The result of 15 s is therefore the *floor* of the intended 15–45 s range, not a truncation bug. This is our tightest possible escalation window and it was flagged as a risk — see §7.

**Locked parameter set:**

| Parameter | Value | Applies to |
|---|---|---|
| Request Batch Size | 20 requests/cycle | Batching stage |
| Route Optimization Threshold | 510 m | Footpath → shuttle_route switch |
| Resource Allocation Timeout | 15 s | High-priority escalation |

---

## 4. Request profile

350 requests over 29 days, averaging ~12/day with a clear ramp after week 27.

**By status**

| Status | Count | Share |
|---|---|---|
| resolved | 203 | 58.0% |
| in_progress | 74 | 21.1% |
| pending | 55 | 15.7% |
| cancelled | 18 | 5.1% |

**Open backlog = 129 requests** (pending + in_progress).

**By request type**

| Type | Count | Share | Resolved | Cancelled |
|---|---|---|---|---|
| shuttle_ride | 157 | 44.9% | 59.2% | 5.7% |
| maintenance_ticket | 69 | 19.7% | 59.4% | 1.4% |
| cleaning_request | 59 | 16.9% | 54.2% | 6.8% |
| lab_equipment_request | 42 | 12.0% | 59.5% | 7.1% |
| security_escort | 23 | 6.6% | 52.2% | 4.3% |

Resolution rates are remarkably flat across types — a 7.3-point spread between best and worst. That uniformity suggests resolution is currently governed by something type-independent (elapsed time, or the generator's own distribution) rather than by per-type service quality. We should not read `security_escort` at 52.2% as an SLA problem yet.

**By priority**

| Priority | Count | Share |
|---|---|---|
| low | 225 | 64.3% |
| medium | 69 | 19.7% |
| high | 54 | 15.4% |
| critical | 2 | 0.6% |

**Volume by ISO week**

| Week | Requests |
|---|---|
| 2026-W27 (partial) | 40 |
| 2026-W28 | 79 |
| 2026-W29 | 83 |
| 2026-W30 | 83 |
| 2026-W31 (partial) | 65 |

Steady state is ~83/week. W27 and W31 are truncated by the snapshot boundaries, not a real dip.

---

## 5. Resource pool

35 resources, all with `batch_capacity = 20`.

| Type | Total | Available | Busy | Offline |
|---|---|---|---|---|
| campus_shuttle | 9 | 6 | 2 | 1 |
| janitorial_staff | 8 | 7 | 1 | 0 |
| maintenance_van | 8 | 4 | 2 | 2 |
| security_patrol | 7 | 6 | 1 | 0 |
| lab_technician | 3 | 2 | 0 | 1 |
| **Total** | **35** | **25 (71%)** | **6 (17%)** | **4 (11%)** |

**maintenance_van is the weakest pool** — only 4 of 8 units are available, with 2 offline and 2 busy. It carries the second-largest backlog and both critical tickets.

**lab_technician is the thinnest pool** in absolute terms: 3 units, one offline, leaving 2 to serve 42 lab equipment requests over the window.

### Backlog against capacity (batch = 20)

| Type | Open | Available resources | Cycle capacity | Cycles to clear |
|---|---|---|---|---|
| shuttle_ride | 55 | 6 shuttles | 120 | 1 |
| maintenance_ticket | 27 | 4 vans | 80 | 1 |
| cleaning_request | 23 | 7 janitorial | 140 | 1 |
| lab_equipment_request | 14 | 2 technicians | 40 | 1 |
| security_escort | 10 | 6 patrols | 120 | 1 |

**Every queue clears in one cycle.** Total open backlog (129) is 25.8% of total single-cycle capacity (500). Raw throughput is not the constraint this system faces — routing quality and placement are. This reframes the sprint: we should stop optimizing for batch throughput and start optimizing for travel distance.

---

## 6. Routing analysis against the 510 m threshold

With 55 locations there are 1,485 unique location pairs. Haversine distances computed on the supplied lat/long:

| Metric | Value |
|---|---|
| Minimum pair distance | 1.2 m |
| Median pair distance | 580.9 m |
| Mean pair distance | 639.8 m |
| Maximum pair distance | 2,787.4 m (Pentagon Hostel → Main Gate) |
| Pairs **above** 510 m (→ shuttle_route) | 874 (58.9%) |
| Pairs **at or below** 510 m (→ footpath) | 611 (41.1%) |

The 510 m threshold lands just under the median pair distance, splitting the campus roughly 59/41 in favour of shuttle routing. That is a defensible split — it is not degenerate in either direction, which is the main risk with an index-derived constant.

### Finding 6a — Shuttle placement is misaligned with demand

All 6 available shuttles idle at residence halls: Elizabeth Sey (RES_001), Pentagon Hostel (RES_005, RES_013), Commonwealth Hall (RES_006), Jean Nelson Hall (RES_009, RES_023). Measuring each of the 55 open shuttle requests to its nearest available shuttle:

| Metric | Distance |
|---|---|
| Minimum | 0 m (request originates at an idle shuttle's location) |
| Median | 323 m |
| Maximum | 886 m |
| **Beyond the 510 m threshold** | **16 of 55 (29%)** |

Nearly a third of open shuttle demand sits farther from its nearest shuttle than the threshold we use to decide that walking is unreasonable. Two shuttles are double-parked at Pentagon Hostel and two at Jean Nelson Hall while no shuttle sits anywhere near the academic core. Redistributing one Pentagon unit and one Jean Nelson unit toward the Great Hall / JQB corridor is the single highest-leverage change available and costs nothing but a repositioning instruction.

### Finding 6b — Micro-clusters defeat the threshold entirely

Four location groups are internally so tight that every intra-group pair falls under the threshold by two orders of magnitude:

| Cluster | Locations | Max internal span | Requests |
|---|---|---|---|
| JQB Rooms | LOC_025–LOC_039 | 20.9 m | 66 |
| Balme Library Sections | LOC_040–LOC_047 | 20.9 m | 26 |
| Business School Theatres | LOC_048–LOC_052 | 16.2 m | 20 |
| Night Market Stalls | LOC_053–LOC_055 | 19.2 m | 11 |

**123 requests — 35.1% of all traffic — originate inside these clusters.** A 20 m span against a 510 m threshold means the routing engine will never distinguish between JQB Room 2 and JQB Room 15; they are the same point as far as the algorithm is concerned. Treating them as 31 separate routing nodes wastes the batching stage's time and produces routes that ping-pong between coordinates that are functionally identical.

**Proposed fix:** collapse each cluster into a single routing supernode for the pathfinding stage, then fan out to the specific room/section/stall at the delivery stage. This reduces the routing graph from 55 nodes to 28 and should measurably shrink batch-assembly time without any loss of delivery precision.

### Finding 6c — Demand concentrates in residence halls

Top 10 request origins:

| Location | Type | Requests |
|---|---|---|
| Volta Hall | Hall | 23 |
| TF Hostel | Hall | 23 |
| ISH 1 | Hall | 22 |
| Jean Nelson Hall | Hall | 20 |
| Legon Hall | Hall | 19 |
| Elizabeth Sey Hall | Hall | 18 |
| Akuafo Hall | Hall | 16 |
| Commonwealth Hall | Hall | 16 |
| Pentagon Hostel | Hall | 14 |
| JQB Room 15 | Lecture Venue | 10 |

Nine of the top ten are residence halls. The 9 Hall locations generate 171 requests (48.9%) from 16.4% of the location count. Together with Finding 6a this suggests the shuttles are *not* badly placed for total volume — they are badly placed for the **academic-core minority** that has no nearby coverage at all.

### Finding 6d — Shuttle demand is sharply diurnal

Shuttle requests by hour (157 total) peak hard in the late morning:

| Hour | 08 | 09 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 |
|---|---|---|---|---|---|---|---|---|---|---|
| Requests | 12 | 7 | 15 | 19 | 12 | 14 | 9 | 15 | 11 | 9 |

The 08:00–17:00 band carries 114 of 157 shuttle requests (72.6%), with a distinct 11:00 peak. Overnight hours (00:00–05:00) total 9 requests. The batch size of 20 is generous during peak and almost meaningless overnight — a fixed batch window will sit half-empty for most of the night. Worth considering a time-of-day batch fill timeout in a later sprint, though that would be an addition to the parameter set rather than a change to it.

---

## 7. Risks and open issues

**R1 — 15 s escalation timeout is at the floor of the range (medium risk).**
Parameter 3's derivation produced a zero remainder, giving us the minimum possible value of the 15–45 s band. A 15 s window to acknowledge a high-priority request is tight, and 26 open requests currently carry high or critical priority. If acknowledgement latency in the real system exceeds 15 s with any regularity, we will escalate through the entire available pool before anyone responds. The parameter is correctly derived and we are not proposing to change it — but we need instrumentation on actual acknowledgement latency before we trust it in production.

**R2 — maintenance_van pool is degraded (medium risk).**
Half the fleet is unavailable (2 offline: RES_017 at Athletic Oval, RES_035 at Night Market Stall 2; 2 busy). Both critical tickets are maintenance tickets. Capacity math still says one cycle, but that assumes the 4 remaining vans stay up. A single further outage takes us to 3 vans against a 27-ticket backlog.

**R3 — Two critical tickets are aging (high risk).**

| Request | Location | Status | Opened | Age at snapshot |
|---|---|---|---|---|
| REQ_0082 | Balme Library Section 3 | **pending** | 2026-07-03 14:29 | 29 days |
| REQ_0331 | JQB Room 2 | in_progress | 2026-07-18 07:12 | 14 days |

REQ_0082 has been **pending — not even in progress — for 29 days**, the entire span of the dataset. It is the oldest unactioned item in the system at the highest priority level. Whatever escalation path exists did not fire for it. This is the clearest evidence we have that priority is not currently driving dispatch order, and it should be the first thing the allocation logic is tested against.

**R4 — lab_technician pool has no redundancy (low-medium risk).**
3 technicians, 1 offline. A second outage halves remaining capacity and the 14 open lab requests include 8 at high priority.

---

## 8. Decisions taken

1. **Parameter set is locked** at batch=20, threshold=510 m, timeout=15 s. All three verified against the derivation formulas. No changes proposed.
2. **Batch size will not be tuned this sprint.** Capacity analysis shows every queue clearing in one cycle; tuning batch size optimizes a stage that is not constrained.
3. **Routing graph will be collapsed to supernodes** for the four identified micro-clusters (55 nodes → 28) ahead of the next pathfinding build.
4. **Shuttle repositioning is recommended** but flagged as an operational change requiring sign-off, not a code change we can merge unilaterally.

---

## 9. Next week

- [ ] Implement micro-cluster supernode collapse in the routing graph builder; benchmark batch-assembly time before/after.
- [ ] Add acknowledgement-latency instrumentation so R1 can be evaluated against real data rather than assumption.
- [ ] Trace REQ_0082 through the allocation logic and determine why a 29-day-old critical ticket never left `pending`.
- [ ] Model the effect of repositioning 2 shuttles to the academic core; target is cutting the 16 over-threshold open requests to under 5.
- [ ] Confirm whether RES_017 and RES_035 (offline maintenance vans) are recoverable or need replacement in the resource pool.
- [ ] Draft a proposal for time-of-day-aware batch fill timeouts as a future parameter addition (informed by §6d).

---

## Appendix — Method notes

- Distances computed by the haversine formula on WGS-84 coordinates with R = 6,371,000 m. This is straight-line distance and **overstates walkability** — real footpath distance will always be longer, so the 29% of shuttle requests exceeding the threshold is a floor, not a ceiling.
- "Open" is defined as `status ∈ {pending, in_progress}`.
- Cycle capacity = (available resources of matching type) × 20, using the mapping: shuttle_ride→campus_shuttle, cleaning_request→janitorial_staff, maintenance_ticket→maintenance_van, security_escort→security_patrol, lab_equipment_request→lab_technician.
- Resources with status `busy` or `offline` are excluded from capacity figures.
- Week boundaries follow ISO 8601.
