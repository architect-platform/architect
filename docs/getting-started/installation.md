# Getting Started with Architect

Go from zero to running your first task in under 15 minutes.

## Prerequisites

- **macOS, Linux, or Windows (WSL2)** — all platforms supported
- **Java 17+** — only required for JVM-based plugins; the CLI itself ships as a native binary
- **Git** — required for Git-based plugin sources

---

## 1. Install the CLI

### macOS (Homebrew)

```bash
brew tap architect-platform/tap
brew install architect
```

### Linux / macOS (binary install)

```bash
curl -sSfL https://github.com/architect-platform/architect/releases/latest/download/install.sh -o install.sh
sha256sum install.sh   # ← verify output matches the checksum published on the Releases page
bash install.sh
```

### Windows

Download the `.msi` installer from the [GitHub Releases page](https://github.com/architect-platform/architect/releases/latest) or use winget:

```powershell
winget install architect-platform.architect
```

### Verify installation

```bash
architect --version
# Architect CLI v1.x.x  |  API v2.x.x
```

---

## 2. Create your first project

Navigate to any project directory (or create a new one):

```bash
mkdir my-project && cd my-project
```

Create `architect.yml`:

```yaml
# architect.yml
project:
  name: my-project
  description: "My first Architect project"

tasks:
  hello:
    description: "Say hello"
    command: echo "Hello from Architect!"
```

---

## 3. Run your first task

```bash
architect hello
```

You will see:
```
⚙  Running hello...
Hello from Architect!
✅ hello — completed in 42ms
```

---

## 4. Add a plugin

Architect's power comes from plugins. Add the `git-architected` plugin:

```yaml
project:
  name: my-project
  description: "My first Architect project"

plugins:
  - name: git-architected
    type: github
    repo: architect-platform/git-architected

tasks:
  hello:
    description: "Say hello"
    command: echo "Hello from Architect!"
```

Now run:

```bash
architect tasks        # list all available tasks (yours + from plugins)
architect git-status   # from git-architected plugin
```

---

## 5. Run with the embedded engine (no daemon needed)

By default, `architect` auto-starts a background engine daemon. For CI or minimal environments, use embedded mode:

```bash
architect --embedded hello
```

No daemon, no HTTP, tasks run in-process. Ideal for CI/CD.

---

## 6. Next Steps

| Goal | Guide |
|------|-------|
| Configure tasks, phases, and workflows | [architect.yml reference](../reference/architect-yml.md) |
| See all CLI commands and flags | [CLI command reference](../reference/cli-commands.md) |
| Add your first plugin | [Adding plugins](adding-plugins.md) |
| Run tasks in a monorepo | [Concepts: Monorepo](../concepts/monorepo.md) |
| Write your own plugin | [Plugin authoring guide](../guides/authoring-plugins.md) |
| Integrate with CI/CD | [CI/CD integration guide](../guides/ci-cd-integration.md) |
