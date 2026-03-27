# CLI Command Reference

Complete reference for the `architect` CLI.

```
architect [OPTIONS] <COMMAND> [COMMAND OPTIONS] [ARGS...]
```

---

## Global options

These options are available for all commands:

| Flag | Default | Description |
|------|---------|-------------|
| `--config <path>` | `./architect.yml` | Path to the project configuration file |
| `--engine-url <url>` | `http://localhost:7070` | Architect Engine base URL |
| `--embedded` | `false` | Run tasks embedded (no engine daemon required) |
| `--verbose`, `-v` | `false` | Increase output verbosity |
| `--quiet`, `-q` | `false` | Suppress all output except errors |
| `--json` | `false` | Output results as JSON |
| `--no-color` | `false` | Disable ANSI colour output |
| `--help`, `-h` | | Print help text and exit |
| `--version` | | Print version and exit |

---

## Task execution

### `architect <task-id>`

Run a specific task by ID.

```bash
architect build
architect git-commit
architect test --args "--filter=UnitTests"
```

| Flag | Description |
|------|-------------|
| `--args "<args>"` | Pass additional arguments to the task (quoted string) |
| `--dry-run` | Resolve and print the execution plan without running tasks |
| `--force` | Rerun tasks even if outputs are cached |
| `--parallel <n>` | Maximum number of tasks to run concurrently (default: CPU count) |
| `--timeout <duration>` | Task timeout (e.g. `60s`, `5m`, `1h`) |

---

### `architect run <task-id> [task-id...]`

Explicitly run one or more tasks. Equivalent to `architect <task-id>` but supports multiple.

```bash
architect run lint build test
```

---

### `architect --phase <phase>`

Run all tasks belonging to a lifecycle phase.

```bash
architect --phase build
architect --phase test
```

---

## Project inspection

### `architect tasks`

List all available tasks (from plugins and inline definitions).

```bash
architect tasks
architect tasks --json
architect tasks --filter git
```

| Flag | Description |
|------|-------------|
| `--filter <pattern>` | Filter by task ID substring |
| `--json` | Machine-readable output |
| `--phase <phase>` | Show only tasks in a given phase |

---

### `architect plan [task-id]`

Preview the execution plan without running anything. Shows task order, dependencies, and skipped tasks.

```bash
architect plan
architect plan build
```

---

### `architect graph`

Open a browser-based interactive dependency graph of all tasks.

```bash
architect graph
architect graph --output graph.html   # save to file
```

| Flag | Description |
|------|-------------|
| `--output <path>` | Write HTML graph to file instead of opening browser |
| `--format dot` | Output GraphViz DOT format |

---

### `architect info`

Print project information: resolved path, registered plugins, task count, engine status.

```bash
architect info
```

---

## History and caching

### `architect history`

Show a log of recent task executions.

```bash
architect history
architect history --limit 50
architect history --id <execution-id>
```

| Flag | Description |
|------|-------------|
| `--limit <n>` | Number of entries to show (default: 20) |
| `--id <id>` | Show full details for one execution |
| `--json` | Machine-readable output |
| `--task <task-id>` | Filter by task ID |

---

### `architect cache`

Manage the local result cache.

```bash
architect cache status
architect cache clear
architect cache clear --task build   # clear only one task's cache
```

| Subcommand | Description |
|-----------|-------------|
| `status` | Show cache size and hit statistics |
| `clear` | Delete all cached results |
| `clear --task <id>` | Delete cached results for one task |

---

## Monorepo

### `architect affected [task-id]`

Run a task only for projects affected by changes since the base ref.

```bash
architect affected test
architect affected build --base main
architect affected test --base HEAD~1
```

| Flag | Default | Description |
|------|---------|-------------|
| `--base <ref>` | `HEAD~1` or `main` | Git ref to compare against |
| `--uncommitted` | `false` | Include uncommitted changes |
| `--all` | `false` | Run against all projects (skip affected check) |

---

## File watching

### `architect watch [task-id]`

Watch source files and re-run tasks on change.

```bash
architect watch test
architect watch --all   # re-run all tasks on any change
```

| Flag | Description |
|------|-------------|
| `--all` | Re-run all tasks (not just the specified one) |
| `--debounce <ms>` | Debounce delay in milliseconds (default: 200) |
| `--pattern <glob>` | Watch files matching glob (default: from plugin / task config) |

---

## Engine management

### `architect engine start`

Start the Architect Engine daemon.

```bash
architect engine start
architect engine start --port 7070
architect engine start --background
```

| Flag | Default | Description |
|------|---------|-------------|
| `--port <port>` | `7070` | HTTP port for the Engine |
| `--background` | `false` | Detach and run as a daemon |
| `--log-file <path>` | | Write logs to file |

---

### `architect engine stop`

Stop the running Engine daemon.

```bash
architect engine stop
```

---

### `architect engine status`

Check whether the Engine is running and healthy.

```bash
architect engine status
```

---

### `architect engine logs`

Stream Engine logs.

```bash
architect engine logs
architect engine logs --follow
architect engine logs --tail 100
```

---

## Plugin management

### `architect plugin list`

List all installed / configured plugins for the current project.

```bash
architect plugin list
```

---

### `architect plugin install <name>`

Add a plugin to `architect.yml` and download it.

```bash
architect plugin install git-architected
architect plugin install git-architected --version 1.2.0
```

---

### `architect plugin search <query>`

Search the Architect plugin registry.

```bash
architect plugin search docker
```

---

### `architect plugin docs <name>`

Open the documentation page for an installed plugin.

```bash
architect plugin docs gradle-architected
```

---

### `architect plugin validate <path>`

Validate a local plugin JAR against the plugin API contract.

```bash
architect plugin validate ./my-plugin/build/libs/my-plugin.jar
```

---

### `architect plugin test <path>`

Run plugin graduation-oriented checks against a local plugin JAR.

```bash
architect plugin test ./my-plugin/build/libs/my-plugin.jar
architect plugin test ./my-plugin/build/libs/my-plugin.jar --json
```

Checks include:

- SPI wiring and plugin discovery
- plugin contract verification
- `configSchema()` shape validation
- task registration checks

---

### `architect plugin create <name>`

Scaffold a new plugin project.

```bash
architect plugin create my-plugin
architect plugin create my-plugin --template kotlin
```

| Flag | Default | Description |
|------|---------|-------------|
| `--template <lang>` | `kotlin` | Language scaffold: `kotlin`, `python`, `go`, `typescript` |
| `--output <dir>` | `./<name>` | Output directory |

---

## Project initialisation

### `architect init`

Interactively generate an `architect.yml` for the current project.

```bash
architect init
architect init --preset gradle-kotlin
```

| Flag | Description |
|------|-------------|
| `--preset <name>` | Skip interactive prompts and apply a preset |
| `--force` | Overwrite an existing `architect.yml` |

Available presets: `gradle-kotlin`, `gradle-java`, `npm`, `python`, `go`, `rust`, `maven`, `docker`.

---

### `architect validate`

Validate `architect.yml` against the JSON Schema.

```bash
architect validate
architect validate --config path/to/architect.yml
```

---

## Exit codes

| Code | Meaning |
|------|---------|
| `0` | All tasks succeeded |
| `1` | One or more tasks failed |
| `2` | Configuration error |
| `3` | Engine not reachable |
| `4` | Task not found |
| `130` | Cancelled by user (Ctrl-C) |
