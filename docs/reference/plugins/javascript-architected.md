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
| `javascript-install` | `INIT` | Install dependencies (`npm install` / `yarn` / `pnpm install`) |
| `javascript-build` | `BUILD` | Build the project (`npm run build`) |
| `javascript-test` | `TEST` | Run tests (`npm test`) |
| `javascript-lint` | `TEST` | Run linter (`npm run lint`) |
| `javascript-dev` | `RUN` | Start the development server (`npm run dev`) |

## Configuration

Configuration key: `javascript`

```yaml
javascript-architected:
  packageManager: npm    # npm | yarn | pnpm
  workingDirectory: .    # path to directory containing package.json
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `packageManager` | `string` | `npm` | Package manager: `npm`, `yarn`, or `pnpm` |
| `workingDirectory` | `string` | `.` | Directory containing `package.json` |

## Usage examples

```bash
# Install dependencies
architect javascript-install

# Build and test
architect --phase BUILD
architect --phase TEST

# Start dev server
architect javascript-dev
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
