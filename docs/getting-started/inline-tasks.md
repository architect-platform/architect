# Inline Tasks

Define custom tasks directly in `architect.yml` without writing a plugin.

---

## Basic command task

```yaml
tasks:
  greet:
    description: "Print a greeting"
    command: echo "Hello, World!"
```

Run it: `architect greet`

---

## Shell script task

For multi-line logic, use `script` instead of `command`:

```yaml
tasks:
  generate-version:
    description: "Derive version from git"
    script: |
      VERSION=$(git describe --tags --always --dirty)
      echo "Building version: ${VERSION}"
      echo "${VERSION}" > .version
```

---

## Task with dependencies

`dependsOn` lists task IDs that must succeed before this task runs:

```yaml
tasks:
  compile:
    phase: build
    command: ./gradlew compileKotlin

  jar:
    phase: build
    dependsOn:
      - compile
    command: ./gradlew jar

  test:
    phase: test
    dependsOn:
      - jar
    command: ./gradlew test
```

---

## Task phases

Assign a task to a lifecycle phase so it fits into the standard workflow:

```yaml
tasks:
  lint:
    phase: lint
    command: ktlint src/**/*.kt

  build:
    phase: build
    command: ./gradlew build

  docker-build:
    phase: publish
    dependsOn:
      - build
    command: docker build -t myapp:latest .
```

All tasks in `lint` run before `build`; all tasks in `build` run before `publish`.

---

## Environment variables

```yaml
tasks:
  build:
    command: npm run build
    env:
      NODE_ENV: production
      API_URL: https://api.example.com
```

Or inherit from the current shell with `envFrom`:

```yaml
tasks:
  deploy:
    command: ./scripts/deploy.sh
    envFrom:
      - DEPLOY_TOKEN
      - CLUSTER_URL
```

---

## Composite (parent) tasks

Group related tasks under a single parent task. Running the parent runs all children:

```yaml
tasks:
  ci:
    description: "Full CI pipeline"
    tasks:
      - lint
      - build
      - test
      - docker-build

  lint:
    command: ./gradlew ktlintCheck

  build:
    command: ./gradlew build

  test:
    command: ./gradlew test

  docker-build:
    command: docker build .
```

Running `architect ci` executes all four child tasks in dependency order.

---

## Conditional execution

Skip a task based on environment:

```yaml
tasks:
  publish:
    description: "Publish to registry"
    condition: "env.CI == 'true'"
    command: ./gradlew publish
```

Conditions are evaluated as simple expressions. Supported variables:
- `env.<NAME>` — environment variable
- `file.exists('<path>')` — file existence check
- `git.branch` — current git branch name

---

## Arguments and parameters

Accept arguments passed from the command line:

```yaml
tasks:
  create-release:
    description: "Create a release tag"
    args:
      - name: version
        description: "Semantic version (e.g., 1.2.3)"
        required: true
    command: git tag -a v${args.version} -m "Release v${args.version}"
```

Usage:
```bash
architect create-release --version 1.2.3
```

---

## Working directory

Run a task in a specific sub-directory:

```yaml
tasks:
  frontend-build:
    description: "Build the React app"
    workdir: frontend/
    command: npm run build
```

---

## Timeout

Fail a task if it takes longer than expected:

```yaml
tasks:
  integration-test:
    command: ./run-integration-tests.sh
    timeout: 300s   # fail after 5 minutes
```

---

## Next steps

- [architect.yml reference](../reference/architect-yml.md) — full schema of all task properties
- [CLI command reference](../reference/cli-commands.md) — flags for `architect run`, `plan`, `watch`
- [Plugin authoring guide](../guides/authoring-plugins.md) — when inline tasks aren't enough
