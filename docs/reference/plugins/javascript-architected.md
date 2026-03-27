# javascript-architected

JavaScript/Node.js project integration for Architect.

**Source**: `architect-platform/javascript-architected`

## Installation

```yaml
plugins:
  - name: javascript-architected
    type: github
    repo: architect-platform/javascript-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `javascript-install` | `INIT` | Install dependencies (`npm install` / `yarn install` / `pnpm install` / `bun install`) |
| `javascript-workspace-check` | `VERIFY` | Detect monorepo/workspace type from common workspace markers |
| `javascript-lockfile-check` | `VERIFY` | Validate lockfile presence for the configured package manager |
| `javascript-audit` | `VERIFY` | Run dependency security audit (`npm audit`, `pnpm audit`, `yarn npm audit`, `bun audit`) |
| `javascript-build` | `BUILD` | Build the project (`npm run build`) |
| `javascript-test` | `TEST` | Run tests (`npm test`) |
| `javascript-lint` | `TEST` | Run linter (`npm run lint`) |
| `javascript-dev` | `RUN` | Start the development server (`npm run dev`) |
| `javascript-version` | `RELEASE` | Bump package version (`npm version patch`, etc.) |
| `javascript-publish` | `PUBLISH` | Publish package to registry |

## Configuration

Configuration key: `javascript`

```yaml
javascript-architected:
  packageManager: npm        # npm | yarn | pnpm | bun
  yarnMode: auto             # auto | classic | berry
  workingDirectory: .        # path to directory containing package.json
  publishAccess: public      # public | restricted
  defaultVersionBump: patch  # major | minor | patch | prerelease
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `packageManager` | `string` | `npm` | Package manager: `npm`, `yarn`, `pnpm`, or `bun` |
| `yarnMode` | `string` | `auto` | Yarn mode (`classic` or `berry`), auto-detected by default |
| `workingDirectory` | `string` | `.` | Directory containing `package.json` |
| `publishAccess` | `string` | `public` | Access mode for publish flows where supported |
| `defaultVersionBump` | `string` | `patch` | Default bump type for `javascript-version` |

## Usage examples

```bash
# Install dependencies
architect javascript-install

# Verify, build, and test
architect javascript-workspace-check
architect javascript-lockfile-check
architect javascript-audit
architect --phase BUILD
architect --phase TEST

# Start dev server
architect javascript-dev

# Release and publish
architect javascript-version -- minor
architect javascript-publish
```

## Multi-package mono-repo

For monorepos with multiple JS packages, use multiple plugin declarations:

```yaml
plugins:
  - name: javascript-architected
    type: github
    repo: architect-platform/javascript-architected

tasks:
  frontend-install:
    run: pnpm install
    workdir: frontend/

  backend-install:
    run: pnpm install
    workdir: backend/
```
