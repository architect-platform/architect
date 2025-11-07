# Docs Architected Plugin

Documentation management with multi-framework support and monorepo capabilities.

## Overview

The Docs Architected plugin provides comprehensive documentation management for the Architect platform, including:
- Multi-framework support (MkDocs, Docusaurus, VuePress)
- Monorepo/nested documentation structures
- GitHub Pages publishing
- Automated workflow generation
- Custom domain support

## Features

### Supported Frameworks

- **MkDocs** - Python-based with Material Design theme
- **Docusaurus** - React-based by Facebook
- **VuePress** - Vue-powered static site generator
- **Manual** - Custom build process

### Monorepo Support ⭐

Build documentation for projects with multiple components:
- Each component has its own docs folder
- Aggregated into single documentation site
- Path-based URL routing (e.g., `/component-a/`, `/component-b/`)
- Uses mkdocs-monorepo-plugin

## Configuration

### Basic Setup

```yaml
docs:
  build:
    framework: "mkdocs"
    siteName: "My Documentation"
    siteDescription: "Project docs"
    siteAuthor: "Dev Team"
  publish:
    enabled: true
    githubPages: true
```

### Monorepo Setup

```yaml
docs:
  build:
    framework: "mkdocs"
    monorepo: true
    components:
      - name: "API"
        path: "api"
        docsPath: "docs"
      - name: "CLI"
        path: "cli"
        docsPath: "docs"
  publish:
    enabled: true
    githubPages: true
```

### Complete Configuration

```yaml
docs:
  build:
    enabled: true
    framework: "mkdocs"
    sourceDir: "docs"
    outputDir: "site"
    installDeps: true
    mkdocsVersion: "1.5.3"
    mkdocsMaterialVersion: "9.5.3"
    mkdocsMonorepoVersion: "1.0.5"
    siteName: "Project Documentation"
    siteDescription: "Complete guide"
    siteAuthor: "Team"
    repoUrl: "https://github.com/user/repo"
    repoName: "user/repo"
    primaryColor: "indigo"
    accentColor: "purple"
  publish:
    enabled: true
    githubPages: true
    branch: "gh-pages"
    domain: "docs.example.com"
    cname: true
```

## Tasks

### docs-init

Initialize documentation structure:
- Creates docs directory
- Generates configuration files
- Sets up GitHub Actions workflow

```bash
architect docs-init
```

### docs-build

Build documentation from sources:
- Installs dependencies if needed
- Executes framework build command
- Generates static site

```bash
architect docs-build
```

### docs-publish

Publish documentation to GitHub Pages:
- Deploys to gh-pages branch
- Creates CNAME for custom domains
- Pushes to remote repository

```bash
architect docs-publish
```

## Usage Examples

### MkDocs Project

```bash
# 1. Configure
cat > architect.yml << 'EOF'
docs:
  build:
    framework: "mkdocs"
    siteName: "My Docs"
  publish:
    enabled: true
EOF

# 2. Initialize
architect docs-init

# 3. Add content to docs/
echo "# Hello" > docs/index.md

# 4. Build
architect docs-build

# 5. Publish
architect docs-publish
```

### Monorepo Documentation

```bash
# 1. Configure with components
cat > architect.yml << 'EOF'
docs:
  build:
    framework: "mkdocs"
    monorepo: true
    components:
      - name: "Backend"
        path: "backend"
      - name: "Frontend"
        path: "frontend"
  publish:
    enabled: true
EOF

# 2. Initialize (creates docs in each component)
architect docs-init

# 3. Build (aggregates all docs)
architect docs-build

# 4. Publish to GitHub Pages
architect docs-publish
```

## GitHub Actions Integration

The plugin generates a GitHub Actions workflow:

```yaml
name: Publish Documentation

on:
  push:
    branches: [main]
    paths:
      - 'docs/**'
      - '*/docs/**'
      - 'mkdocs.yml'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.x'
      - run: pip install mkdocs mkdocs-material mkdocs-monorepo-plugin
      - run: mkdocs build
      - uses: actions/upload-pages-artifact@v3
  
  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/deploy-pages@v4
```

## Customization

### Custom Theme Colors

```yaml
docs:
  build:
    primaryColor: "blue"
    accentColor: "cyan"
```

Available colors: red, pink, purple, deep-purple, indigo, blue, light-blue, cyan, teal, green, light-green, lime, yellow, amber, orange, deep-orange

### Custom Domain

```yaml
docs:
  publish:
    domain: "docs.myproject.com"
    cname: true
```

Then configure DNS:
```
CNAME: docs.myproject.com -> username.github.io
```

## Troubleshooting

### Build Fails

**Issue**: Dependencies not installed

**Solution**:
```yaml
docs:
  build:
    installDeps: true
```

### Publish Fails

**Issue**: Output directory not found

**Solution**: Run `docs-build` before `docs-publish`

### Monorepo Navigation Issues

**Issue**: Component docs not accessible

**Solution**: Verify component paths and ensure mkdocs.yml exists in each component's docs folder

## Best Practices

1. **Version Control**: Commit mkdocs.yml and docs/ to Git
2. **Local Testing**: Always build locally before pushing
3. **Regular Updates**: Keep framework versions updated
4. **Custom Domains**: Configure DNS before enabling CNAME
5. **Documentation Structure**: Keep docs organized with clear navigation

## Contributing

Contributions welcome! See [CONTRIBUTING.md](../../../CONTRIBUTING.md)

## License

MIT License
