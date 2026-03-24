# Architect Cloud UI

**Status**: Incubating proof of concept

`architect-cloud/ui` is the frontend surface for Architect Cloud. Today it is a small React + Vite monitoring stub that can poll the cloud backend REST API, but it does not yet provide a complete dashboard experience.

## Current scope

The current UI scope is intentionally narrow:

- boot a React application with Vite
- poll the cloud backend REST API for engines, projects, and executions
- keep the fetched data in local component state
- surface a basic error banner with a retry action

It is **not** yet a supported product dashboard. There is no routing, no dedicated view structure, no real-time WebSocket subscription, and no test or lint tooling configured.

## Current architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for the concrete frontend architecture and [STATUS.md](./STATUS.md) for the current support level and graduation criteria.

## What exists today

- `src/main.jsx` mounts a single React application into `index.html`
- `src/App.jsx` owns all UI state and backend polling logic
- `src/App.css` and `src/index.css` provide a minimal global style layer
- `vite.config.js` configures the Vite dev/build entrypoint

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

- dashboard rendering is still minimal
- data fetching is implemented with ad hoc polling in one component
- the backend's WebSocket event stream is not consumed by the UI
- `lint` and `test` scripts are still placeholders for future work
- no component, integration, or end-to-end tests exist yet
