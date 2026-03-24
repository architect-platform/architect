# syntax=docker/dockerfile:1
# =============================================================================
# Architect Platform — Docker image
# Bundles both the CLI and the Engine in a single lightweight image.
# =============================================================================

# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy only the files needed to resolve Gradle dependencies first, for better layer caching.
COPY architect-api/api/build.gradle.kts       architect-api/api/build.gradle.kts
COPY architect-api/api/settings.gradle.kts    architect-api/api/settings.gradle.kts
COPY architect-api/api/gradle.properties      architect-api/api/gradle.properties
COPY architect-api/api/gradle/                architect-api/api/gradle/
COPY architect-api/api/gradlew                architect-api/api/gradlew

COPY architect-engine/engine/build.gradle.kts     architect-engine/engine/build.gradle.kts
COPY architect-engine/engine/settings.gradle.kts  architect-engine/engine/settings.gradle.kts
COPY architect-engine/engine/gradle.properties    architect-engine/engine/gradle.properties
COPY architect-engine/engine/gradle/              architect-engine/engine/gradle/
COPY architect-engine/engine/gradlew              architect-engine/engine/gradlew

COPY architect-cli/cli/build.gradle.kts      architect-cli/cli/build.gradle.kts
COPY architect-cli/cli/settings.gradle.kts   architect-cli/cli/settings.gradle.kts
COPY architect-cli/cli/gradle.properties     architect-cli/cli/gradle.properties
COPY architect-cli/cli/gradle/               architect-cli/cli/gradle/
COPY architect-cli/cli/gradlew               architect-cli/cli/gradlew

# Copy full source
COPY architect-api/     architect-api/
COPY architect-engine/  architect-engine/
COPY architect-cli/     architect-cli/
COPY architect-core/    architect-core/

# Build CLI and Engine JARs
ARG GITHUB_USER
ARG REGISTRY_TOKEN
ENV GITHUB_USER=${GITHUB_USER}
ENV REGISTRY_TOKEN=${REGISTRY_TOKEN}

RUN chmod +x architect-engine/engine/gradlew architect-cli/cli/gradlew && \
    cd architect-engine/engine && ./gradlew shadowJar --no-daemon --info -x test && \
    cd /build/architect-cli/cli && ./gradlew shadowJar --no-daemon --info -x test

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="Architect"
LABEL org.opencontainers.image.description="Plugin-based task execution framework for developer workflows"
LABEL org.opencontainers.image.url="https://github.com/architect-platform/architect"
LABEL org.opencontainers.image.source="https://github.com/architect-platform/architect"
LABEL org.opencontainers.image.licenses="MIT"

# Create a non-root user
RUN addgroup -S architect && adduser -S architect -G architect

WORKDIR /app

# Copy built JARs from builder stage
COPY --from=builder /build/architect-engine/engine/build/libs/*-all.jar  /app/architect-engine.jar
COPY --from=builder /build/architect-cli/cli/build/libs/*-all.jar        /app/architect-cli.jar

# Launcher script: runs Engine in background then delegates to CLI
RUN printf '#!/bin/sh\nset -e\n# Start engine in background if not already running\nif ! curl -sf http://localhost:8080/health >/dev/null 2>&1; then\n  java -jar /app/architect-engine.jar &\n  i=0\n  until curl -sf http://localhost:8080/health >/dev/null 2>&1 || [ $i -ge 30 ]; do\n    sleep 1; i=$((i+1))\n  done\nfi\nexec java -jar /app/architect-cli.jar "$@"\n' > /app/architect && \
    chmod +x /app/architect && \
    chown architect:architect /app/architect

# Add architect to PATH
ENV PATH="/app:${PATH}"

# Default data directory accessible to non-root user
RUN mkdir -p /data && chown architect:architect /data
VOLUME ["/data"]

USER architect
WORKDIR /data

ENTRYPOINT ["/app/architect"]
CMD ["--help"]
