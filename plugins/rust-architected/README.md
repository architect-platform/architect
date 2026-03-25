# rust-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

## Overview

rust-architected integrates Rust/Cargo into the Architect task lifecycle, providing tasks for building, testing, and linting Rust projects. It enables teams to manage Rust development through standardized Architect commands.

## Tasks

| Task ID | Description |
|---|---|
| `cargo-build` | Build the Rust project with Cargo |
| `cargo-test` | Run the Cargo test suite |
| `cargo-lint` | Run Cargo Clippy with warning enforcement |
| `cargo-publish` | Publish the crate with Cargo |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: rust-architected
    repo: architect-platform/rust-architected

rust:
  profile: release
  features:
    - serde
  target: x86_64-unknown-linux-gnu
  enabled: true
```

## Local Build and Test

```bash
cd plugins/rust-architected/app
gradle build
gradle test
```

## Limitations

This plugin is at incubating tier. Tests and documentation are minimal, and the current implementation still needs broader validation against representative Cargo workflows. See [STATUS.md](STATUS.md) for graduation criteria.
