# javascript-architected

> **Status**: Active — see [STATUS.md](STATUS.md)

## Overview

`javascript-architected` integrates JavaScript/Node.js workflows into Architect phases with first-class support for:

- npm
- yarn classic and yarn berry
- pnpm
- bun

It includes monorepo workspace detection, lockfile validation, dependency audit, version bumping, and publish tasks.

## Tasks

| Task ID | Phase | Description |
|---|---|---|
| `javascript-install` | `INIT` | Install dependencies using the configured package manager |
| `javascript-workspace-check` | `VERIFY` | Detect workspace type (single package, workspaces, pnpm, nx, turbo, rush) |
| `javascript-lockfile-check` | `VERIFY` | Validate lockfile presence for configured package manager |
| `javascript-audit` | `VERIFY` | Run dependency security audit (`npm audit`, `pnpm audit`, etc.) |
| `javascript-build` | `BUILD` | Run build script |
| `javascript-test` | `TEST` | Run tests |
| `javascript-lint` | `TEST` | Run linter |
| `javascript-dev` | `RUN` | Run dev command |
| `javascript-version` | `RELEASE` | Bump package version |
| `javascript-publish` | `PUBLISH` | Publish package to registry |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: javascript-architected
    repo: architect-platform/architect

javascript:
  packageManager: npm        # npm | yarn | pnpm | bun
  yarnMode: auto             # auto | classic | berry (used when packageManager=yarn)
  workingDirectory: .        # directory containing package.json
  publishAccess: public      # public | restricted
  defaultVersionBump: patch  # major | minor | patch | prerelease
```

## Package manager behavior

- npm: `install`, `audit`, `version`, `publish` use native npm commands
- yarn classic: standard yarn scripts and `yarn install --frozen-lockfile` for CI workflows
- yarn berry: immutable install mode (`yarn install --immutable`)
- pnpm: native pnpm install/test/build/audit/publish/version commands
- bun: native bun install/test/run/audit/version/publish commands

## Monorepo and workspace checks

`javascript-workspace-check` detects common workspace markers:

- `pnpm-workspace.yaml`
- `nx.json`
- `turbo.json`
- `rush.json`
- `workspaces` field in `package.json`

Result data is emitted as task result metadata (`workspaceType`) for downstream consumers.

## Local build and test

```bash
cd plugins/javascript-architected/app
./gradlew clean test
./gradlew build
```

## Recommended validation before release

```bash
# From repository root
architect plugin validate plugins/javascript-architected/app/build/libs/javascript-architected-*.jar
architect plugin test plugins/javascript-architected/app/build/libs/javascript-architected-*.jar
```

## Notes

- `javascript-lockfile-check` fails when required lockfile is missing.
- `javascript-version` accepts an optional bump argument (`major`, `minor`, `patch`, `prerelease`).
- `javascript-publish` uses `publishAccess` for package managers that support access flags.
