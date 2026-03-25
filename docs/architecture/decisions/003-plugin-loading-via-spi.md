# ADR-003: Use Java SPI for plugin discovery

| Field    | Value      |
|----------|------------|
| **Status** | Accepted |
| **Date**   | 2025-01-15 |
| **Authors** | Architect Platform contributors |

## Context

The Architect engine must discover and load plugins at runtime without
hard-coding references to specific implementations. Plugins are distributed as
JAR files and downloaded from GitHub Releases.

We evaluated three discovery mechanisms:

1. **Annotation scanning** (e.g. Reflections library, classpath scanning) —
   finds annotated classes at startup. Requires scanning the entire classpath,
   which is slow and fragile with custom classloaders.
2. **Explicit registration** — a configuration file lists plugin class names.
   Simple but requires users to maintain an extra manifest.
3. **Java Service Provider Interface (SPI)** — plugins declare their provider
   class in `META-INF/services/<interface>`. The JDK `ServiceLoader` handles
   discovery automatically.

## Decision

Plugins implement `io.github.architectplatform.api.core.plugins.ArchitectPlugin`
and register via the standard SPI mechanism by placing a file at
`META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin`
inside their JAR.

At runtime, the engine uses a `URLClassLoader` to add plugin JARs (downloaded
from GitHub Releases) to the classpath, then calls `ServiceLoader.load()` to
discover all registered implementations.

## Consequences

### Positive

- SPI is a standard JDK mechanism since Java 6, well-documented, and understood
  by most JVM developers. No additional library dependencies.
- No reflection or annotation processing at runtime — discovery is a simple
  file lookup inside the JAR.
- Works with any JVM language (Kotlin, Java, Scala, Groovy) as long as the
  META-INF entry is present.
- Build tools (Gradle, Maven) have first-class support for generating SPI
  descriptors.

### Negative

- All plugin JARs share a single `URLClassLoader`, so there is no per-plugin
  classloader isolation. A plugin that bundles a conflicting version of a
  transitive dependency can break other plugins or the engine itself.
- Version conflicts between plugins manifest as silent runtime errors (wrong
  method signatures, `ClassCastException`) — classic "JAR hell" — with no
  built-in detection or resolution.
- Adding classloader isolation in the future (e.g. OSGi, Java modules, or
  custom classloaders) would be a significant architectural change.
