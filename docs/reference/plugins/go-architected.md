# go-architected

Go (Golang) project integration for Architect.

**Source**: `architect-platform/go-architected`

## Installation

```yaml
plugins:
  - name: go-architected
    type: github
    repo: architect-platform/go-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `go-build` | `BUILD` | Compile the Go binary |
| `go-test` | `TEST` | Run the test suite |
| `go-lint` | `LINT` | Run golangci-lint |
| `go-release` | `RELEASE` | Build release binaries (multi-platform) |

## Configuration

Configuration key: `go`

```yaml
go-architected:
  modulePath: .
  outputBinary: bin/app
  ldflags: "-s -w -X main.version=${VERSION}"
  platforms:
    - GOOS: linux
      GOARCH: amd64
    - GOOS: darwin
      GOARCH: arm64
    - GOOS: windows
      GOARCH: amd64
  testArgs: []
  lintConfig: .golangci.yml
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `modulePath` | `string` | `.` | Path to Go module root |
| `outputBinary` | `string` | `./app` | Output binary path for `go-build` |
| `ldflags` | `string` | — | Linker flags passed to `go build` |
| `platforms` | `object[]` | `[{linux,amd64}]` | GOOS/GOARCH combinations for `go-release` |
| `testArgs` | `string[]` | `[]` | Extra arguments for `go test` |
| `lintConfig` | `string` | `.golangci.yml` | golangci-lint config file |

## Usage examples

```bash
# Build binary
architect go-build

# Run tests
architect go-test

# Lint
architect go-lint

# Build release binaries for all platforms
architect go-release
```
