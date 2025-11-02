# Docs Architected Plugin - Usage Examples

This document provides practical examples of using the docs-architected plugin in various scenarios.

## Example 1: Simple MkDocs Setup

The simplest configuration for a project using MkDocs.

### architect.yml
```yaml
project:
  name: my-project
  description: "My awesome project"

docs:
  build:
    framework: "mkdocs"
  publish:
    enabled: true
    githubPages: true

plugins:
  - name: docs-architected
    repo: architect-platform/architect
```

### Usage
```bash
# Initialize documentation structure
architect run docs-init

# Write your documentation in docs/index.md and other files

# Build documentation
architect run docs-build

# Publish to GitHub Pages
architect run docs-publish
```

## Example 2: Docusaurus with Custom Domain

Using Docusaurus with a custom domain configuration.

### architect.yml
```yaml
project:
  name: awesome-docs
  description: "Documentation site for Awesome Project"

docs:
  build:
    enabled: true
    framework: "docusaurus"
    sourceDir: "docs"
    outputDir: "build"
    installDeps: true
  publish:
    enabled: true
    githubPages: true
    branch: "gh-pages"
    domain: "docs.awesome-project.com"
    cname: true

plugins:
  - name: docs-architected
    repo: architect-platform/architect
```

### DNS Configuration
Add a CNAME record in your DNS settings:
```
docs.awesome-project.com -> your-username.github.io
```

### Usage
```bash
# Initialize
architect run docs-init

# Install and setup Docusaurus
npm init docusaurus@latest

# Build
architect run docs-build

# Publish with custom domain
architect run docs-publish
```

## Example 3: VuePress for Technical Documentation

Using VuePress for technical API documentation.

### architect.yml
```yaml
project:
  name: api-documentation
  description: "API Documentation"

docs:
  build:
    enabled: true
    framework: "vuepress"
    sourceDir: "docs"
    outputDir: "docs/.vuepress/dist"
  publish:
    enabled: true
    githubPages: true

plugins:
  - name: docs-architected
    repo: architect-platform/architect
```

### package.json
```json
{
  "scripts": {
    "docs:dev": "vuepress dev docs",
    "docs:build": "vuepress build docs"
  },
  "devDependencies": {
    "vuepress": "^1.9.7"
  }
}
```

### Usage
```bash
architect run docs-init
npm install
npm run docs:dev  # Local development
architect run docs-build
architect run docs-publish
```

## Example 4: Multi-Version Documentation

Managing multiple versions of documentation.

### architect.yml
```yaml
project:
  name: versioned-docs
  description: "Multi-version documentation"

docs:
  build:
    enabled: true
    framework: "docusaurus"
    sourceDir: "docs"
    outputDir: "build"
  publish:
    enabled: true
    githubPages: true
    branch: "gh-pages"

plugins:
  - name: docs-architected
    repo: architect-platform/architect
```

### Workflow
```bash
# Initialize
architect run docs-init

# Create version snapshot (Docusaurus)
npm run docusaurus docs:version 1.0

# Build all versions
architect run docs-build

# Publish
architect run docs-publish
```

## Example 5: Documentation Only Build (No Publishing)

Build documentation locally without publishing to GitHub Pages.

### architect.yml
```yaml
docs:
  build:
    enabled: true
    framework: "mkdocs"
    sourceDir: "docs"
    outputDir: "site"
  publish:
    enabled: false  # Disable publishing

plugins:
  - name: docs-architected
    repo: architect-platform/architect
```

### Usage
```bash
architect run docs-build

# Preview locally
cd site
python -m http.server 8000
```

## Example 6: Manual Framework with Custom Build

Using manual framework for custom documentation build process.

### architect.yml
```yaml
docs:
  build:
    enabled: true
    framework: "manual"
    sourceDir: "docs"
    outputDir: "public"
    installDeps: false
  publish:
    enabled: true
    githubPages: true

plugins:
  - name: docs-architected
    repo: architect-platform/architect
```

### Custom Build Script
Create a `build-docs.sh` script:
```bash
#!/bin/bash
# Custom documentation build
echo "Building custom documentation..."
# Your custom build logic here
```

### Usage
```bash
architect run docs-init
./build-docs.sh  # Run your custom build
architect run docs-publish
```

## Example 7: GitHub Actions Integration

Complete CI/CD setup with automated documentation deployment.

### architect.yml
```yaml
project:
  name: ci-docs-project

docs:
  build:
    framework: "mkdocs"
  publish:
    enabled: true
    githubPages: true

github:
  pipelines:
    - name: docs-deploy
      type: github/classic-java-17
      path: docs/**
      branch: main

plugins:
  - name: docs-architected
    repo: architect-platform/architect
  - name: github-architected
    repo: architect-platform/architect
```

### Workflow
The plugin automatically creates a GitHub Actions workflow that:
1. Triggers on changes to `docs/**`
2. Builds documentation
3. Deploys to GitHub Pages

No manual intervention needed after setup!

## Example 8: Private Repository Documentation

Documentation for private repositories.

### architect.yml
```yaml
project:
  name: private-docs
  
docs:
  build:
    enabled: true
    framework: "mkdocs"
  publish:
    enabled: true
    githubPages: true

plugins:
  - name: docs-architected
    repo: architect-platform/architect
```

### GitHub Settings
1. Go to repository Settings → Pages
2. Set source to `gh-pages` branch
3. Set visibility (public/private)

Note: GitHub Pages for private repos requires GitHub Pro/Enterprise.

## Example 9: Documentation with Search

Using MkDocs with enhanced search capabilities.

### architect.yml
```yaml
docs:
  build:
    framework: "mkdocs"
    sourceDir: "docs"

plugins:
  - name: docs-architected
    repo: architect-platform/architect
```

### mkdocs.yml (auto-generated, then customize)
```yaml
site_name: My Project
theme:
  name: material
  features:
    - search.suggest
    - search.highlight
    - search.share

plugins:
  - search:
      separator: '[\s\-\.]+'
      lang:
        - en
```

## Example 10: Multiple Documentation Sites

Managing multiple documentation sites in a monorepo.

### architect.yml (for docs site 1)
```yaml
project:
  name: docs-api
  
docs:
  build:
    framework: "mkdocs"
    sourceDir: "docs/api"
    outputDir: "site-api"
  publish:
    enabled: true
    branch: "gh-pages-api"
```

### architect.yml (for docs site 2)
```yaml
project:
  name: docs-user-guide
  
docs:
  build:
    framework: "mkdocs"
    sourceDir: "docs/user-guide"
    outputDir: "site-guide"
  publish:
    enabled: true
    branch: "gh-pages-guide"
```

## Troubleshooting Tips

### Issue: Build fails with dependency errors
**Solution**: Ensure framework is installed
```bash
# For MkDocs
pip3 install mkdocs mkdocs-material

# For Docusaurus/VuePress
npm install
```

### Issue: GitHub Pages not updating
**Solution**: Check workflow logs and branch permissions
```bash
git checkout gh-pages
git log  # Check if commits are being added
```

### Issue: Custom domain not working
**Solution**: Verify DNS and CNAME
```bash
dig docs.example.com  # Should point to username.github.io
```

## Best Practices

1. **Version Control**: Always commit `mkdocs.yml`, `docusaurus.config.js`, etc.
2. **Local Testing**: Test builds locally before publishing
3. **Incremental Updates**: Make small, frequent documentation updates
4. **Review Process**: Use pull requests for documentation changes
5. **Automated Deployment**: Let GitHub Actions handle publishing

## Integration with Other Plugins

### With Git Plugin
```yaml
git:
  config:
    user.name: "Doc Bot"
    user.email: "docs@example.com"

docs:
  build:
    framework: "mkdocs"

plugins:
  - name: git-architected
    repo: architect-platform/architect
  - name: docs-architected
    repo: architect-platform/architect
```

### With GitHub Plugin
```yaml
github:
  pipelines:
    - name: docs-pipeline
      type: github/classic-java-17
      path: docs/**

docs:
  build:
    framework: "mkdocs"

plugins:
  - name: github-architected
    repo: architect-platform/architect
  - name: docs-architected
    repo: architect-platform/architect
```

## Next Steps

1. Review the [main README](README.md) for detailed configuration options
2. Check the [plugin source code](app/src/main/kotlin/io/github/architectplatform/plugins/docs/DocsPlugin.kt)
3. Explore framework-specific documentation:
   - [MkDocs](https://www.mkdocs.org/)
   - [Docusaurus](https://docusaurus.io/)
   - [VuePress](https://vuepress.vuejs.org/)
