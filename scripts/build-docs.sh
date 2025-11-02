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

# Copy component documentation
echo "Copying component documentation..."
cp -r architect-api/docs/* docs/architect-api/ 2>/dev/null || true
cp -r architect-cli/docs/* docs/architect-cli/ 2>/dev/null || true
cp -r architect-engine/docs/* docs/architect-engine/ 2>/dev/null || true
cp -r architect-cloud/docs/* docs/architect-cloud/ 2>/dev/null || true

# Copy plugin documentation
echo "Copying plugin documentation..."
cp -r plugins/docs-architected/docs docs/plugins/docs-architected 2>/dev/null || true
cp -r plugins/git-architected/docs docs/plugins/git-architected 2>/dev/null || true
cp -r plugins/github-architected/docs docs/plugins/github-architected 2>/dev/null || true
cp -r plugins/gradle-architected/docs docs/plugins/gradle-architected 2>/dev/null || true
cp -r plugins/javascript-architected/docs docs/plugins/javascript-architected 2>/dev/null || true
cp -r plugins/pipelines-architected/docs docs/plugins/pipelines-architected 2>/dev/null || true
cp -r plugins/scripts-architected/docs docs/plugins/scripts-architected 2>/dev/null || true

echo "Building documentation with MkDocs..."
mkdocs build

echo "Documentation built successfully! Output in site/"
