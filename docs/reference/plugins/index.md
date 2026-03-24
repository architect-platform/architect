# Plugin Reference

Architect ships a suite of official plugins. Each plugin is hosted as an independent GitHub repository and loaded via the standard plugin system.

## Available plugins

| Plugin | Source | Capabilities |
|--------|--------|-------------|
| [git-architected](git-architected.md) | `architect-platform/git-architected` | Git status, commit, push, pull, tag, branch |
| [github-architected](github-architected.md) | `architect-platform/github-architected` | GitHub releases, changelogs, PR automation |
| [gradle-architected](gradle-architected.md) | `architect-platform/gradle-architected` | Gradle build, test, lint, publish |
| [javascript-architected](javascript-architected.md) | `architect-platform/javascript-architected` | npm/yarn/pnpm install, build, test |
| [docker-architected](docker-architected.md) | `architect-platform/docker-architected` | Docker build, push, multi-arch, Compose |
| [kubernetes-architected](kubernetes-architected.md) | `architect-platform/kubernetes-architected` | kubectl apply, rollout, port-forward |
| [docs-architected](docs-architected.md) | `architect-platform/docs-architected` | MkDocs, Docusaurus, VuePress |
| [scripts-architected](scripts-architected.md) | `architect-platform/scripts-architected` | Custom shell scripts |
| [terraform-architected](terraform-architected.md) | `architect-platform/terraform-architected` | Terraform init, plan, apply, destroy |
| [maven-architected](maven-architected.md) | `architect-platform/maven-architected` | Maven compile, test, package, deploy |
| [go-architected](go-architected.md) | `architect-platform/go-architected` | Go build, test, lint, release |
| [rust-architected](rust-architected.md) | `architect-platform/rust-architected` | Cargo build, test, clippy, publish |
| [python-architected](python-architected.md) | `architect-platform/python-architected` | pip install, lint, test, build, publish |
| [nx-architected](nx-architected.md) | `architect-platform/nx-architected` | Nx affected, run-many, graph |
| [architecture-architected](architecture-architected.md) | `architect-platform/architecture-architected` | Arch-unit rules, dependency constraints |
| [pipelines-architected](pipelines-architected.md) | `architect-platform/pipelines-architected` | Composite pipeline definitions |

## Adding any plugin

```yaml
plugins:
  - name: <plugin-name>
    type: github
    repo: architect-platform/<plugin-name>
```

For version pinning, add `version: "x.y.z"`.

## Writing your own plugin

See the [Plugin Authoring Guide](../guides/authoring-plugins.md).
