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
- **Language**: TypeScript
- **State model**: typed polling hook built on `useState`, `useEffect`, `useMemo`, and `useCallback`
- **Styling**: global CSS files (`App.css`, `index.css`)
- **Transport**: browser `fetch()` calls to the cloud backend REST API

## Source layout

```text
architect-cloud/ui/
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── api/
    ├── components/
    ├── hooks/
    ├── types/
    ├── main.tsx
    ├── App.tsx
    ├── App.css
    └── index.css
```

## Runtime architecture

### Entry points

- `index.html` hosts the root DOM node
- `src/main.tsx` mounts the React tree
- `src/App.tsx` coordinates the typed dashboard hook and presentational components
- `src/api/cloudApi.ts` contains typed fetch helpers for backend responses
- `src/hooks/useCloudDashboard.ts` owns the polling lifecycle and typed state
- `src/types/cloud.ts` defines the shared response/state contracts used by the UI

### Data flow

The typed dashboard flow currently works like this:

1. `useCloudDashboard()` triggers a refresh on mount and on the polling interval
2. `cloudApi.ts` fetches `/api/engines`
3. `cloudApi.ts` fetches `/api/projects`
4. `cloudApi.ts` fetches `/api/executions/engine/{engineId}` for each engine
5. executions are sorted client-side by `startedAt`
6. the hook stores typed state plus `lastUpdated`
7. `App.tsx` renders typed summary components from the hook state

## Current architectural characteristics

- **Small typed module split**: the UI is now divided into typed API, hook, component, and type modules
- **No routing**: the UI is one screen with no navigation model
- **Hook-based state layer**: there is a typed dashboard hook, but no broader query/cache/store abstraction yet
- **Polling instead of streaming**: the backend exposes WebSocket-based event streaming, but the UI currently refreshes with periodic REST polling only
- **Minimal rendering**: the current view renders a small summary-card view and an error banner, but not a full dashboard layout

## Known limitations

- no dedicated components for engines, projects, or executions
- no request cancellation, retry policy, or normalized cache
- no environment-based API configuration beyond in-file constants
- linting, type-checking, and unit-test harnesses exist, but coverage is still minimal
- no support statement beyond incubating proof-of-concept status

## Near-term architecture follow-ups

The remaining Phase 7 tasks build on this definition:

- replace empty lint/test scripts with real tooling
- introduce clearer API/state handling
- add component test coverage
- decide whether the UI is a supported product or a reference surface
