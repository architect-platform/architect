# architect-intellij status

- Status: incubating
- Last reviewed: 2026-03-24
- Owner group: Product Surfaces

Current scope:
- IntelliJ plugin for architect.yml schema association and editor helpers.
- Classified as a thin reference integration, not a supported product.

Current limitations:
- Feature depth remains narrow even though basic plugin tests now exist.
- Not published to the JetBrains Marketplace and no release automation is in place.
- Still depends on the external `architect` CLI for task execution.

Graduation criteria:
- Add automated tests for schema and task UX features.
- Expand coverage beyond schema/marker behavior into end-to-end task execution UX.
- Add release/distribution workflow and explicit support commitments before promoting it beyond reference status.
