import * as vscode from "vscode";
import * as cp from "child_process";

/**
 * WebView panel that renders the task dependency graph using Mermaid.
 * Runs `architect graph` in DOT mode, converts to Mermaid syntax, and displays
 * in a live-updating webview panel.
 */
export class GraphPanel {
  private static currentPanel: GraphPanel | undefined;
  private readonly panel: vscode.WebviewPanel;
  private disposables: vscode.Disposable[] = [];

  static show(context: vscode.ExtensionContext) {
    if (GraphPanel.currentPanel) {
      GraphPanel.currentPanel.panel.reveal(vscode.ViewColumn.Beside);
      GraphPanel.currentPanel.loadGraph();
      return;
    }
    GraphPanel.currentPanel = new GraphPanel(context);
  }

  private constructor(private context: vscode.ExtensionContext) {
    this.panel = vscode.window.createWebviewPanel(
      "architectGraph",
      "Architect — Task Graph",
      vscode.ViewColumn.Beside,
      { enableScripts: true, retainContextWhenHidden: true },
    );

    this.panel.onDidDispose(() => this.dispose(), null, this.disposables);
    this.loadGraph();

    // Re-render when architect.yml changes
    const watcher = vscode.workspace.createFileSystemWatcher("**/architect.yml");
    watcher.onDidChange(() => this.loadGraph());
    watcher.onDidCreate(() => this.loadGraph());
    watcher.onDidDelete(() => this.loadGraph());
    this.disposables.push(watcher);
  }

  private loadGraph() {
    const workspaceFolder = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath;
    if (!workspaceFolder) {
      this.panel.webview.html = this.errorHtml("No workspace folder open.");
      return;
    }

    const config = vscode.workspace.getConfiguration("architect");
    const executable = config.get<string>("executablePath", "architect");

    this.panel.webview.html = this.loadingHtml();

    // Run `architect --embedded graph` to get DOT output, then convert to Mermaid
    const proc = cp.execFile(
      executable,
      ["--embedded", "graph"],
      { cwd: workspaceFolder, timeout: 30000 },
      (error: Error | null, stdout: string, stderr: string) => {
        if (error) {
          this.panel.webview.html = this.errorHtml(
            `Failed to generate graph.\n${stderr || error.message}`,
          );
          return;
        }
        const mermaid = this.dotToMermaid(stdout);
        this.panel.webview.html = this.graphHtml(mermaid);
      },
    );
  }

  /**
   * Convert DOT output into Mermaid graph syntax.
   * Handles: `"a" -> "b"` edges and `"a" [label="..."]` node labels.
   */
  private dotToMermaid(dot: string): string {
    const lines: string[] = ["graph TD"];
    const labels = new Map<string, string>();
    const edges: string[] = [];

    for (const line of dot.split("\n")) {
      const trimmed = line.trim();

      // Node label: "id" [label="..."]
      const labelMatch = trimmed.match(
        /^"([^"]+)"\s*\[label="([^"]+)"]/,
      );
      if (labelMatch) {
        labels.set(labelMatch[1], labelMatch[2]);
        continue;
      }

      // Edge: "a" -> "b"
      const edgeMatch = trimmed.match(/^"([^"]+)"\s*->\s*"([^"]+)"/);
      if (edgeMatch) {
        edges.push(`  ${sanitize(edgeMatch[1])} --> ${sanitize(edgeMatch[2])}`);
      }
    }

    // Emit node labels
    for (const [id, label] of labels) {
      lines.push(`  ${sanitize(id)}["${escapeLabel(label)}"]`);
    }

    lines.push(...edges);
    return lines.join("\n");
  }

  private graphHtml(mermaid: string): string {
    return /* html */ `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<style>
  body { margin: 0; padding: 16px; background: var(--vscode-editor-background); color: var(--vscode-editor-foreground); font-family: var(--vscode-font-family); }
  h2 { margin: 0 0 12px 0; font-size: 14px; font-weight: 600; }
  .mermaid { display: flex; justify-content: center; }
  .mermaid svg { max-width: 100%; height: auto; }
</style>
</head>
<body>
<h2>Task Dependency Graph</h2>
<div class="mermaid">
${escapeHtml(mermaid)}
</div>
<script src="https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"></script>
<script>
  mermaid.initialize({
    startOnLoad: true,
    theme: document.body.classList.contains('vscode-light') ? 'default' : 'dark',
    flowchart: { useMaxWidth: true, htmlLabels: true, curve: 'basis' },
  });
</script>
</body>
</html>`;
  }

  private loadingHtml(): string {
    return /* html */ `<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8">
<style>body { display:flex; align-items:center; justify-content:center; height:100vh; margin:0; background:var(--vscode-editor-background); color:var(--vscode-editor-foreground); font-family:var(--vscode-font-family); }</style>
</head><body><p>Loading task graph\u2026</p></body></html>`;
  }

  private errorHtml(message: string): string {
    return /* html */ `<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8">
<style>body { padding:16px; background:var(--vscode-editor-background); color:var(--vscode-errorForeground); font-family:var(--vscode-font-family); } pre { white-space:pre-wrap; }</style>
</head><body><h2>Graph Error</h2><pre>${escapeHtml(message)}</pre></body></html>`;
  }

  private dispose() {
    GraphPanel.currentPanel = undefined;
    this.panel.dispose();
    for (const d of this.disposables) {
      d.dispose();
    }
    this.disposables = [];
  }
}

function sanitize(id: string): string {
  return id.replace(/[^a-zA-Z0-9_]/g, "_");
}

function escapeLabel(text: string): string {
  return text.replace(/"/g, "&quot;");
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}
