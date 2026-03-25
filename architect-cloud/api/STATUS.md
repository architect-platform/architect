# architect-cloud api status

- Status: incubating
- Last reviewed: 2026-03-24
- Owner group: Product Surfaces

Current scope:
- Shared API types and contracts between the Cloud Backend and Agents.
- Intended to hold DTOs, status enumerations, and shared constants.
- Consumed as a Gradle composite-build dependency by sibling modules.

Current health:
- Tests: — (no source code yet; Gradle scaffold only)
- Build infrastructure exists (.gradle cache) but no compilable sources.

Current limitations:
- Module is scaffolded but has no source code.
- DTOs and contracts still live inline in the backend module.
- No published artefact or version.

Graduation criteria:
- Extract shared DTOs from the backend into this module.
- Add unit tests for serialisation round-trips.
- Publish a versioned artefact consumed by backend and agents.
- Define backward-compatibility policy for wire-format changes.
