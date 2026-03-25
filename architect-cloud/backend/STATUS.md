# architect-cloud backend status

- Status: beta
- Version: 1.0.0
- Last reviewed: 2026-03-24
- Owner group: Product Surfaces

Current scope:
- Hexagonal-architecture REST API built on Micronaut and Kotlin.
- Receives engine registrations, project metadata, and execution events.
- Provides query APIs consumed by the Cloud UI dashboard.
- Uses H2 in-memory database for development (configurable for production).

Current health:
- Tests: ✅ passing (57 tests)
- ArchUnit enforcement validates hexagonal-layer boundaries.
- All API endpoints functional and covered by integration tests.

Current limitations:
- No authentication or authorisation layer.
- H2 is the only validated database driver; PostgreSQL config exists but is untested.
- WebSocket support for real-time event push is not implemented.

Graduation criteria:
- Add authentication and authorisation.
- Validate a production-grade database (PostgreSQL or MySQL).
- Implement WebSocket event streaming.
- Reach ≥ 80 % line coverage for domain and adapter layers.
