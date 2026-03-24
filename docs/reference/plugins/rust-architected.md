# rust-architected

Rust project integration for Architect.

**Source**: `architect-platform/rust-architected`

## Installation

```yaml
plugins:
  - name: rust-architected
    type: github
    repo: architect-platform/rust-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `cargo-build` | `BUILD` | Compile with `cargo build` |
| `cargo-test` | `TEST` | Run the test suite with `cargo test` |
| `cargo-lint` | `LINT` | Run Clippy and `cargo fmt --check` |
| `cargo-publish` | `PUBLISH` | Publish to crates.io |

## Configuration

Configuration key: `rust`

```yaml
rust-architected:
  profile: release        # debug | release
  target: x86_64-unknown-linux-musl
  features:
    - tokio-rt
    - tls
  testArgs:
    - --all-features
  publishArgs:
    - --allow-dirty
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `profile` | `string` | `debug` | Build profile: `debug` or `release` |
| `target` | `string` | — | Cargo target triple (e.g. `x86_64-unknown-linux-musl`) |
| `features` | `string[]` | `[]` | Cargo features to enable |
| `testArgs` | `string[]` | `[]` | Extra arguments for `cargo test` |
| `publishArgs` | `string[]` | `[]` | Extra arguments for `cargo publish` |

## Usage examples

```bash
# Debug build
architect cargo-build

# Release build
# Set profile: release in config, then:
architect cargo-build

# Run tests
architect cargo-test

# Clippy + format check
architect cargo-lint

# Publish to crates.io
architect cargo-publish
```

## Security notes

`profile` and `target` values are validated to contain only alphanumeric characters, hyphens, underscores, and dots. Arbitrary input is rejected.
