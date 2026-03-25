# Architect Cloud UI

## Overview

The Architect Cloud UI is a React + Vite single-page application that provides
a monitoring surface for the Architect Cloud platform. It polls the Cloud
Backend REST API and renders engine, project, and execution data in a
lightweight dashboard.

> **Status: incubating** — the UI is a proof of concept. It does not yet
> provide a complete dashboard experience, routing, or WebSocket support.

## Current Capabilities

- Boots a React application with Vite (port 3000)
- Polls the backend REST API through a typed fetch layer (`cloudApi.ts`)
- Maintains dashboard state in a typed React hook (`useCloudDashboard.ts`)
- Renders summary cards for engines, projects, and executions
- Provides an error banner with a retry action

## Technology Stack

| Area | Technology |
|---|---|
| Framework | React 18 |
| Bundler | Vite 5 |
| Language | TypeScript / JSX |
| Linting | ESLint (repo-local config) |
| Testing | Vitest (minimal coverage) |

## Development

```bash
cd architect-cloud/ui
npm install
npm run dev          # Vite dev server on http://localhost:3000
```

## Build & Preview

```bash
npm run build        # production build → dist/
npm run preview      # serve the production build locally
```

## Current Status & Known Gaps

- Dashboard rendering is limited to summary cards
- No routing or dedicated view structure
- Backend WebSocket events are not consumed
- Test coverage is minimal (hook-backed summary flow and small components)
- No integration or end-to-end tests

See [ARCHITECTURE.md](../ARCHITECTURE.md) for the frontend architecture and
[STATUS.md](../STATUS.md) for support level and graduation criteria.

## Further Reading

- [UI README](../README.md) — detailed scope and local development guide
- [Cloud README](../../README.md) — umbrella module overview
