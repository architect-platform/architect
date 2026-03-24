# Migrating from Nx

This guide helps teams using Nx monorepo tooling adopt Architect alongside or instead of Nx.

---

## Architect vs Nx

Architect and Nx are complementary, not competing tools. Architect is a *universal task runner* with a rich plugin system for any tech stack. Nx excels at JavaScript/TypeScript monorepo management with task caching and affected-task computation.

| Feature | Nx | Architect |
|---------|-----|-----------|
| Affected task detection | ✅ | ✅ (`architect affected`) |
| Task caching | ✅ | ✅ |
| Multiple language support | Partial (JS-first) | ✅ (Go, Rust, Python, Java, …) |
| Plugin ecosystem | ✅ (JS plugins) | ✅ (JVM + process plugins) |
| CI/CD pipeline sync | ❌ | ✅ (`github-architected`) |
| Engine API / SSE streaming | ❌ | ✅ |

---

## Option A: Use Architect alongside Nx

The simplest migration: keep Nx for JS/TS graph calculations and use Architect tasks for non-JS workflows (Docker, Kubernetes, docs, releases).

```yaml
# architect.yml
plugins:
  - name: nx-architected
    type: github
    repo: architect-platform/nx-architected

  - name: docker-architected
    type: github
    repo: architect-platform/docker-architected

  - name: github-architected
    type: github
    repo: architect-platform/github-architected

nx-architected:
  targets:
    - name: build
      phase: BUILD
      affected: true
    - name: test
      phase: TEST
      affected: true
    - name: lint
      phase: LINT
      affected: true
```

Running `architect --phase BUILD` will invoke `nx affected --target=build` for JS packages, while Gradle/Docker/Kubernetes tasks run in the same pipeline.

---

## Option B: Replace `project.json` targets with `architect.yml` tasks

For teams wanting to consolidate entirely on Architect, map each Nx target to an inline task or plugin.

**Nx `project.json`:**
```json
{
  "targets": {
    "build": {
      "executor": "@nx/webpack:webpack",
      "options": { "outputPath": "dist/my-app" }
    },
    "test": {
      "executor": "@nx/jest:jest"
    },
    "lint": {
      "executor": "@nx/eslint:lint"
    }
  }
}
```

**Architect equivalent (`architect.yml`):**
```yaml
plugins:
  - name: javascript-architected
    type: github
    repo: architect-platform/javascript-architected

javascript-architected:
  packageManager: pnpm
  workingDirectory: apps/my-app

tasks:
  my-app-build:
    run: pnpm nx build my-app
    phase: BUILD

  my-app-test:
    run: pnpm nx test my-app
    phase: TEST
    depends: [my-app-build]

  my-app-lint:
    run: pnpm nx lint my-app
    phase: LINT
```

---

## Migrating `nx.json` global configuration

**`nx.json`:**
```json
{
  "targetDefaults": {
    "build": { "dependsOn": ["^build"] },
    "test": { "dependsOn": ["build"] }
  },
  "affected": {
    "defaultBase": "main"
  }
}
```

**Architect equivalent:**
```yaml
project:
  name: my-monorepo
  affected:
    always-include:
      - shared-libs

nx-architected:
  targets:
    - name: build
      phase: BUILD
      affected: true
    - name: test
      phase: TEST
      affected: true
```

Phase ordering handles `build` before `test` automatically.

---

## Affected task detection

Nx and Architect both support running only tasks for changed projects.

**Nx:**
```bash
nx affected --target=test --base=main
```

**Architect:**
```bash
architect affected test --base main
```

The `nx-architected` plugin delegates to `nx affected` internally when `affected: true` is set, giving you Nx's graph analysis with Architect's orchestration.

---

## Recommended migration path

1. **Add Architect** alongside Nx: `architect init --preset npm`.
2. **Add `nx-architected`** plugin and configure targets — verify existing Nx tasks still work.
3. **Add non-JS plugins** (Docker, Kubernetes, docs, GitHub releases) for workflows Nx doesn't cover.
4. **Gradually replace** `project.json` targets with Architect inline tasks, starting with the simplest ones.
5. **Remove Nx** once all targets are migrated (optional — the two tools coexist well).

---

## Running Architect and Nx in the same repository

Both tools can coexist. Use `architect tasks` to see all Architect tasks and `nx show projects` to see Nx projects. There is no conflict.

```bash
# Everything through Architect (delegates to Nx where needed)
architect --phase BUILD

# Direct Nx access still works
nx run my-app:build
```
