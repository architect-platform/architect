# JavaScript Architected Plugin

Architect plugin for integrating JavaScript/Node.js projects into the Architect workflow system.

## Overview

This plugin enables JavaScript projects (using npm, yarn, or pnpm) to be managed and executed within Architect's structured workflow phases.

## Features

- **Multi-package manager support**: npm, yarn, and pnpm
- **Standard JavaScript workflows**: install, build, test, lint, dev
- **Configurable working directory**: Support for monorepos and nested projects
- **Automatic command translation**: Handles differences between package managers

## Configuration

Add to your `architect.yml`:

```yaml
project:
  name: my-javascript-project
  description: "My JavaScript application"

plugins:
  - name: javascript-architected
    repo: architect-platform/architect
    context:
      packageManager: "npm"  # or "yarn" or "pnpm"
      workingDirectory: "."
```

### Context Options

- `packageManager` (default: `"npm"`): The package manager to use (`npm`, `yarn`, or `pnpm`)
- `workingDirectory` (default: `"."`): The directory containing `package.json`

## Available Tasks

The plugin registers the following tasks:

### `javascript-install` (INIT phase)
Installs project dependencies.
- npm: `npm install`
- yarn: `yarn`
- pnpm: `pnpm install`

### `javascript-build` (BUILD phase)
Builds the project.
- npm: `npm run build`
- yarn: `yarn build`
- pnpm: `pnpm build`

### `javascript-test` (TEST phase)
Runs the test suite.
- npm: `npm test`
- yarn: `yarn test`
- pnpm: `pnpm test`

### `javascript-lint` (TEST phase)
Runs the linter.
- npm: `npm run lint`
- yarn: `yarn lint`
- pnpm: `pnpm lint`

### `javascript-dev` (RUN phase)
Starts the development server.
- npm: `npm run dev`
- yarn: `yarn dev`
- pnpm: `pnpm dev`

## Examples

### React/Vite Project

```yaml
plugins:
  - name: javascript-architected
    repo: architect-platform/architect
    context:
      packageManager: "npm"
```

### Monorepo with Yarn

```yaml
plugins:
  - name: javascript-architected
    repo: architect-platform/architect
    context:
      packageManager: "yarn"
      workingDirectory: "packages/frontend"
```

### pnpm Project

```yaml
plugins:
  - name: javascript-architected
    repo: architect-platform/architect
    context:
      packageManager: "pnpm"
```

## Usage

Execute tasks via the Architect CLI or Engine:

```bash
# Install dependencies
architect run javascript-install

# Build the project
architect run javascript-build

# Run tests
architect run javascript-test

# Start dev server
architect run javascript-dev
```

## Requirements

- Node.js (version depends on your project)
- Chosen package manager installed (npm, yarn, or pnpm)
- `package.json` in the working directory

## Integration with CI/CD

The plugin works seamlessly with Architect's workflow phases:

1. **INIT**: `javascript-install` installs dependencies
2. **BUILD**: `javascript-build` compiles/bundles the application
3. **TEST**: `javascript-test` and `javascript-lint` validate the code
4. **RUN**: `javascript-dev` starts the development server

## Troubleshooting

### Package manager not found
Ensure the specified package manager is installed and available in PATH.

### Working directory issues
Verify that `workingDirectory` points to a directory containing `package.json`.

### Custom scripts
The plugin expects standard npm scripts (`build`, `test`, `lint`, `dev`). Ensure your `package.json` defines these scripts.

## License

MIT License - see LICENSE file for details.
