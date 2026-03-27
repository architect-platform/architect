# Plugin Reference

Architect ships a suite of official plugins. Each plugin is hosted as an independent GitHub repository and loaded via the standard plugin system.

## Available plugins

| Plugin | Tier | Source | Capabilities |
|--------|------|--------|-------------|
| [git-architected](git-architected.md) | active | `architect-platform/git-architected` | Git status, commit, push, pull, tag, branch |
| [github-architected](github-architected.md) | active | `architect-platform/github-architected` | GitHub releases, changelogs, PR automation |
| [gradle-architected](gradle-architected.md) | active | `architect-platform/gradle-architected` | Gradle build, test, lint, publish |
| [javascript-architected](javascript-architected.md) | incubating | `architect-platform/javascript-architected` | npm/yarn/pnpm install, build, test |
| [docker-architected](docker-architected.md) | incubating | `architect-platform/docker-architected` | Docker build, push, multi-arch, Compose |
| [kubernetes-architected](kubernetes-architected.md) | incubating | `architect-platform/kubernetes-architected` | kubectl apply, rollout, port-forward |
| [docs-architected](docs-architected.md) | active | `architect-platform/docs-architected` | MkDocs, Docusaurus, VuePress |
| [scripts-architected](scripts-architected.md) | active | `architect-platform/scripts-architected` | Custom shell scripts |
| [terraform-architected](terraform-architected.md) | incubating | `architect-platform/terraform-architected` | Terraform init, plan, apply, destroy |
| [maven-architected](maven-architected.md) | incubating | `architect-platform/maven-architected` | Maven compile, test, package, deploy |
| [go-architected](go-architected.md) | incubating | `architect-platform/go-architected` | Go build, test, lint, release |
| [rust-architected](rust-architected.md) | incubating | `architect-platform/rust-architected` | Cargo build, test, clippy, publish |
| [python-architected](python-architected.md) | incubating | `architect-platform/python-architected` | pip install, lint, test, build, publish |
| [security-architected](security-architected.md) | incubating | `architect-platform/security-architected` | Security scanning, dependency audits, and CycloneDX/SPDX SBOM generation |
| [quality-architected](quality-architected.md) | incubating | `architect-platform/quality-architected` | Linting, static analysis, reporting, and explicit quality-gate checks |
| [testing-architected](testing-architected.md) | incubating | `architect-platform/testing-architected` | Unified unit/integration/e2e/coverage tasks across Gradle, pytest, Jest, Vitest, Go, and Cargo |
| [nx-architected](nx-architected.md) | incubating | `architect-platform/nx-architected` | Nx affected, run-many, graph |
| [architecture-architected](architecture-architected.md) | incubating | `architect-platform/architecture-architected` | Arch-unit rules, dependency constraints |
| [pipelines-architected](pipelines-architected.md) | active | `architect-platform/pipelines-architected` | Composite pipeline definitions |

Incubating plugins remain part of the official set, but they have not yet met the full plugin standard defined in the [Official Plugin Standard](../../guides/plugin-standard.md).

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
