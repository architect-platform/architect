# Architect Platform Scripts

Utility scripts for the Architect Platform.

## build-docs.sh

Builds the comprehensive documentation for the Architect Platform.

### Prerequisites

```bash
pip install mkdocs==1.5.3 mkdocs-material==9.5.3
```

### Usage

```bash
# Build documentation
./scripts/build-docs.sh

# Serve documentation locally
mkdocs serve

# Clean built documentation
rm -rf site docs/architect-* docs/plugins
```

### How It Works

1. **Copies component docs** from each component's `docs/` folder to the main `docs/` directory
2. **Copies plugin docs** from each plugin's `docs/` folder to `docs/plugins/`
3. **Builds the site** using MkDocs with Material theme
4. **Outputs to** `site/` directory

### Structure

```
docs/
├── index.md                    # Main landing page
├── architect-api/              # API documentation (copied from architect-api/docs/)
├── architect-cli/              # CLI documentation (copied from architect-cli/docs/)
├── architect-engine/           # Engine documentation (copied from architect-engine/docs/)
├── architect-cloud/            # Cloud documentation (copied from architect-cloud/docs/)
└── plugins/                    # Plugin documentation (copied from plugins/*/docs/)
    ├── docs-architected/
    ├── git-architected/
    └── ...
```

### Publishing

To publish to GitHub Pages:

```bash
# Build documentation
./scripts/build-docs.sh

# Deploy to gh-pages branch
mkdocs gh-deploy
```

Or use the architect CLI (when available):

```bash
architect docs-build
architect docs-publish
```

## Notes

- The copied documentation folders are in `.gitignore` to avoid duplication
- Source documentation is maintained in each component's own `docs/` folder
- The build script aggregates all documentation into a unified site
