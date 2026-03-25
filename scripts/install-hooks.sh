#!/usr/bin/env bash
# install-hooks.sh — Install git pre-commit hooks for the Architect Platform.
# Usage: bash scripts/install-hooks.sh

set -euo pipefail

HOOK_DIR="$(git rev-parse --git-dir)/hooks"
HOOK_FILE="$HOOK_DIR/pre-commit"

echo "Installing pre-commit hook..."

cat > "$HOOK_FILE" << 'HOOK'
#!/usr/bin/env bash
# Architect Platform pre-commit hook
# Installed by scripts/install-hooks.sh

set -euo pipefail

echo "Running pre-commit checks..."

# Convention check (fast)
if [ -f scripts/convention-check.sh ]; then
  echo "  Checking conventions..."
  bash scripts/convention-check.sh > /dev/null 2>&1 || {
    echo "  ❌ Convention check failed. Run: bash scripts/convention-check.sh"
    exit 1
  }
  echo "  ✅ Conventions OK"
fi

# Check for generated workflow drift
if git diff --cached --name-only | grep -q '.github/workflows/'; then
  echo "  Checking workflow drift..."
  python3 .github/scripts/gen_workflows.py > /dev/null 2>&1
  if ! git diff --exit-code .github/workflows/ > /dev/null 2>&1; then
    echo "  ❌ Generated workflows have drifted. Run: python3 .github/scripts/gen_workflows.py"
    git checkout -- .github/workflows/
    exit 1
  fi
  echo "  ✅ Workflows OK"
fi

echo "All pre-commit checks passed ✅"
HOOK

chmod +x "$HOOK_FILE"
echo "✅ Pre-commit hook installed at $HOOK_FILE"
echo
echo "The hook will run on every commit and check:"
echo "  - Convention compliance (scripts/convention-check.sh)"
echo "  - Generated workflow drift (.github/scripts/gen_workflows.py)"
