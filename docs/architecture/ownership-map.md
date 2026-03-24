# Ownership Map

> Maintained as part of Phase 1a repository decomposition work | Last updated: 2026-03-24T09:28:39Z

This document defines ownership metadata for the repository's bounded areas.
Because the repository does not yet maintain a `CODEOWNERS` file or published
team-handle map, ownership is recorded here as **stewardship groups**.

These groups describe who is responsible for architectural decisions, review
standards, and change coordination inside each bounded area. They are
conceptual ownership units, not GitHub permission objects.

---

## Stewardship Groups

| Group | Scope |
|---|---|
| Platform Runtime | Core execution stack, contracts, runtime internals |
| Product Surfaces | User-facing applications and IDE/editor integrations |
| Plugin Ecosystem | First-party plugin development and quality standards |
| SDK Ecosystem | Language SDKs and compatibility with the plugin protocol |
| Docs and Governance | Repository-wide docs, contribution policy, release/delivery metadata |

---

## Bounded Area Ownership

| Bounded area | Paths | Stewardship group | Primary responsibilities |
|---|---|---|---|
| Core platform/runtime | `architect-api/`, `architect-core/`, `architect-engine/`, `architect-cli/` | Platform Runtime | Contracts, runtime layering, execution flow, build/test trust, dependency boundaries |
| End-user products | `architect-cloud/`, `architect-vscode/`, `architect-intellij/` | Product Surfaces | Product behavior, UX, release readiness, integration quality, support-scope clarity |
| Official plugins | `plugins/` | Plugin Ecosystem | Plugin standards, contract tests, docs/examples, packaging consistency |
| SDKs | `sdk/` | SDK Ecosystem | Protocol compatibility, SDK ergonomics, cross-language examples, conformance testing |
| Documentation and governance | `docs/`, `README.md`, `CONTRIBUTING.md`, `PLAN.md`, `STATUS.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`, `LICENSE`, `mkdocs.yml`, `architect.yml`, `.github/`, `homebrew/` | Docs and Governance | Contributor guidance, repo narrative, policy maintenance, documentation publishing, delivery metadata |

---

## Usage Rules

- Every new top-level module must declare which stewardship group owns it.
- Cross-boundary changes should be reviewed from each affected stewardship
  group, not just by the code author.
- If the repository later adopts `CODEOWNERS` or named teams, that mechanism
  should refine this map rather than replace the bounded areas themselves.