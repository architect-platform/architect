#!/usr/bin/env bash
# setup.sh — Quick-start script for new Architect Platform contributors.
# Usage: bash scripts/setup.sh

set -euo pipefail

echo "🏗️  Architect Platform — Developer Setup"
echo "========================================="
echo

# Check prerequisites
echo "Checking prerequisites..."

check_cmd() {
  if command -v "$1" &>/dev/null; then
    echo "  ✅ $1 found: $($1 --version 2>&1 | head -1)"
  else
    echo "  ❌ $1 not found — please install it"
    MISSING=1
  fi
}

MISSING=0
check_cmd java
check_cmd git
check_cmd python3

if [ "$MISSING" = "1" ]; then
  echo
  echo "Please install missing prerequisites and re-run this script."
  exit 1
fi

echo
echo "Checking Java version..."
JAVA_VERSION=$(java -version 2>&1 | head -1 | grep -oP '(?<=version ")[\d]+' || java -version 2>&1 | head -1 | sed 's/.*"\([0-9]*\).*/\1/')
if [ "$JAVA_VERSION" -lt 17 ] 2>/dev/null; then
  echo "  ⚠️  Java 17+ required (found Java $JAVA_VERSION)"
else
  echo "  ✅ Java $JAVA_VERSION"
fi

echo
echo "Building core modules..."
echo "  Building architect-api..."
(cd architect-api/api && ./gradlew build -q 2>/dev/null) && echo "  ✅ architect-api built" || echo "  ⚠️  architect-api build failed (may need GITHUB_TOKEN)"

echo "  Building architect-core..."
(cd architect-core/core && ./gradlew build -q 2>/dev/null) && echo "  ✅ architect-core built" || echo "  ⚠️  architect-core build failed"

echo
echo "Running convention checks..."
bash scripts/convention-check.sh 2>/dev/null && echo "  ✅ All conventions pass" || echo "  ⚠️  Some convention checks failed"

echo
echo "========================================="
echo "🎉 Setup complete!"
echo
echo "Useful commands:"
echo "  architect gradle-build     — Build all Kotlin modules"
echo "  architect gradle-test      — Run all tests"
echo "  architect scripts-ktlint   — Check code style"
echo "  architect docs-build       — Build documentation"
echo
echo "See docs/guides/architect-commands.md for all available commands."
