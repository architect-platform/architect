# architect-cloud ui status

- Status: incubating
- Last reviewed: 2026-03-24
- Owner group: Product Surfaces

Current scope:
- React + Vite proof of concept for Architect Cloud monitoring.
- Polls the cloud backend REST API through typed API and hook modules.
- Provides a small summary-card view and error/retry experience today.

Current limitations:
- No complete dashboard views for engines, projects, or executions.
- No routing, shared state layer, or WebSocket event subscription.
- Lint, type-check, and test tooling now exist, but coverage remains minimal.
- Product behavior and support expectations are still being defined.

Graduation criteria:
- Publish a truthful frontend architecture and scope document.
- Implement and enforce non-empty lint and test scripts.
- Define stable feature scope and support expectations.
- Add real dashboard rendering and data-state structure.
