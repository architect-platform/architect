# Architect Cloud UI

**Status**: Incubating proof of concept

`architect-cloud/ui` is the frontend surface for Architect Cloud. Today it is a small React + Vite monitoring stub that can poll the cloud backend REST API, but it does not yet provide a complete dashboard experience.

## Current scope

The current UI scope is intentionally narrow:

- boot a React application with Vite
- poll the cloud backend REST API for engines, projects, and executions
- keep the fetched data in local component state
- render a lightweight summary-card view plus an error banner with a retry action

It is **not** yet a supported product dashboard. There is no routing, no dedicated view structure, no real-time WebSocket subscription, and the current lint/test coverage is still minimal.

## Current architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for the concrete frontend architecture and [STATUS.md](./STATUS.md) for the current support level and graduation criteria.

## What exists today

- `src/main.jsx` mounts a single React application into `index.html`
- `src/App.jsx` owns all UI state and backend polling logic
- `src/App.css` and `src/index.css` provide a minimal global style layer
- `vite.config.js` configures the Vite dev/build entrypoint
- `src/App.test.jsx` covers the current fetch/error behavior with Vitest + Testing Library
- `eslint.config.js` provides repo-local frontend linting for the UI surface

## Local development

```bash
cd architect-cloud/ui
npm install
npm run dev
```

The Vite dev server runs on port `3000`.

## Build

```bash
cd architect-cloud/ui
npm run build
npm run preview
```

## Known gaps

- dashboard rendering is still minimal and limited to summary cards
- data fetching is implemented with ad hoc polling in one component
- the backend's WebSocket event stream is not consumed by the UI
- test coverage is currently limited to the main `App` component behavior
- there are still no integration or end-to-end tests
