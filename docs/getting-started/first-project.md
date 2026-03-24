# Your First Architect Project

This guide walks through creating a real project from scratch using `architect.yml`.

## 1. Create the project file

In your project root (a git repository works best), create `architect.yml`:

```yaml
project:
  name: my-app
  description: "Example application with Architect"
```

## 2. Add inline tasks

Architect lets you define tasks directly in `architect.yml` using `command` (runs a shell command) or `script` (runs a script file).

```yaml
project:
  name: my-app

tasks:
  build:
    description: "Compile the application"
    phase: build
    command: ./compile.sh

  test:
    description: "Run unit tests"
    phase: test
    dependsOn:
      - build
    command: ./run-tests.sh

  lint:
    description: "Check code style"
    phase: lint
    command: ./lint.sh

  clean:
    description: "Remove build artefacts"
    command: rm -rf build/

  package:
    description: "Create release package"
    phase: release
    dependsOn:
      - test
    command: ./package.sh
```

## 3. Run tasks

```bash
# Run a single task
architect build

# Run all tasks in the test phase
architect --phase test

# List all tasks without running them
architect tasks

# Preview execution order (dry run)
architect plan
```

## 4. Phases and ordering

Phases define the standard lifecycle. Tasks registered to a phase run after all tasks in earlier phases complete.

Built-in phases (in order):

| Phase | Purpose |
|-------|---------|
| `init` | Project initialization, dependency downloads |
| `lint` | Code style and static analysis |
| `verify` | Contract and schema validation |
| `build` | Compilation and artefact generation |
| `test` | Unit and integration tests |
| `run` | Local execution / development server |
| `release` | Packaging and changelog generation |
| `publish` | Artefact upload and registry push |

## 5. Working with outputs and environments

```yaml
tasks:
  build:
    command: go build -o ./bin/app .
    env:
      CGO_ENABLED: "0"
      GOOS: linux
    outputs:
      - ./bin/app

  docker-build:
    dependsOn:
      - build
    command: docker build -t myapp:latest .
```

## 6. Viewing task history

```bash
# Last 20 executions
architect history

# Execution details for a specific run
architect history --id <execution-id>
```

## 7. Next steps

- [Adding plugins](adding-plugins.md) — extend with Git, GitHub, Gradle, and more
- [Inline tasks](inline-tasks.md) — compose and condition tasks
- [architect.yml reference](../reference/architect-yml.md) — full configuration schema
