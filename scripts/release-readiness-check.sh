#!/bin/bash
#
# release-readiness-check.sh — Validates a module meets its support-tier release criteria.
#
# Usage:
#   ./scripts/release-readiness-check.sh <module-path> [--tier <active|beta|incubating>]
#
# If --tier is not provided, the script reads it from <module-path>/STATUS.md.
#
# Exit codes:
#   0 — All checks pass
#   1 — One or more checks failed
#   2 — Usage / configuration error
#
# This script is designed to be run locally or in CI as a pre-release gate.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BOLD='\033[1m'
RESET='\033[0m'

# ── Parse arguments ─────────────────────────────────────────────────
MODULE_PATH=""
TIER=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tier) TIER="$2"; shift 2 ;;
    --help|-h) echo "Usage: $0 <module-path> [--tier <active|beta|incubating>]"; exit 0 ;;
    *) MODULE_PATH="$1"; shift ;;
  esac
done

if [[ -z "$MODULE_PATH" ]]; then
  echo "Error: module path required" >&2
  echo "Usage: $0 <module-path> [--tier <active|beta|incubating>]" >&2
  exit 2
fi

MODULE_DIR="$REPO_ROOT/$MODULE_PATH"
if [[ ! -d "$MODULE_DIR" ]]; then
  echo "Error: directory not found: $MODULE_DIR" >&2
  exit 2
fi

# ── Detect tier from STATUS.md if not provided ─────────────────────
if [[ -z "$TIER" ]]; then
  STATUS_FILE="$MODULE_DIR/STATUS.md"
  if [[ -f "$STATUS_FILE" ]]; then
    # Try **bold** format first, then plain format
    TIER=$(grep -i "status:" "$STATUS_FILE" | head -1 | sed 's/.*: *\*\*\([a-z]*\)\*\*/\1/' | tr '[:upper:]' '[:lower:]' || true)
    # If we got a full line back (no match), try plain format
    if [[ "$TIER" == *"status"* || "$TIER" == *"-"* || -z "$TIER" ]]; then
      TIER=$(grep -i "status:" "$STATUS_FILE" | head -1 | grep -oiE '(active|beta|incubating|placeholder|deprecated)' | head -1 | tr '[:upper:]' '[:lower:]' || true)
    fi
  fi
  if [[ -z "$TIER" ]]; then
    TIER=$(grep "$MODULE_PATH" "$REPO_ROOT/STATUS.md" | grep -oE '\*\*(active|beta|incubating|placeholder|deprecated)\*\*' | head -1 | tr -d '*' | tr '[:upper:]' '[:lower:]' || true)
  fi
  # Try partial path match (e.g. sdk/typescript matches sdk/typescript/plugin-sdk)
  if [[ -z "$TIER" ]]; then
    # Strip trailing path segments and try again
    PARENT_PATH=$(dirname "$MODULE_PATH")
    while [[ "$PARENT_PATH" != "." && -z "$TIER" ]]; do
      TIER=$(grep "$PARENT_PATH" "$REPO_ROOT/STATUS.md" | grep -oE '\*\*(active|beta|incubating|placeholder|deprecated)\*\*' | head -1 | tr -d '*' | tr '[:upper:]' '[:lower:]' || true)
      PARENT_PATH=$(dirname "$PARENT_PATH")
    done
  fi
  if [[ -z "$TIER" ]]; then
    echo "Warning: could not detect tier for $MODULE_PATH; defaulting to 'incubating'"
    TIER="incubating"
  fi
fi

echo -e "${BOLD}Release Readiness Check${RESET}"
echo -e "Module:  ${BOLD}$MODULE_PATH${RESET}"
echo -e "Tier:    ${BOLD}$TIER${RESET}"
echo "─────────────────────────────────────────────"

PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0

pass() { PASS_COUNT=$((PASS_COUNT + 1)); echo -e "  ${GREEN}✓${RESET} $1"; }
fail() { FAIL_COUNT=$((FAIL_COUNT + 1)); echo -e "  ${RED}✗${RESET} $1"; }
warn() { WARN_COUNT=$((WARN_COUNT + 1)); echo -e "  ${YELLOW}⚠${RESET} $1"; }

# ── CHECKS ──────────────────────────────────────────────────────────

# 1. README exists
echo ""
echo "Documentation:"
if [[ -f "$MODULE_DIR/README.md" ]]; then
  pass "README.md exists"
else
  fail "README.md missing"
fi

# 2. STATUS.md exists (all tiers)
if [[ -f "$MODULE_DIR/STATUS.md" ]]; then
  pass "STATUS.md exists"
else
  if [[ "$TIER" == "active" || "$TIER" == "beta" ]]; then
    fail "STATUS.md missing (required for $TIER)"
  else
    warn "STATUS.md missing"
  fi
fi

# 3. Version is declared
echo ""
echo "Versioning:"
VERSION=""
if [[ -f "$MODULE_DIR/build.gradle.kts" ]]; then
  # Match lines like: version = "1.0.0" or version = libs.versions.xxx.get()
  # Exclude plugin DSL lines like: kotlin("jvm") version "1.9.25"
  VERSION=$(grep -E '^\s*version\s*=' "$MODULE_DIR/build.gradle.kts" | head -1 | sed 's/.*version\s*=\s*//' | tr -d '"' | tr -d ' ' || true)
elif [[ -f "$MODULE_DIR/package.json" ]]; then
  VERSION=$(grep '"version"' "$MODULE_DIR/package.json" | head -1 | grep -oE '"[0-9][^"]*"' | tr -d '"' || true)
elif [[ -f "$MODULE_DIR/pyproject.toml" ]]; then
  VERSION=$(grep 'version' "$MODULE_DIR/pyproject.toml" | head -1 | grep -oE '"[0-9][^"]*"' | tr -d '"' || true)
fi

# Check nested build files if not found at top level
if [[ -z "$VERSION" ]]; then
  NESTED_BUILD=$(find "$MODULE_DIR" -maxdepth 2 -name "build.gradle.kts" 2>/dev/null | head -1 || true)
  if [[ -n "$NESTED_BUILD" ]]; then
    VERSION=$(grep -E '^\s*version\s*=' "$NESTED_BUILD" | head -1 | sed 's/.*version\s*=\s*//' | tr -d '"' | tr -d ' ' || true)
  fi
fi

if [[ -n "$VERSION" ]]; then
  pass "Version declared: $VERSION"
else
  if [[ "$TIER" == "active" || "$TIER" == "beta" ]]; then
    fail "No version found in build file"
  else
    warn "No version found (acceptable for $TIER)"
  fi
fi

# 4. Tests exist
echo ""
echo "Testing:"
HAS_TESTS=false
if find "$MODULE_DIR" -path "*/test/*" -name "*.kt" -o -path "*/test/*" -name "*.java" 2>/dev/null | head -1 | grep -q .; then
  HAS_TESTS=true
elif find "$MODULE_DIR" -path "*/tests/*" -name "*.py" 2>/dev/null | head -1 | grep -q .; then
  HAS_TESTS=true
elif find "$MODULE_DIR" -path "*/test/*" -name "*.ts" -o -path "*/test/*" -name "*.test.ts" 2>/dev/null | head -1 | grep -q .; then
  HAS_TESTS=true
elif find "$MODULE_DIR" -name "*_test.go" 2>/dev/null | head -1 | grep -q .; then
  HAS_TESTS=true
fi

if $HAS_TESTS; then
  pass "Test files present"
else
  if [[ "$TIER" == "active" || "$TIER" == "beta" ]]; then
    fail "No test files found (required for $TIER)"
  else
    warn "No test files found"
  fi
fi

# 5. Build file exists
echo ""
echo "Build:"
if [[ -f "$MODULE_DIR/build.gradle.kts" || -f "$MODULE_DIR/build.gradle" || -f "$MODULE_DIR/package.json" || -f "$MODULE_DIR/pyproject.toml" || -f "$MODULE_DIR/go.mod" || -f "$MODULE_DIR/Cargo.toml" ]]; then
  pass "Build configuration present"
else
  # Check one level deeper (e.g. plugins/git-architected/app)
  if find "$MODULE_DIR" -maxdepth 2 \( -name "build.gradle.kts" -o -name "build.gradle" -o -name "package.json" \) 2>/dev/null | head -1 | grep -q .; then
    pass "Build configuration present (nested)"
  else
    if [[ "$TIER" == "active" || "$TIER" == "beta" ]]; then
      fail "No build configuration found"
    else
      warn "No build configuration found"
    fi
  fi
fi

# 6. SPI registration (plugins only)
if [[ "$MODULE_PATH" == plugins/* ]]; then
  echo ""
  echo "Plugin Contract:"
  SPI_FILE=$(find "$MODULE_DIR" -path "*/META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin" 2>/dev/null | head -1)
  if [[ -n "$SPI_FILE" ]]; then
    pass "SPI service registration present"
  else
    if [[ "$TIER" == "active" ]]; then
      fail "SPI service registration missing (required for active plugins)"
    else
      warn "SPI service registration missing"
    fi
  fi
fi

# 7. Graduation criteria (incubating only)
if [[ "$TIER" == "incubating" ]]; then
  echo ""
  echo "Graduation:"
  if [[ -f "$MODULE_DIR/STATUS.md" ]] && grep -qi "graduation" "$MODULE_DIR/STATUS.md"; then
    pass "Graduation criteria documented"
  else
    warn "Graduation criteria not found in STATUS.md"
  fi
fi

# 8. No blocking TODOs (active/beta only)
if [[ "$TIER" == "active" || "$TIER" == "beta" ]]; then
  echo ""
  echo "Code Quality:"
  BLOCKING_TODOS=0
  if [[ -d "$MODULE_DIR/src" ]]; then
    BLOCKING_TODOS=$(grep -rn "TODO.*block\|FIXME.*release\|HACK.*remove" "$MODULE_DIR/src" 2>/dev/null | wc -l | tr -d ' ')
  fi
  if [[ "$BLOCKING_TODOS" -eq 0 ]]; then
    pass "No blocking TODO/FIXME/HACK comments"
  else
    warn "$BLOCKING_TODOS blocking TODO/FIXME/HACK comment(s) found"
  fi
fi

# ── Summary ─────────────────────────────────────────────────────────
echo ""
echo "─────────────────────────────────────────────"
TOTAL=$((PASS_COUNT + FAIL_COUNT + WARN_COUNT))
echo -e "Results: ${GREEN}$PASS_COUNT passed${RESET}, ${RED}$FAIL_COUNT failed${RESET}, ${YELLOW}$WARN_COUNT warnings${RESET} ($TOTAL checks)"
echo ""

if [[ $FAIL_COUNT -gt 0 ]]; then
  echo -e "${RED}${BOLD}RELEASE NOT READY${RESET} — $FAIL_COUNT check(s) failed"
  exit 1
else
  echo -e "${GREEN}${BOLD}RELEASE READY${RESET} — all required checks passed"
  exit 0
fi
