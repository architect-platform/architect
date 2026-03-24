# architect-cloud ui status

- Status: incubating
- Last reviewed: 2026-03-24
- Owner group: Product Surfaces

Current scope:
- React + Vite proof of concept for Architect Cloud monitoring.
- Polls the cloud backend REST API from a single `App.jsx` component.
- Provides only a minimal shell and error/retry experience today.

Current limitations:
- No complete dashboard views for engines, projects, or executions.
- No routing, shared state layer, or WebSocket event subscription.
- Test and lint surface is incomplete.
- Product behavior and support expectations are still being defined.

Graduation criteria:
- Publish a truthful frontend architecture and scope document.
- Implement and enforce non-empty lint and test scripts.
- Define stable feature scope and support expectations.
- Add real dashboard rendering and data-state structure.
