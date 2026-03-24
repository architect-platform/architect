# docs-architected

Multi-framework documentation building and publishing for Architect.

**Source**: `architect-platform/docs-architected`

## Installation

```yaml
plugins:
  - name: docs-architected
    type: github
    repo: architect-platform/docs-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `docs-init` | `INIT` | Initialize documentation structure and config |
| `docs-build` | `BUILD` | Build the documentation site |
| `docs-publish` | `PUBLISH` | Publish documentation to GitHub Pages |

## Configuration

Configuration key: `docs`

```yaml
docs-architected:
  build:
    enabled: true
    framework: mkdocs        # mkdocs | docusaurus | vuepress | manual
    sourceDir: docs/
    outputDir: site/
    installDeps: true
    siteName: "My Project Docs"
    siteDescription: "Project documentation"
    siteAuthor: "Your Name"
    repoUrl: https://github.com/my-org/my-repo
    repoName: my-org/my-repo
    primaryColor: indigo
    accentColor: indigo
    autoDiscoverComponents: true
    componentPaths:
      - "."
      - plugins
    components:              # explicit list; overrides auto-discovery
      - name: architect-api
        path: architect-api
        docsPath: docs
      - name: architect-engine
        path: architect-engine
        docsPath: docs

  publish:
    enabled: true
    githubPages: true
    branch: gh-pages
    domain: ""               # leave empty for <org>.github.io/<repo>
    cname: true
```

### `docs.build`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | `boolean` | `true` | Enable or disable doc builds |
| `framework` | `string` | `mkdocs` | Documentation framework: `mkdocs`, `docusaurus`, `vuepress`, `manual` |
| `sourceDir` | `string` | `docs` | Directory containing source markdown |
| `outputDir` | `string` | `site` | Output directory for built HTML |
| `configFile` | `string` | `""` | Custom framework config file path |
| `installDeps` | `boolean` | `true` | Install Python/Node deps before build |
| `mkdocsVersion` | `string` | `1.5.3` | MkDocs version to install |
| `mkdocsMaterialVersion` | `string` | `9.5.3` | MkDocs Material theme version |
| `mkdocsMonorepoVersion` | `string` | `1.0.5` | mkdocs-monorepo-plugin version |
| `siteName` | `string` | `My Project Documentation` | Site title |
| `siteDescription` | `string` | `Project documentation` | Meta description |
| `siteAuthor` | `string` | `Your Name` | Meta author |
| `repoUrl` | `string` | `""` | GitHub / GitLab repo URL for "edit this page" links |
| `repoName` | `string` | `""` | Short name shown in theme (e.g. `my-org/my-repo`) |
| `primaryColor` | `string` | `indigo` | Theme primary colour |
| `accentColor` | `string` | `indigo` | Theme accent colour |
| `autoDiscoverComponents` | `boolean` | `true` | Auto-discover sub-project `docs/` folders |
| `componentPaths` | `string[]` | `[".", "plugins"]` | Search roots for auto-discovery |
| `components` | `ComponentDocs[]` | `[]` | Explicit component list (overrides auto-discovery) |

### `docs.build.components[]`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `name` | `string` | `""` | Component display name |
| `path` | `string` | `""` | Relative path to component root |
| `docsPath` | `string` | `docs` | Sub-path within component containing markdown |

### `docs.publish`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | `boolean` | `true` | Enable GitHub Pages publishing |
| `githubPages` | `boolean` | `true` | Use GitHub Pages as publish target |
| `branch` | `string` | `gh-pages` | Branch to push built HTML to |
| `domain` | `string` | `""` | Custom domain (e.g. `docs.myproject.io`) |
| `cname` | `boolean` | `true` | Generate a `CNAME` file when `domain` is set |

## Usage examples

```bash
# Build docs locally
architect docs-build

# Serve locally (MkDocs only)
cd docs && mkdocs serve

# Publish to GitHub Pages
architect docs-publish
```
