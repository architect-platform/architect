# Adding Plugins

Plugins extend Architect with pre-built task suites for popular tools and workflows.

## Plugin types

| Type | Description | `type` value |
|------|-------------|--------------|
| **GitHub** | Loaded from a GitHub repository's latest release | `github` |
| **Registry** | Installed from the Architect plugin registry | `registry` |
| **Local** | Loaded from a local path on disk | `local` |

---

## Adding a GitHub plugin

```yaml
plugins:
  - name: git-architected
    type: github
    repo: architect-platform/git-architected
```

Architect will download the latest compatible release of the plugin the first time you run any command. To pin to a version:

```yaml
plugins:
  - name: git-architected
    type: github
    repo: architect-platform/git-architected
    version: "1.2.3"
```

---

## Plugin configuration

Most plugins expose a top-level configuration block. The block key matches the plugin `name`:

```yaml
plugins:
  - name: git-architected
    type: github
    repo: architect-platform/git-architected

  - name: github-architected
    type: github
    repo: architect-platform/github-architected

git-architected:
  remote: origin
  defaultBranch: main
  signCommits: false

github-architected:
  owner: my-org
  repo: my-app
  releasePrefix: "v"
  changelog:
    enabled: true
    sections:
      - "feat"
      - "fix"
      - "perf"
```

---

## Official plugins

| Plugin | Capabilities |
|--------|-------------|
| `git-architected` | status, commit, push, pull, tag, cherry-pick, branch management |
| `github-architected` | releases, changelogs, PR creation, Actions trigger |
| `gradle-architected` | build, test, lint, publish, dependency check |
| `javascript-architected` | npm/yarn/pnpm install, build, test, lint, publish |
| `docker-architected` | build, push, multi-arch builds, compose up/down |
| `kubernetes-architected` | apply, delete, rollout, port-forward, logs |
| `docs-architected` | MkDocs, Docusaurus, VuePress build and serve |
| `scripts-architected` | custom shell scripts with lifecycle hooks |
| `terraform-architected` | init, plan, apply, destroy, workspace management |
| `maven-architected` | compile, test, package, deploy |
| `go-architected` | build, test, lint, mod tidy, release |
| `rust-architected` | build, test, clippy, fmt, publish |
| `python-architected` | install, lint, test, build, publish |
| `security-architected` | security scanning, dependency audits, and SBOM generation with severity thresholds |
| `quality-architected` | linting, static analysis, reporting, and explicit quality-gate enforcement |
| `release-architected` | semantic release preparation, changelog generation, and coordinated npm/Docker/GitHub publishing |
| `testing-architected` | unit, integration, e2e, and coverage orchestration with threshold enforcement |
| `nx-architected` | affected, run-many, graph, cache |
| `architecture-architected` | arch-unit validation, dependency constraints |
| `pipelines-architected` | composite pipeline definitions |

Full reference: [Plugin pages](../reference/plugins/index.md)

---

## Local plugins (development)

Point to a plugin on your local filesystem while developing it:

```yaml
plugins:
  - name: my-local-plugin
    type: local
    path: ../my-plugin/app/build/libs/my-plugin.jar
```

---

## Listing available tasks after adding plugins

```bash
architect tasks
```

Output:
```
⚙  Tasks from git-architected:
  git-status        Show working tree status
  git-commit        Stage and commit changes
  git-push          Push commits to remote
  git-tag           Create and push a tag
  ...

⚙  Tasks from github-architected:
  github-release    Create a GitHub release
  github-changelog  Generate changelog
  ...

⚙  Your tasks:
  build             Compile the application
  test              Run unit tests
```

---

## Troubleshooting

**Plugin not found**: Ensure the `repo` value is a valid public GitHub repository and a release exists for your platform.

**Tasks not appearing**: Run `architect tasks --verbose` to see plugin loading errors.

**Version conflicts**: Use a `version` pin to avoid breaking changes from plugin updates.
