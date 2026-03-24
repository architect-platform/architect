# architect-vscode status

- Status: incubating
- Last reviewed: 2026-03-24
- Owner group: Product Surfaces

Current scope:
- VS Code extension for architect.yml schema assistance and task interactions.
- Classified as a thin reference integration, not a supported product.

Current limitations:
- Coverage is still narrow even though parser-model regression tests now exist.
- Not distributed through the VS Code Marketplace and no release automation is in place.
- Still depends on the external `architect` CLI for task execution.

Graduation criteria:
- Add automated tests for schema association and task execution paths.
- Expand coverage beyond parsing into command/task-tree integration behavior.
- Add release/distribution workflow and explicit support commitments before promoting it beyond reference status.
