import * as vscode from "vscode";
import * as path from "path";
import * as fs from "fs";

export class TaskItem extends vscode.TreeItem {
  constructor(
    public readonly taskId: string,
    public readonly phase: string | undefined,
    public readonly description: string
  ) {
    super(taskId, vscode.TreeItemCollapsibleState.None);
    this.tooltip = description || taskId;
    this.contextValue = "task";
    this.iconPath = new vscode.ThemeIcon("symbol-event");
    if (phase) {
      this.description = `[${phase}]`;
    }
  }
}

export class TaskTreeProvider implements vscode.TreeDataProvider<TaskItem> {
  private _onDidChangeTreeData = new vscode.EventEmitter<
    TaskItem | undefined | void
  >();
  readonly onDidChangeTreeData = this._onDidChangeTreeData.event;

  private tasks: TaskItem[] = [];

  refresh(): void {
    this.loadTasks();
    this._onDidChangeTreeData.fire();
  }

  getTreeItem(element: TaskItem): vscode.TreeItem {
    return element;
  }

  getChildren(): TaskItem[] {
    return this.tasks;
  }

  private loadTasks(): void {
    const workspaceFolder = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath;
    if (!workspaceFolder) {
      this.tasks = [];
      return;
    }

    const configPath = path.join(workspaceFolder, "architect.yml");
    if (!fs.existsSync(configPath)) {
      this.tasks = [];
      return;
    }

    // Parse inline tasks from architect.yml
    try {
      const content = fs.readFileSync(configPath, "utf-8");
      this.tasks = this.parseInlineTasks(content);
    } catch {
      this.tasks = [];
    }
  }

  private parseInlineTasks(yamlContent: string): TaskItem[] {
    // Simple YAML parsing for the tasks section
    const items: TaskItem[] = [];
    const lines = yamlContent.split("\n");
    let inTasks = false;
    let currentIndent = 0;

    for (const line of lines) {
      const trimmed = line.trimStart();
      const indent = line.length - trimmed.length;

      if (trimmed === "tasks:") {
        inTasks = true;
        currentIndent = indent;
        continue;
      }

      if (inTasks) {
        // Detect top-level key under tasks (2 spaces deeper)
        if (indent === currentIndent + 2 && trimmed.endsWith(":")) {
          const taskId = trimmed.slice(0, -1).trim();
          items.push(new TaskItem(taskId, undefined, ""));
        }
        // Back to same or less indent means we left the tasks section
        if (indent <= currentIndent && trimmed.length > 0 && !trimmed.startsWith("#")) {
          inTasks = false;
        }
      }
    }

    return items;
  }
}
