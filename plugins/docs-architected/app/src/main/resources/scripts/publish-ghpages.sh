#!/bin/bash
set -e

OUTPUT_DIR=${1:-"site"}
BRANCH=${2:-"gh-pages"}

echo "Publishing documentation from $OUTPUT_DIR to $BRANCH branch..."

# Check if output directory exists
if [ ! -d "$OUTPUT_DIR" ]; then
    echo "Error: Output directory $OUTPUT_DIR not found"
    exit 1
fi

# Get current branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

# Create a temporary directory
TEMP_DIR=$(mktemp -d)

# Copy built documentation to temp directory
cp -r "$OUTPUT_DIR"/* "$TEMP_DIR/"

# Save current git state
git stash push -m "Stashing changes before gh-pages deployment"

# Check if gh-pages branch exists
if git show-ref --verify --quiet refs/heads/$BRANCH; then
    # Branch exists, checkout
    git checkout $BRANCH
else
    # Branch doesn't exist, create orphan branch
    git checkout --orphan $BRANCH
    git rm -rf .
fi

# Remove all files in the current directory (except .git)
find . -maxdepth 1 ! -name '.git' ! -name '.' -exec rm -rf {} +

# Copy files from temp directory
cp -r "$TEMP_DIR"/* .

# Create .nojekyll file to bypass Jekyll processing on GitHub Pages
touch .nojekyll

# Add and commit
git add .
git commit -m "docs: update documentation" || echo "No changes to commit"

# Push to remote
git push origin $BRANCH

# Return to original branch
git checkout $CURRENT_BRANCH

# Restore stashed changes if any
git stash pop || echo "No stashed changes to restore"

# Clean up
rm -rf "$TEMP_DIR"

echo "Documentation published successfully to $BRANCH branch"
