# architect-cloud status

- Status: beta
- Last reviewed: 2026-03-24
- Owner group: Product Surfaces

Current scope:
- Multi-tenant cloud service for tracking and managing Architect Engine instances.
- Umbrella module containing backend, UI, agents, and shared API definitions.
- Backend is beta-quality; UI and agents are still incubating.

Current limitations:
- UI dashboard is an incubating proof of concept, not a supported product.
- Agent definitions are scaffolded but not yet fully implemented.
- Shared API module has no source code yet (Gradle scaffold only).
- Authentication and multi-tenancy are not implemented.

Graduation criteria:
- Backend reaches stable with production database support and auth.
- UI graduates from incubating to beta with real dashboard views.
- Agent definitions are validated in Docker Compose and Kubernetes environments.
- Shared API module contains canonical DTOs consumed by backend and agents.
