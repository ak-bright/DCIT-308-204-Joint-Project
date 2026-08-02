# Dataset Evidence Note: UG Legon Mapping

## Methodology
This dataset models the University of Ghana, Legon campus as a networked graph. Nodes represent distinct physical structures or functional areas, and edges represent the transit paths between them.

## Spatial Clustering
1. **Academic Core:** Locations such as the Balme Library, JQB, N-Block, and Central Block are clustered tightly. The distances generated for these edges are constrained to 50-300 meters to reflect actual walking distances via footpaths.
2. **Residential Periphery:** Halls like Pentagon, TF Hostel, and the Diaspora Halls represent the outer nodes of the campus graph. Paths connecting these to the Academic Core are classified predominantly as `shuttle_route` with distances scaling up to 1,500 meters.
3. **Service Hubs:** The University Hospital and UGCS Labs operate as high-priority nodes for specific service requests (`maintenance_ticket`, `lab_equipment_request`), dictating the distribution of generated `service_requests.csv` data.

## Real-World Constraints Applied
- **No Self-Loops:** A road cannot have the same source and target location.
- **Resource Distribution:** Shuttles and maintenance vans are assigned to active nodes (e.g., Main Gate, Night Market) mirroring real campus asset staging.
