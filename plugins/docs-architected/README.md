# Docs Architected Plugin

An Architect plugin that provides comprehensive documentation management capabilities, including building and publishing to GitHub Pages with support for multiple documentation frameworks.

## Overview

The Docs Architected plugin integrates with the Architect platform to provide:
- Documentation building from Markdown sources
- Support for multiple documentation frameworks (MkDocs, Docusaurus, VuePress)
- Automated publishing to GitHub Pages
- GitHub Actions workflow generation
- Standardized documentation structure
- Custom domain support

## Features

### 1. Documentation Building

Build documentation from Markdown sources using your preferred framework:

- **MkDocs**: Python-based documentation generator with Material theme
- **Docusaurus**: React-based documentation framework by Facebook
- **VuePress**: Vue-powered static site generator
- **Manual**: Custom build process for specialized needs

#### Configuration

```yaml
docs:
  build:
    enabled: true
    framework: "mkdocs"  # mkdocs, docusaurus, vuepress, or manual
    sourceDir: "docs"
    outputDir: "site"
    configFile: ""  # Optional custom config file
    installDeps: true
```

### 2. GitHub Pages Publishing

Automatically publish your documentation to GitHub Pages:

- **Automated Deployment**: Push to gh-pages branch
- **Custom Domains**: Support for custom domain configuration
- **CNAME Generation**: Automatic CNAME file creation
- **Branch Control**: Configurable target branch

#### Configuration

```yaml
docs:
  publish:
    enabled: true
    githubPages: true
    branch: "gh-pages"
    domain: "docs.example.com"  # Optional custom domain
    cname: true
```

### 3. Workflow Automation

Generates GitHub Actions workflows for continuous documentation deployment:

- **Automatic Triggers**: Deploy on documentation changes
- **Multi-Framework Support**: Works with all supported frameworks
- **Optimized Caching**: Uses Node.js and Python caching for faster builds
- **Pages Deployment**: Utilizes GitHub's official Pages deployment action

## Architecture

### Core Components

- **DocsPlugin**: Main plugin class implementing `ArchitectPlugin`
- **DocsTask**: Task executor for documentation operations
- **DocsContext**: Configuration container for documentation settings
- **BuildContext**: Build-specific configuration
- **PublishContext**: Publishing-specific configuration

### Configuration Model

```kotlin
DocsContext(
  build: BuildContext(
    enabled: Boolean,
    framework: String,
    sourceDir: String,
    outputDir: String,
    configFile: String,
    installDeps: Boolean
  ),
  publish: PublishContext(
    enabled: Boolean,
    githubPages: Boolean,
    branch: String,
    domain: String,
    cname: Boolean
  )
)
```

## Tasks

The plugin registers the following tasks:

### `docs-init` (Init Phase)
- **Phase**: `INIT`
- **Purpose**: Initialize documentation structure and workflows
- **Actions**:
  - Creates documentation directory
  - Generates initial index.md
  - Creates GitHub Actions workflow
  - Generates framework-specific configuration files

### `docs-build` (Build Phase)
- **Phase**: `BUILD`
- **Purpose**: Build documentation from sources
- **Actions**:
  - Installs framework dependencies
  - Executes framework build command
  - Validates output

### `docs-publish` (Publish Phase)
- **Phase**: `PUBLISH`
- **Purpose**: Publish documentation to GitHub Pages
- **Actions**:
  - Deploys to gh-pages branch
  - Creates CNAME file for custom domains
  - Pushes changes to remote

## Supported Frameworks

### MkDocs

Python-based documentation generator with beautiful Material theme.

**Installation**: Automatic via pip
**Build Command**: `mkdocs build`
**Configuration**: `mkdocs.yml`

**Features**:
- Material Design theme
- Full-text search
- Code highlighting
- Markdown extensions
- Navigation tabs

### Docusaurus

Modern documentation framework built with React.

**Installation**: Automatic via npm
**Build Command**: `npm run build`
**Configuration**: `docusaurus.config.js`

**Features**:
- React-based
- MDX support
- Versioning
- Internationalization
- Blog support

### VuePress

Vue-powered static site generator for documentation.

**Installation**: Automatic via npm
**Build Command**: `npm run docs:build`
**Configuration**: `docs/.vuepress/config.js`

**Features**:
- Vue in Markdown
- Plugin system
- Default theme
- Search
- PWA support

### Manual

For projects with custom documentation build processes.

**Configuration**: No automatic build
**Use Case**: Projects with specialized build requirements

## Building

```bash
cd app
./gradlew build
```

**Note**: Building requires access to the Architect API package. Set the following environment variables:
- `GITHUB_USER` or `githubUser` property
- `REGISTRY_TOKEN` or `GITHUB_TOKEN` environment variable

## Usage

### Basic Setup with MkDocs

1. Add the plugin to your Architect project
2. Configure the plugin in your `architect.yml`:

```yaml
docs:
  build:
    enabled: true
    framework: "mkdocs"
    sourceDir: "docs"
    outputDir: "site"
  publish:
    enabled: true
    githubPages: true
    branch: "gh-pages"
```

3. Initialize documentation structure:
```bash
architect run docs-init
```

4. Build documentation:
```bash
architect run docs-build
```

5. Publish to GitHub Pages:
```bash
architect run docs-publish
```

### Setup with Docusaurus

```yaml
docs:
  build:
    enabled: true
    framework: "docusaurus"
    sourceDir: "docs"
    outputDir: "build"
  publish:
    enabled: true
    githubPages: true
```

### Setup with VuePress

```yaml
docs:
  build:
    enabled: true
    framework: "vuepress"
    sourceDir: "docs"
    outputDir: "docs/.vuepress/dist"
  publish:
    enabled: true
    githubPages: true
```

### Custom Domain Configuration

```yaml
docs:
  publish:
    enabled: true
    githubPages: true
    domain: "docs.myproject.com"
    cname: true
```

## Resource Files

The plugin includes embedded resources:

- `workflows/docs-publish.yml`: GitHub Actions workflow template
- `configs/mkdocs.yml`: MkDocs configuration template
- `configs/docusaurus.config.js`: Docusaurus configuration template
- `configs/vuepress.config.js`: VuePress configuration template
- `scripts/publish-ghpages.sh`: GitHub Pages publishing script

## Workflow Integration

The plugin integrates with Architect workflow phases:

| Architect Phase | Docs Tasks | Purpose |
|----------------|-----------|---------|
| `INIT` | `docs-init` | Setup documentation structure |
| `BUILD` | `docs-build` | Build documentation |
| `PUBLISH` | `docs-publish` | Deploy to GitHub Pages |

## GitHub Actions Integration

The generated workflow file automatically:
1. Detects changes to documentation files
2. Sets up the appropriate runtime (Node.js or Python)
3. Installs framework dependencies
4. Builds the documentation
5. Deploys to GitHub Pages

**Trigger Paths**:
- `docs/**`
- Framework config files
- Workflow file itself

## Technical Stack

- **Language**: Kotlin 1.9.25
- **JVM**: Java 17
- **Dependencies**: Architect API 1.1.2, Jackson
- **Build Tool**: Gradle 8.12

## Development

### Adding New Framework Support

1. Create a configuration template in `resources/configs/`
2. Update the `initDocs()` method to handle the new framework
3. Update the `buildDocs()` method with build commands
4. Update the workflow template for the new framework

### Customizing Build Process

1. Modify `buildDocs()` method in `DocsPlugin.kt`
2. Add custom build logic for your framework
3. Update configuration context if new settings are needed

### Extending Publishing Options

1. Add new properties to `PublishContext`
2. Update `publishDocs()` method with new logic
3. Modify publish script if needed

## Error Handling

The plugin provides comprehensive error handling:
- Git repository validation
- Output directory validation
- Framework detection and validation
- Dependency installation error capture
- Build failure reporting
- Publishing error handling

## Best Practices

1. **Use Standard Directory Structure**: Keep documentation in `docs/` directory
2. **Commit Configuration Files**: Version control framework config files
3. **Test Locally First**: Build locally before pushing
4. **Use Conventional Commits**: Document changes clearly
5. **Custom Domains**: Configure DNS before enabling custom domain
6. **Keep Dependencies Updated**: Regularly update framework versions

## Examples

### Complete Configuration Example

```yaml
project:
  name: my-awesome-project
  description: "An awesome project with great docs"

docs:
  build:
    enabled: true
    framework: "mkdocs"
    sourceDir: "docs"
    outputDir: "site"
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

### Minimal Configuration

```yaml
docs:
  build:
    framework: "mkdocs"
  publish:
    enabled: true
```

### Disable Publishing (Build Only)

```yaml
docs:
  build:
    enabled: true
    framework: "mkdocs"
  publish:
    enabled: false
```

## Troubleshooting

### Build Fails

- **Issue**: Dependencies not installed
- **Solution**: Ensure `installDeps: true` and framework is installed globally

### Publish Fails

- **Issue**: Output directory not found
- **Solution**: Run `docs-build` before `docs-publish`

### Workflow Not Triggering

- **Issue**: GitHub Actions workflow not running
- **Solution**: Check workflow file in `.github/workflows/` and permissions

### Custom Domain Not Working

- **Issue**: Domain not resolving
- **Solution**: Configure DNS CNAME record pointing to `<username>.github.io`

## Security Considerations

- Scripts are executed with appropriate permissions
- Temporary files are cleaned up
- Git operations respect repository boundaries
- No credentials stored in configuration

## Comparison with Manual Setup

### Manual Documentation Management
```bash
cd docs
mkdocs build
git checkout gh-pages
cp -r site/* .
git add .
git commit -m "Update docs"
git push
git checkout main
```

### Using Docs Architected Plugin
```bash
architect run docs-init
architect run docs-build
architect run docs-publish
```

Benefits of using the plugin:
- Automated workflow generation
- Standardized structure
- Framework abstraction
- Reduced manual steps
- Integration with Architect platform
- Consistent team workflow

## Contributing

Contributions are welcome! Please follow the existing code style and add tests for new functionality.

## License

MIT License - see LICENSE file for details

## Support

For issues, questions, or contributions, please visit the [Architect Platform repository](https://github.com/architect-platform/architect).
