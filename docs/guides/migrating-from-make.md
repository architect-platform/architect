# Migrating from Make

This guide helps teams migrate from Makefile-based workflows to Architect.

---

## Why migrate?

| Feature | Make | Architect |
|---------|------|-----------|
| Cross-platform | ❌ (POSIX only) | ✅ |
| Plugin ecosystem | ❌ | ✅ |
| Task graph visualisation | ❌ | ✅ |
| Result caching | ❌ | ✅ |
| Real-time streaming | ❌ | ✅ |
| IDE integration | ❌ | ✅ (IntelliJ, VS Code) |
| Structured phases | ❌ | ✅ |

---

## Quick translation guide

### Simple command targets

**Make:**
```makefile
build:
	go build -o bin/app .

test:
	go test ./...

clean:
	rm -rf bin/
```

**Architect (`architect.yml`):**
```yaml
tasks:
  build:
    run: go build -o bin/app .
    phase: BUILD

  test:
    run: go test ./...
    phase: TEST

  clean:
    run: rm -rf bin/
```

---

### Dependencies between targets

**Make:**
```makefile
test: build
	go test ./...

docker-build: test
	docker build -t myapp .
```

**Architect:**
```yaml
tasks:
  build:
    run: go build -o bin/app .
    phase: BUILD

  test:
    run: go test ./...
    phase: TEST
    depends:
      - build

  docker-build:
    run: docker build -t myapp .
    phase: PUBLISH
    depends:
      - test
```

Or simply assign tasks to phases — Architect runs BUILD before TEST before PUBLISH automatically.

---

### Variables / environment

**Make:**
```makefile
VERSION ?= dev
REGISTRY ?= docker.io/myorg

build:
	go build -ldflags "-X main.version=$(VERSION)" -o bin/app .
```

**Architect:**
```yaml
tasks:
  build:
    run: go build -ldflags "-X main.version=${VERSION}" -o bin/app .
    env:
      VERSION: dev     # default; override with env var at runtime
```

Run with override: `VERSION=1.2.3 architect build`

---

### Phony / utility targets

**Make:**
```makefile
.PHONY: lint fmt tidy

lint:
	golangci-lint run

fmt:
	gofmt -w .

tidy:
	go mod tidy
```

**Architect:**
```yaml
tasks:
  lint:
    run: golangci-lint run
    phase: LINT

  fmt:
    run: gofmt -w .

  tidy:
    run: go mod tidy
```

---

### Conditional targets

**Make:**
```makefile
release:
ifdef CI
	./scripts/release.sh
else
	@echo "Not in CI, skipping release"
endif
```

**Architect:**
```yaml
tasks:
  release:
    run: ./scripts/release.sh
    condition: "env.CI == 'true'"
    phase: RELEASE
```

---

## Recommended migration path

1. **Map each `make` target to an Architect task** in `architect.yml`.
2. **Assign phases** — most targets map naturally: `build` → BUILD, `test` → TEST, `deploy` → PUBLISH.
3. **Replace implicit Make dependencies** with explicit `depends` or rely on phase ordering.
4. **Replace `$(VARIABLE)` syntax** with `${VARIABLE}` (shell-compatible).
5. **Add a plugin** for your tool stack (Go, Gradle, npm, etc.) to get additional tasks for free.
6. **Run** `architect tasks` to verify all tasks are registered.
7. **Remove the Makefile** once the team has migrated.

---

## Keeping both during transition

You can keep the `Makefile` during transition and delegate to Architect:

```makefile
build:
	architect --embedded --phase BUILD

test:
	architect --embedded --phase TEST

.PHONY: build test
```
