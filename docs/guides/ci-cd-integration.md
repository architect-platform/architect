# CI/CD Integration Guide

This guide shows how to integrate Architect into popular CI/CD platforms.

---

## GitHub Actions

### Minimal example

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: temurin

      - name: Setup Architect
        uses: ./.github/actions/setup-architect
        with:
          install-engine: 'true'

      - name: Build and test
        run: |
          architect --phase BUILD
          architect --phase TEST
```

### Full pipeline with artefact publishing

```yaml
name: Release Pipeline

on:
  push:
    tags: ['v*']

jobs:
  release:
    runs-on: ubuntu-latest
    permissions:
      contents: write
      packages: write

    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0   # full history for changelog

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: temurin

      - name: Setup Architect
        uses: ./.github/actions/setup-architect
        with:
          install-engine: 'true'

      - name: Run CI pipeline
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          architect --phase BUILD
          architect --phase TEST
          architect --phase RELEASE
          architect --phase PUBLISH
```

### Using embedded mode (no daemon)

For ephemeral runners where you don't want an engine daemon:

```yaml
- name: Build
  run: architect --embedded --phase BUILD
```

---

## GitLab CI

```yaml
# .gitlab-ci.yml
image: eclipse-temurin:17-jdk

stages:
  - build
  - test
  - release

variables:
  ARCHITECT_ENGINE_EMBEDDED: "true"

before_script:
  - curl -sSfL https://github.com/architect-platform/architect/releases/latest/download/install.sh -o install.sh
  - echo "$INSTALL_SHA256  install.sh" | sha256sum -c
  - bash install.sh
  - export PATH="$HOME/.architect/bin:$PATH"

build:
  stage: build
  script:
    - architect --phase BUILD
  artifacts:
    paths:
      - build/

test:
  stage: test
  script:
    - architect --phase TEST
  coverage: '/Total.*?(\d+%)/'

release:
  stage: release
  only:
    - tags
  script:
    - architect --phase RELEASE
    - architect --phase PUBLISH
```

---

## Jenkins

```groovy
// Jenkinsfile
pipeline {
    agent {
        docker {
            image 'eclipse-temurin:17-jdk'
        }
    }

    environment {
        GITHUB_TOKEN = credentials('github-token')
    }

    stages {
        stage('Setup') {
            steps {
                sh '''
                    curl -sSfL https://github.com/architect-platform/architect/releases/latest/download/install.sh -o install.sh
                    bash install.sh
                    export PATH="$HOME/.architect/bin:$PATH"
                    architect --version
                '''
            }
        }

        stage('Build') {
            steps {
                sh 'architect --embedded --phase BUILD'
            }
        }

        stage('Test') {
            steps {
                sh 'architect --embedded --phase TEST'
            }
            post {
                always {
                    junit 'build/test-results/**/*.xml'
                }
            }
        }

        stage('Release') {
            when {
                tag 'v*'
            }
            steps {
                sh 'architect --embedded --phase RELEASE'
                sh 'architect --embedded --phase PUBLISH'
            }
        }
    }
}
```

---

## CircleCI

```yaml
# .circleci/config.yml
version: 2.1

jobs:
  build-test:
    docker:
      - image: cimg/openjdk:17.0

    steps:
      - checkout

      - run:
          name: Install Architect
          command: |
            curl -sSfL https://github.com/architect-platform/architect/releases/latest/download/install.sh -o install.sh
            bash install.sh
            echo 'export PATH="$HOME/.architect/bin:$PATH"' >> "$BASH_ENV"

      - run:
          name: Build
          command: architect --embedded --phase BUILD

      - run:
          name: Test
          command: architect --embedded --phase TEST

      - store_test_results:
          path: build/test-results

workflows:
  ci:
    jobs:
      - build-test
```

---

## Best practices

### Use embedded mode in CI

Prefer `--embedded` in CI/CD to avoid port conflicts and lifecycle issues:

```bash
architect --embedded --phase BUILD
```

### Pin the Architect version

Avoid nondeterministic builds by pinning to a specific version:

```yaml
# In .github/actions/setup-architect
with:
  version: "1.2.3"
```

Or set `ARCHITECT_VERSION` in your CI environment.

### Pass secrets via environment variables

Never put tokens in `architect.yml`. Reference them by environment variable:

```yaml
github-architected:
  token: ${GITHUB_TOKEN}    # resolved from env at runtime
```

### Cache plugin downloads

Architect downloads plugin JARs on first use. Cache `~/.architect/plugins/` to speed up subsequent runs:

```yaml
# GitHub Actions
- name: Cache Architect plugins
  uses: actions/cache@v4
  with:
    path: ~/.architect/plugins
    key: architect-plugins-${{ hashFiles('architect.yml') }}
```

### Stream execution logs with SSE

For long-running tasks, stream logs directly from the Engine:

```
GET http://localhost:7070/api/executions/{executionId}/events
Accept: text/event-stream
```

### Fail fast on security issues

The `dependency-vulnerability-scan` workflow runs Trivy and fails on CRITICAL findings. Run it on every push:

```yaml
on:
  push:
  schedule:
    - cron: '0 8 * * 1'   # weekly Monday 08:00 UTC
```

See `.github/workflows/dependency-vulnerability-scan.yml`.
