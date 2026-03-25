#!/usr/bin/env bash
# Convention checker for Architect Platform engineering standards.
# Runs static checks that enforce logging, error-handling, and security conventions.
# Exit 0 = pass, 1 = violations found, 2 = usage error.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VIOLATIONS=0
WARNINGS=0

red()   { printf '\033[0;31m%s\033[0m\n' "$*"; }
yellow(){ printf '\033[0;33m%s\033[0m\n' "$*"; }
green() { printf '\033[0;32m%s\033[0m\n' "$*"; }
info()  { printf '  %s\n' "$*"; }

fail() { VIOLATIONS=$((VIOLATIONS + 1)); red "  FAIL: $*"; }
warn() { WARNINGS=$((WARNINGS + 1)); yellow "  WARN: $*"; }
pass() { green "  PASS: $*"; }

# ─── 1. No System.out/System.err in core/engine Kotlin source ────────────────
echo "=== Checking: No System.out/System.err in core/engine ==="
for module in architect-core/core architect-engine/engine; do
  src="$REPO_ROOT/$module/src/main/kotlin"
  [ -d "$src" ] || continue
  hits=$(grep -rn 'System\.\(out\|err\)\.\(print\|write\)' "$src" --include='*.kt' || true)
  if [ -n "$hits" ]; then
    fail "$module uses System.out/System.err (use SLF4J instead):"
    echo "$hits" | head -5 | while IFS= read -r line; do info "$line"; done
  else
    pass "$module: no System.out/System.err usage"
  fi
done

# ─── 2. No raw Exception throws in core/engine ──────────────────────────────
echo "=== Checking: No raw Exception/RuntimeException throws ==="
for module in architect-core/core architect-engine/engine architect-api/api; do
  src="$REPO_ROOT/$module/src/main/kotlin"
  [ -d "$src" ] || continue
  hits=$(grep -rn 'throw\s\+\(Exception\|RuntimeException\|IllegalArgumentException\)(' "$src" --include='*.kt' || true)
  if [ -n "$hits" ]; then
    warn "$module throws raw exceptions (prefer ArchitectException subtypes):"
    echo "$hits" | head -5 | while IFS= read -r line; do info "$line"; done
  else
    pass "$module: no raw exception throws"
  fi
done

# ─── 3. No silent exception swallowing (empty catch blocks) ──────────────────
echo "=== Checking: No silent exception swallowing ==="
for module in architect-core/core architect-engine/engine architect-cli/cli; do
  src="$REPO_ROOT/$module/src/main/kotlin"
  [ -d "$src" ] || continue
  hits=$(grep -rn 'catch.*{[[:space:]]*}' "$src" --include='*.kt' || true)
  if [ -n "$hits" ]; then
    warn "$module has empty catch blocks:"
    echo "$hits" | head -5 | while IFS= read -r line; do info "$line"; done
  else
    pass "$module: no empty catch blocks"
  fi
done

# ─── 4. No HTTP (non-HTTPS) URLs in download/fetch code ─────────────────────
echo "=== Checking: No plain HTTP URLs in source ==="
hits=$(grep -rn '"http://' "$REPO_ROOT/architect-core" "$REPO_ROOT/architect-engine" "$REPO_ROOT/architect-cli" --include='*.kt' | grep -v 'localhost\|127\.0\.0\.1\|0\.0\.0\.0\|//test\|Test\.kt\|json-schema\.org\|xml\.org\|w3\.org' || true)
if [ -n "$hits" ]; then
  fail "Plain HTTP URLs found (use HTTPS):"
  echo "$hits" | head -5 | while IFS= read -r line; do info "$line"; done
else
  pass "No plain HTTP URLs in production code"
fi

# ─── 5. No hardcoded secrets/tokens in source ───────────────────────────────
echo "=== Checking: No hardcoded secrets in source ==="
hits=$(grep -rni '\(password\|secret\|token\|api_key\|apikey\)\s*=\s*"[^"${}]\{8,\}"' "$REPO_ROOT/architect-core" "$REPO_ROOT/architect-engine" "$REPO_ROOT/architect-cli" "$REPO_ROOT/plugins" --include='*.kt' | grep -v 'Test\.kt\|test/' || true)
if [ -n "$hits" ]; then
  fail "Possible hardcoded secrets found:"
  echo "$hits" | head -5 | while IFS= read -r line; do info "$line"; done
else
  pass "No hardcoded secrets detected"
fi

# ─── 6. Plugins use try/catch in execute() ───────────────────────────────────
echo "=== Checking: Plugin execute() methods have error handling ==="
plugin_fails=0
for plugin_dir in "$REPO_ROOT"/plugins/*/app/src/main/kotlin; do
  [ -d "$plugin_dir" ] || continue
  plugin_name=$(echo "$plugin_dir" | sed 's|.*/plugins/\([^/]*\)/.*|\1|')
  # Find files with execute() that don't contain try/catch
  for kt_file in $(grep -rl 'override fun execute' "$plugin_dir" --include='*.kt' || true); do
    if ! grep -q 'try\s*{' "$kt_file"; then
      warn "$plugin_name: $(basename "$kt_file") has execute() without try/catch"
      plugin_fails=$((plugin_fails + 1))
    fi
  done
done
if [ "$plugin_fails" -eq 0 ]; then
  pass "All plugin execute() methods have error handling"
fi

# ─── 7. SPI registration exists for all plugins ─────────────────────────────
echo "=== Checking: SPI registration for plugins ==="
for plugin_dir in "$REPO_ROOT"/plugins/*/app; do
  [ -d "$plugin_dir" ] || continue
  plugin_name=$(echo "$plugin_dir" | sed 's|.*/plugins/\([^/]*\)/.*|\1|')
  spi="$plugin_dir/src/main/resources/META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin"
  if [ ! -f "$spi" ]; then
    warn "$plugin_name: missing SPI registration file"
  elif [ ! -s "$spi" ]; then
    warn "$plugin_name: SPI file is empty"
  fi
done
pass "SPI registration check complete"

# ─── 8. No wildcard imports in production code ──────────────────────────────
echo "=== Checking: No wildcard imports ==="
for module in architect-api/api architect-core/core architect-engine/engine; do
  src="$REPO_ROOT/$module/src/main/kotlin"
  [ -d "$src" ] || continue
  hits=$(grep -rn '^import .*\.\*$' "$src" --include='*.kt' || true)
  if [ -n "$hits" ]; then
    warn "$module has wildcard imports:"
    echo "$hits" | head -3 | while IFS= read -r line; do info "$line"; done
  else
    pass "$module: no wildcard imports"
  fi
done

# ─── Summary ─────────────────────────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════"
if [ "$VIOLATIONS" -gt 0 ]; then
  red "RESULT: $VIOLATIONS violation(s), $WARNINGS warning(s)"
  exit 1
elif [ "$WARNINGS" -gt 0 ]; then
  yellow "RESULT: 0 violations, $WARNINGS warning(s)"
  exit 0
else
  green "RESULT: All checks passed"
  exit 0
fi
