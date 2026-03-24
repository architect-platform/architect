# go-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

Go build, test, lint, and release workflow automation for Architect projects.

## Tasks

| Task ID | Description |
|---|---|
| `go-build` | Compile the Go module |
| `go-test` | Run Go test suite |
| `go-lint` | Run golangci-lint or equivalent linter |
| `go-release` | Build and package a release binary |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: go-architected
    repo: architect-platform/go-architected

go:
  module: github.com/my-org/my-app
  outputBinary: bin/my-app
  ldflags: "-s -w"
```

## Local Build and Test

```bash
cd plugins/go-architected/app
gradle build
gradle test
```

## Limitations

This plugin is at incubating tier. Tests and documentation are minimal. See [STATUS.md](STATUS.md) for graduation criteria.
