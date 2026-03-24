# Repository Testing Standard

This guide defines the minimum test matrix expected for Architect modules by
module type and support tier. It exists because the repository does not have a
single root build or test entry point, but it still needs a consistent quality
bar across libraries, services, plugins, SDKs, frontend surfaces, and IDE
integrations.

## Scope

This standard applies to:

- Core libraries such as `architect-api` and `architect-core`
- Services such as `architect-engine` and `architect-cloud/backend`
- Official plugins under `plugins/`
- SDKs under `sdk/`
- Frontend products such as `architect-cloud/ui`
- IDE/editor integrations such as `architect-vscode` and `architect-intellij`

Support tiers still matter:

- `active`: must meet the full applicable matrix and keep it green
- `beta`: same expectation as active for release-blocking checks, but may still
  have bounded feature gaps
- `incubating`: must publish current gaps and at least meet the minimum matrix
  described for its module type before it can be treated as supportable

## Minimum Matrix by Module Type

| Module type | Required minimum matrix | Examples in this repo |
|---|---|---|
| Libraries | Unit tests for public/domain logic; at least one negative-path or validation-path test; API/contract-focused regression tests when the module defines shared contracts | `architect-api/api`, `architect-core/core` |
| Services | Unit/service tests for orchestration logic; HTTP or boundary tests for externally visible endpoints; configuration/bootstrap smoke test; one error-path test per major integration boundary | `architect-engine/engine`, `architect-cloud/backend` |
| Official plugins | Positive-path task execution tests; at least one error/validation-path test; plugin contract test using shared plugin contract tooling; example/config stays in sync with tests | `plugins/*/app` |
| SDKs | Protocol serialization/deserialization coverage; compatibility tests against engine/plugin-protocol expectations; example or fixture-backed smoke test proving task registration/execution flow | `sdk/typescript`, `sdk/python`, `sdk/go` |
| Frontend products | Lint + typecheck where applicable; component tests for key UI states; API/state handling tests for fetch/error flows; production build smoke test | `architect-cloud/ui` |
| IDE/editor integrations | Parser/model tests for config discovery; behavior tests for commands, schema association, or line-marker/task-tree surfaces; build/packaging smoke test supported by the module toolchain | `architect-vscode`, `architect-intellij` |

## Required Test Types

### Libraries

Minimum expectations:

- Cover exported/public behavior, not only internal helpers
- Include at least one failure or validation case for each major public API
- Add regression tests whenever fixing shared contracts, escaping, parsing, or
  dependency resolution behavior

Suggested command pattern:

```bash
cd <library-module> && ./gradlew test
```

### Services

Minimum expectations:

- Exercise orchestration/services directly
- Cover externally visible transport boundaries (HTTP, SSE, DTO mapping, etc.)
- Include one startup/configuration smoke path that proves the service boots
- Cover at least one unhappy path for each external dependency class (plugin
  loading, network boundary, process launch, persistence, remote call)

Suggested command pattern:

```bash
cd <service-module> && ./gradlew test
```

### Official Plugins

Minimum expectations:

- Positive-path task execution tests
- At least one error-path or validation-path test
- Plugin contract suite coverage
- Example config and documented task IDs stay aligned with tests

See also: [Official Plugin Standard](plugin-standard.md).

Suggested command pattern:

```bash
cd plugins/<plugin>/app && ./gradlew test
```

### SDKs

Minimum expectations:

- Verify protocol message encoding/decoding against stable fixtures
- Verify task registration and execution semantics expected by APP v1
- Include compatibility coverage against engine-facing protocol expectations
- Keep example plugins or fixtures executable under test, not documentation-only

### Frontend Products

Minimum expectations:

- Lint and typecheck as part of the local validation command
- Component tests for visible loading, success, and error states
- API/state tests for polling, fetch failures, or data normalization
- Production build must succeed locally

Suggested command pattern:

```bash
cd <frontend-module> && npm run lint && npm test && npm run build
```

### IDE / Editor Integrations

Minimum expectations:

- Config parsing/model tests where YAML or schema discovery is involved
- Behavior tests for the editor affordances users actually see (commands, task
  tree/task list, schema association, gutter markers, run configuration basics)
- Module-local build/test or verification command must succeed
- If the integration is only a thin reference integration, the tests may stay
  narrow, but the limited support tier must be documented explicitly

Suggested command pattern:

```bash
cd architect-vscode && npm test
cd architect-intellij && gradle test
```

## Release and Review Expectations

- Changes to an `active` or `beta` module should normally leave its full local
  matrix green before merge.
- Changes to an `incubating` module must at least run the matrix that the module
  actually claims to support today and document any remaining gaps in that
  module's `STATUS.md`.
- When a new module is added, its README and STATUS document must declare:
  - module type
  - support tier
  - local test command
  - missing matrix items if it is incubating

## Repository Reality Notes

- There is no supported root test orchestrator today; run commands from each
  module directory.
- This guide defines the minimum matrix only. Individual modules may and should
  exceed it where risk justifies more coverage.
- Later Phase 8 tasks should convert this standard into compatibility suites,
  smoke tests, and release-readiness checks rather than leaving it as prose only.
