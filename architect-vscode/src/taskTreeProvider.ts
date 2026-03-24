import * as vscode from "vscode";
import * as path from "path";
import * as fs from "fs";
import { parseArchitectConfig } from "./architectConfigModel";

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

    const configPath = resolveArchitectConfigPath(workspaceFolder);
    if (!configPath) {
      this.tasks = [];
      return;
    }

    try {
      const content = fs.readFileSync(configPath, "utf-8");
      const model = parseArchitectConfig(content);
      this.tasks = model.tasks.map(
        (task) => new TaskItem(task.id, task.phase, task.description),
      );
    } catch {
      this.tasks = [];
    }
  }
}

function resolveArchitectConfigPath(workspaceFolder: string): string | undefined {
  for (const candidate of ["architect.yml", "architect.yaml"]) {
    const candidatePath = path.join(workspaceFolder, candidate);
    if (fs.existsSync(candidatePath)) {
      return candidatePath;
    }
  }

  return undefined;
}
