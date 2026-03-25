# architect-cloud agents status

- Status: incubating
- Last reviewed: 2026-03-24
- Owner group: Product Surfaces

Current scope:
- Docker Compose and Kubernetes agent definitions for remote Architect execution.
- Contains common configuration, a Docker Compose agent, and a Kubernetes agent.
- Packages the Architect Engine as a managed, registerable cloud agent.

Current health:
- Tests: — (no tests; infrastructure definitions only)
- No source code yet; directories contain Gradle caches from the parent build.

Current limitations:
- Agent definitions are scaffolded but contain no deployable artefacts.
- No CI/CD pipeline validates agent builds or deployments.
- Health-check and readiness probes are not defined.

Graduation criteria:
- Ship working Docker Compose and Kubernetes manifests that start a real agent.
- Add CI smoke tests that build and health-check the agent containers.
- Document required environment variables and networking prerequisites.
- Validate end-to-end agent registration with the Cloud Backend.
