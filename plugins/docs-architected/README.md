# Docs Architected Plugin

An Architect plugin that provides comprehensive documentation management capabilities, including building and publishing to GitHub Pages with support for multiple documentation frameworks and **monorepo/nested documentation structures**.

## Overview

The Docs Architected plugin integrates with the Architect platform to provide:
- Documentation building from Markdown sources
- Support for multiple documentation frameworks (MkDocs, Docusaurus, VuePress)
- **Monorepo support for nested component documentation**
- Automated publishing to GitHub Pages
- GitHub Actions workflow generation
- Standardized documentation structure
- Custom domain support

## Features

### 1. Documentation Building

Build documentation from Markdown sources using your preferred framework. **Each builder automatically manages its own environment:**

- **MkDocs**: Python-based documentation generator with Material theme
  - ✅ **Automatic Python venv creation** (`.venv-docs`)
  - ✅ Isolated dependency installation in virtual environment
  - ✅ No global Python package pollution
- **Docusaurus**: React-based documentation framework by Facebook
  - ✅ **Automatic Node.js environment detection**
  - ✅ Smart package manager detection (npm/yarn/pnpm)
  - ✅ Lockfile-based dependency installation
- **VuePress**: Vue-powered static site generator
  - ✅ **Automatic Node.js environment detection**
  - ✅ Smart package manager detection (npm/yarn/pnpm)
  - ✅ Lockfile-based dependency installation
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
    mkdocsVersion: "1.5.3"  # MkDocs version (configurable for security updates)
    mkdocsMaterialVersion: "9.5.3"  # Material theme version
    mkdocsMonorepoVersion: "1.0.5"  # Monorepo plugin version (for nested docs)
    # Template configuration - all fields are configurable
    siteName: "My Project Documentation"
    siteDescription: "Project documentation"
    siteAuthor: "Your Name"
    repoUrl: "https://github.com/username/repo"  # Optional
    repoName: "username/repo"  # Optional (used for links)
    primaryColor: "indigo"  # Theme primary color
    accentColor: "indigo"  # Theme accent color
```

### 2. Monorepo/Nested Documentation Support ⭐ NEW

The plugin now supports building documentation for monorepos with multiple components:

```yaml
docs:
  build:
    framework: "mkdocs"
    monorepo: true  # Enable monorepo mode
    components:
      - name: "Component A"
        path: "component-a"
        docsPath: "docs"
      - name: "Component B"
        path: "component-b"
        docsPath: "docs"
      - name: "Shared Library"
        path: "libs/shared"
        docsPath: "documentation"
  publish:
    enabled: true
    githubPages: true
```

**How it works:**
- Main documentation in root `docs/` folder
- Each component has its own `docs/` folder with `mkdocs.yml`
- Uses `mkdocs-monorepo-plugin` to aggregate all docs
- Components accessible via URL paths (e.g., `/component-a/`, `/component-b/`)
- All published together to GitHub Pages

### 3. GitHub Pages Publishing

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

The plugin uses a **builder pattern** with separate abstractions for building and publishing documentation. Each documentation framework (MkDocs, Docusaurus, VuePress) has its own builder implementation that manages its specific environment and dependencies.

### Core Components

- **DocsPlugin**: Main plugin class implementing `ArchitectPlugin`
- **DocsTask**: Task executor for documentation operations
- **DocsContext**: Configuration container for documentation settings
- **BuildContext**: Build-specific configuration
- **PublishContext**: Publishing-specific configuration

### Builder Pattern

#### Documentation Builders

Each builder is responsible for:
- **Environment Setup**: Creating and managing required runtime environments (Python venv, Node.js)
- **Dependency Installation**: Installing framework-specific dependencies
- **Configuration Generation**: Creating framework configuration files
- **Building**: Executing the build process
- **Cleanup**: Removing temporary files

Available builders:
- **MkDocsBuilder**: Python-based, creates and manages Python virtual environment (`.venv-docs`)
- **DocusaurusBuilder**: Node.js-based, manages npm/yarn/pnpm dependencies
- **VuePressBuilder**: Node.js-based, manages npm/yarn/pnpm dependencies
- **NodeJsDocumentationBuilder**: Base class for Node.js-based builders

#### Publishing Strategies

Each publisher is responsible for:
- **Environment Validation**: Ensuring prerequisites are met
- **Pre-publish Operations**: Creating necessary files (CNAME, .nojekyll)
- **Publishing**: Deploying documentation to target destination
- **Cleanup**: Removing temporary files

Available publishers:
- **GitHubPagesPublisher**: Publishes to GitHub Pages via gh-pages branch

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

### Class Diagram

```
DocsPlugin
  ├── Uses DocumentationBuilderFactory
  │     ├── Creates MkDocsBuilder (Python + venv)
  │     ├── Creates DocusaurusBuilder (Node.js)
  │     └── Creates VuePressBuilder (Node.js)
  └── Uses GitHubPagesPublisher
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

### Complete Configuration Example

```yaml
project:
  name: awesome-project
  description: "An awesome open-source project"

docs:
  build:
    enabled: true
    framework: "mkdocs"
    sourceDir: "docs"
    outputDir: "site"
    installDeps: true
    # Version configuration
    mkdocsVersion: "1.5.3"
    mkdocsMaterialVersion: "9.5.3"
    # Site configuration (used in templates)
    siteName: "Awesome Project Documentation"
    siteDescription: "Complete guide for the awesome project"
    siteAuthor: "Awesome Team"
    repoUrl: "https://github.com/awesome/project"
    repoName: "awesome/project"
    primaryColor: "blue"
    accentColor: "cyan"
  publish:
    enabled: true
    githubPages: true
    branch: "gh-pages"
    domain: "docs.awesome-project.com"
    cname: true
```

### Minimal Configuration

```yaml
docs:
  build:
    framework: "mkdocs"
    siteName: "My Docs"
  publish:
    enabled: true
```

### Disable Publishing (Build Only)

```yaml
docs:
  build:
    framework: "mkdocs"
  publish:
    enabled: false
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
- `configs/mkdocs.yml`: MkDocs configuration template (with placeholders)
- `configs/docusaurus.config.js`: Docusaurus configuration template (with placeholders)
- `configs/vuepress.config.js`: VuePress configuration template (with placeholders)
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

The plugin uses a **builder pattern** making it easy to add new documentation frameworks:

1. **Create a new builder class** extending `DocumentationBuilder`:
   ```kotlin
   class MyFrameworkBuilder(
       context: BuildContext,
       commandExecutor: CommandExecutor
   ) : DocumentationBuilder(context, commandExecutor) {
       
       override fun setupEnvironment(workingDir: File): TaskResult {
           // Setup runtime environment (venv, node_modules, etc.)
       }
       
       override fun installDependencies(workingDir: File): TaskResult {
           // Install framework dependencies
       }
       
       override fun generateConfiguration(workingDir: File, components: List<ComponentDocs>): TaskResult {
           // Generate framework config files
       }
       
       override fun build(workingDir: File): TaskResult {
           // Execute build command
       }
       
       override fun cleanup(workingDir: File) {
           // Clean up temporary files
       }
       
       override fun getName(): String = "MyFramework"
   }
   ```

2. **Register the builder** in `DocumentationBuilderFactory`:
   ```kotlin
   fun createBuilder(context: BuildContext, commandExecutor: CommandExecutor): DocumentationBuilder {
       return when (context.framework.lowercase()) {
           "mkdocs" -> MkDocsBuilder(context, commandExecutor)
           "docusaurus" -> DocusaurusBuilder(context, commandExecutor)
           "vuepress" -> VuePressBuilder(context, commandExecutor)
           "myframework" -> MyFrameworkBuilder(context, commandExecutor) // Add here
           else -> throw IllegalArgumentException("Unsupported framework")
       }
   }
   ```

3. **Add tests** for your new builder

### Adding New Publishing Targets

To add support for publishing to new destinations (S3, Netlify, etc.):

1. **Create a new publisher class** extending `PublishStrategy`:
   ```kotlin
   class MyPublisher(
       context: PublishContext,
       commandExecutor: CommandExecutor
   ) : PublishStrategy(context, commandExecutor) {
       
       override fun validateEnvironment(workingDir: File, outputDir: File): TaskResult {
           // Validate prerequisites
       }
       
       override fun prePublish(outputDir: File): TaskResult {
           // Pre-publish setup
       }
       
       override fun publish(workingDir: File, outputDir: File): TaskResult {
           // Publish documentation
       }
       
       override fun cleanup(workingDir: File) {
           // Clean up
       }
       
       override fun getName(): String = "MyPublisher"
   }
   ```

2. **Update `DocsPlugin.publishDocs()`** to use your publisher based on configuration
3. **Add configuration** to `PublishContext` for your publisher's settings

### Extending Configuration

To add new configuration options:

1. Update `BuildContext` or `PublishContext` in the `dto` package
2. Use the new configuration in your builder or publisher
3. Update documentation with new configuration examples

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
