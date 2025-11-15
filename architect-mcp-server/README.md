# Architect MCP Server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server that exposes Architect CLI capabilities to AI agents. This enables AI agents to interact with the Architect platform for project automation, task execution, and workflow management.

## Overview

The Architect MCP Server provides a standardized interface for AI agents to:
- Manage projects and task execution
- Interact with the Architect Engine
- Automate development workflows
- Execute build, test, and deployment tasks

## Features

### Project Management
- **List Projects**: View all projects registered with the Architect Engine
- **Register Project**: Add new projects to the engine
- **List Tasks**: Discover available tasks for a project
- **Get Task Details**: Retrieve information about specific tasks

### Task Execution
- **Execute Tasks**: Run tasks with optional arguments
- **Plain Output Mode**: CI-friendly output format
- **Working Directory Support**: Execute tasks in specific project directories

### Engine Management
- **Status Check**: Verify if the Architect Engine is running
- **Install**: Set up the Architect Engine
- **Start/Stop**: Control the engine lifecycle
- **Clean**: Remove engine data

## Installation

### Prerequisites
- Node.js 18.0.0 or higher
- Architect CLI installed and accessible in PATH
- Architect Engine (can be installed via MCP tools)

### Install from NPM (when published)
```bash
npm install -g architect-mcp-server
```

### Install from Source
```bash
cd architect-mcp-server
npm install
npm run build
npm link
```

## Usage

### With Claude Desktop

Add to your Claude Desktop configuration file:

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "architect": {
      "command": "architect-mcp-server",
      "env": {
        "ARCHITECT_ENGINE_URL": "http://localhost:9292"
      }
    }
  }
}
```

### With Other MCP Clients

Any MCP-compatible client can use this server via stdio transport:

```bash
architect-mcp-server
```

### Environment Variables

- `ARCHITECT_ENGINE_URL`: URL of the Architect Engine API (default: `http://localhost:9292`)

## Available Tools

### architect_list_projects
List all projects registered with the Architect Engine.

**Input**: None

**Output**: JSON array of projects with names and paths

### architect_register_project
Register a project with the Architect Engine.

**Input**:
- `name` (string, required): Project name
- `path` (string, required): Absolute path to project directory

**Output**: Registered project details

### architect_list_tasks
List all available tasks for a project.

**Input**:
- `projectName` (string, required): Name of the project

**Output**: JSON array of tasks with IDs, descriptions, and phases

### architect_get_task
Get detailed information about a specific task.

**Input**:
- `projectName` (string, required): Name of the project
- `taskName` (string, required): Name/ID of the task

**Output**: Task details

### architect_execute_task
Execute a task within a project.

**Input**:
- `taskName` (string, required): Name/ID of the task
- `args` (array of strings, optional): Task arguments
- `projectPath` (string, optional): Project directory path
- `plain` (boolean, optional): Use plain output mode (default: true)

**Output**: Task execution results

### architect_engine_status
Check if the Architect Engine is running.

**Input**: None

**Output**: Engine status and registered project count

### architect_engine_install
Install the Architect Engine.

**Input**:
- `installCi` (boolean, optional): Install for CI environment (default: false)

**Output**: Installation status

### architect_engine_start
Start the Architect Engine.

**Input**: None

**Output**: Engine start status

### architect_engine_stop
Stop the Architect Engine.

**Input**: None

**Output**: Engine stop status

### architect_engine_clean
Clean all Architect Engine data.

**Input**: None

**Output**: Clean operation status

## Example Workflows

### Initialize and Build a Project

1. **Check Engine Status**
   ```
   Tool: architect_engine_status
   ```

2. **Start Engine (if not running)**
   ```
   Tool: architect_engine_start
   ```

3. **Register Project**
   ```
   Tool: architect_register_project
   Input: {
     "name": "my-project",
     "path": "/path/to/project"
   }
   ```

4. **List Available Tasks**
   ```
   Tool: architect_list_tasks
   Input: {
     "projectName": "my-project"
   }
   ```

5. **Execute Build Task**
   ```
   Tool: architect_execute_task
   Input: {
     "taskName": "build",
     "projectPath": "/path/to/project"
   }
   ```

### Documentation Workflow

1. **Initialize Documentation**
   ```
   Tool: architect_execute_task
   Input: {
     "taskName": "docs-init",
     "projectPath": "/path/to/project"
   }
   ```

2. **Build Documentation**
   ```
   Tool: architect_execute_task
   Input: {
     "taskName": "docs-build",
     "projectPath": "/path/to/project"
   }
   ```

3. **Publish Documentation**
   ```
   Tool: architect_execute_task
   Input: {
     "taskName": "docs-publish",
     "projectPath": "/path/to/project"
   }
   ```

## Development

### Building
```bash
npm run build
```

### Watch Mode
```bash
npm run watch
```

### Testing
Test the server by running it directly:
```bash
npm run build
node dist/index.js
```

Then send MCP protocol messages via stdin (JSON-RPC format).

## Architecture

The MCP server acts as a bridge between AI agents and the Architect platform:

```
┌─────────────┐         ┌──────────────────┐         ┌──────────────────┐
│  AI Agent   │ ←──MCP──→ │ Architect MCP    │ ←──API──→ │ Architect Engine │
│  (Claude)   │         │     Server       │         │   (REST API)     │
└─────────────┘         └──────────────────┘         └──────────────────┘
                                 │
                                 │ CLI
                                 ▼
                        ┌──────────────────┐
                        │  Architect CLI   │
                        └──────────────────┘
```

The server provides two modes of operation:
1. **API Mode**: Direct communication with the Architect Engine REST API
2. **CLI Mode**: Executes the Architect CLI for task execution

## Troubleshooting

### Engine Not Accessible
If you get "Engine is not accessible" errors:
1. Check if the engine is installed: `architect engine status`
2. Start the engine: `architect engine start`
3. Verify the `ARCHITECT_ENGINE_URL` environment variable

### Command Not Found
If `architect` command is not found:
1. Ensure Architect CLI is installed
2. Add it to your PATH
3. Install via: `curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-cli/.installers/bash | bash`

### Permission Issues
If you encounter permission errors:
1. Check file permissions in the project directory
2. Ensure the user has write access for engine operations

## Contributing

Contributions are welcome! Please see the main [Architect Contributing Guide](../CONTRIBUTING.md).

## License

MIT License - see the [LICENSE](../LICENSE) file for details.

## Links

- [Architect Platform](https://github.com/architect-platform/architect)
- [Model Context Protocol](https://modelcontextprotocol.io)
- [Architect Documentation](https://github.com/architect-platform/architect/tree/main/docs)

## Support

- 📖 **Documentation**: See the main Architect README
- 💬 **Community**: GitHub Discussions
- 🐛 **Issues**: GitHub Issues
