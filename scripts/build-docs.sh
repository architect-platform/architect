#!/bin/bash
# Build documentation for the Architect Platform
# This script sets up the documentation structure and builds the site

set -e

echo "Setting up documentation structure..."

# Create docs subdirectories
mkdir -p docs/architect-api
mkdir -p docs/architect-cli
mkdir -p docs/architect-engine
mkdir -p docs/architect-cloud
mkdir -p docs/plugins

# Core components to copy
COMPONENTS=("architect-api" "architect-cli" "architect-engine" "architect-cloud")

# Copy component documentation
echo "Copying component documentation..."
for component in "${COMPONENTS[@]}"; do
    if [ -d "$component/docs" ]; then
        cp -r "$component/docs"/* "docs/$component/" 2>/dev/null || true
        echo "  ✓ Copied $component documentation"
    fi
done

# Plugins to copy
PLUGINS=("docs-architected" "git-architected" "github-architected" "gradle-architected" "javascript-architected" "pipelines-architected" "scripts-architected")

# Copy plugin documentation
echo "Copying plugin documentation..."
for plugin in "${PLUGINS[@]}"; do
    if [ -d "plugins/$plugin/docs" ]; then
        cp -r "plugins/$plugin/docs" "docs/plugins/$plugin" 2>/dev/null || true
        echo "  ✓ Copied $plugin documentation"
    fi
done

echo "Building documentation with MkDocs..."
mkdocs build

echo "Documentation built successfully! Output in site/"
