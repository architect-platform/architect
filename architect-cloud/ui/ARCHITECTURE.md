# Architect Cloud UI Architecture

## Status

- **Support level**: incubating
- **Current role**: frontend proof of concept for the Architect Cloud backend
- **Primary goal today**: verify that a browser client can fetch and refresh backend monitoring data

## Product scope

The current product scope for `architect-cloud/ui` is:

> A single-page React + Vite monitoring stub that polls the Architect Cloud REST API and keeps the returned engine, project, and execution data in local component state.

That scope is deliberately smaller than a full dashboard product. The UI currently does **not** define multiple pages, stable workflows, or a complete operator experience.

## Technology stack

- **Framework**: React 18
- **Build tool**: Vite 5
- **Language**: JavaScript (no TypeScript)
- **State model**: local `useState` and `useEffect`
- **Styling**: global CSS files (`App.css`, `index.css`)
- **Transport**: browser `fetch()` calls to the cloud backend REST API

## Source layout

```text
architect-cloud/ui/
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── main.jsx
    ├── App.jsx
    ├── App.css
    └── index.css
```

## Runtime architecture

### Entry points

- `index.html` hosts the root DOM node
- `src/main.jsx` mounts the React tree
- `src/App.jsx` owns the entire application behavior

### Data flow

`App.jsx` currently performs all frontend orchestration:

1. define `API_BASE_URL` and a fixed `REFRESH_INTERVAL`
2. fetch `/api/engines`
3. fetch `/api/projects`
4. fetch `/api/executions/engine/{engineId}` for each engine
5. sort executions client-side by `startedAt`
6. store everything in local component state
7. re-run the fetch cycle on a polling interval

## Current architectural characteristics

- **Single-component application**: the top-level `App` component owns fetch logic, derived stats, loading state, error state, and rendering
- **No routing**: the UI is one screen with no navigation model
- **No shared state layer**: there is no query/cache abstraction, store, or typed API client
- **Polling instead of streaming**: the backend exposes WebSocket-based event streaming, but the UI currently refreshes with periodic REST polling only
- **Minimal rendering**: the current view renders a small summary-card view and an error banner, but not a full dashboard layout

## Known limitations

- no dedicated components for engines, projects, or executions
- no request cancellation, retry policy, or normalized cache
- no environment-based API configuration beyond in-file constants
- linting and unit-test harnesses exist, but coverage is still minimal
- no support statement beyond incubating proof-of-concept status

## Near-term architecture follow-ups

The remaining Phase 7 tasks build on this definition:

- replace empty lint/test scripts with real tooling
- introduce clearer API/state handling
- add component test coverage
- decide whether the UI is a supported product or a reference surface
