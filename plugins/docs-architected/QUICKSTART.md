# Quick Start Guide - docs-architected Plugin

Get your documentation up and running in 5 minutes!

## Installation

Add the plugin to your `architect.yml`:

```yaml
plugins:
  - name: docs-architected
    repo: architect-platform/architect
```

## Quick Setup (MkDocs)

### 1. Configure
```yaml
docs:
  build:
    framework: "mkdocs"
  publish:
    enabled: true
    githubPages: true
```

### 2. Initialize
```bash
architect run docs-init
```

This creates:
- `docs/` directory with `index.md`
- `mkdocs.yml` configuration
- `.github/workflows/docs-publish.yml` workflow

### 3. Write Documentation
Edit `docs/index.md`:
```markdown
# My Project

Welcome to my project documentation!

## Features
- Easy to use
- Well documented
- Open source
```

### 4. Build & Preview Locally
```bash
architect run docs-build
cd site
python -m http.server 8000
# Visit http://localhost:8000
```

### 5. Publish to GitHub Pages
```bash
architect run docs-publish
```

Your documentation is now live at `https://username.github.io/repository/`!

## Framework Comparison

| Feature | MkDocs | Docusaurus | VuePress |
|---------|--------|------------|----------|
| Language | Python | JavaScript | JavaScript |
| Setup Speed | ⚡ Fastest | 🚀 Fast | 🚀 Fast |
| Theme | Material | React | Vue |
| Learning Curve | 🟢 Easy | 🟡 Medium | 🟡 Medium |
| Best For | Technical docs | Project sites | Vue projects |

## Common Tasks

### Add a New Page
```bash
# Create new markdown file
echo "# New Page" > docs/new-page.md

# Update mkdocs.yml
nav:
  - Home: index.md
  - New Page: new-page.md

# Rebuild
architect run docs-build
```

### Change Theme
Edit `mkdocs.yml`:
```yaml
theme:
  name: material
  palette:
    primary: blue
    accent: blue
```

### Enable Search
Already enabled by default with Material theme!

### Add Code Highlighting
Edit `mkdocs.yml`:
```yaml
markdown_extensions:
  - codehilite
  - pymdownx.superfences
```

## Troubleshooting

### "Command not found: mkdocs"
```bash
pip3 install mkdocs mkdocs-material
```

### "Output directory not found"
Run build before publish:
```bash
architect run docs-build
architect run docs-publish
```

### GitHub Pages not updating
1. Check repository Settings → Pages
2. Ensure source is set to `gh-pages` branch
3. Wait 1-2 minutes for deployment

## Configuration Options

### Minimal (defaults)
```yaml
docs:
  build:
    framework: "mkdocs"
```

### Complete
```yaml
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
    domain: ""
    cname: true
```

## Next Steps

1. **Customize Theme**: Edit `mkdocs.yml` to match your brand
2. **Add Content**: Create more `.md` files in `docs/`
3. **Organize**: Use subdirectories for better structure
4. **Automate**: Let GitHub Actions handle deployment
5. **Custom Domain**: Add your own domain name

## Resources

- [Full README](README.md) - Complete documentation
- [Examples](EXAMPLES.md) - Real-world usage scenarios
- [MkDocs Documentation](https://www.mkdocs.org/)
- [Material Theme](https://squidfunk.github.io/mkdocs-material/)

## Tips

✅ **DO**: Write in Markdown, commit regularly, use GitHub Actions
❌ **DON'T**: Edit files in `site/` directory (auto-generated)

---

**Need Help?** Check the [full documentation](README.md) or [examples](EXAMPLES.md)!
