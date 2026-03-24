# Architect Cloud UI

**Status**: Incubating proof of concept

`architect-cloud/ui` is the frontend surface for Architect Cloud. Today it is a small React + Vite monitoring stub that can poll the cloud backend REST API, but it does not yet provide a complete dashboard experience.

## Current scope

The current UI scope is intentionally narrow:

- boot a React application with Vite
- poll the cloud backend REST API for engines, projects, and executions through a typed API layer
- keep the fetched data in typed hook state
- render a lightweight summary-card view plus an error banner with a retry action

It is **not** yet a supported product dashboard. There is no routing, no dedicated view structure, no real-time WebSocket subscription, and the current test coverage remains intentionally small.

## Current architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for the concrete frontend architecture and [STATUS.md](./STATUS.md) for the current support level and graduation criteria.

## What exists today

- `src/main.jsx` mounts a single React application into `index.html`
- `src/App.tsx` composes typed hook and presentational components
- `src/api/cloudApi.ts` defines the typed backend fetch layer
- `src/hooks/useCloudDashboard.ts` owns typed polling/state logic
- `src/components/` contains focused UI components
- `src/App.css` and `src/index.css` provide a minimal global style layer
- `vite.config.js` configures the Vite dev/build entrypoint
- `src/App.test.tsx` plus component tests cover the current fetch/error and rendering behavior
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
- the typed polling model still lives in a single dashboard hook
- the backend's WebSocket event stream is not consumed by the UI
- test coverage is still limited to the current hook-backed summary flow and small presentational components
- there are still no integration or end-to-end tests
