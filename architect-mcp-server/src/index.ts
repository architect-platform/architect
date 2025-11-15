#!/usr/bin/env node

/**
 * Architect MCP Server
 * 
 * This MCP server exposes Architect CLI capabilities to AI agents through the Model Context Protocol.
 * It provides tools for:
 * - Listing and registering projects
 * - Listing and executing tasks
 * - Managing the Architect Engine
 */

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  Tool,
} from "@modelcontextprotocol/sdk/types.js";
import { execSync, spawn } from "child_process";
import { promisify } from "util";
import { exec } from "child_process";

const execAsync = promisify(exec);

/**
 * Default engine URL for API communication
 */
const ENGINE_URL = process.env.ARCHITECT_ENGINE_URL || "http://localhost:9292";

/**
 * Execute an architect CLI command
 */
async function executeArchitectCommand(
  command: string[],
  options: { cwd?: string; plain?: boolean } = {}
): Promise<{ stdout: string; stderr: string }> {
  const args = [...command];
  if (options.plain) {
    args.unshift("--plain");
  }

  const cwd = options.cwd || process.cwd();
  
  try {
    const { stdout, stderr } = await execAsync(`architect ${args.join(" ")}`, {
      cwd,
      maxBuffer: 10 * 1024 * 1024, // 10MB buffer
    });
    return { stdout, stderr };
  } catch (error: any) {
    throw new Error(`Command failed: ${error.message}\nStderr: ${error.stderr || ""}`);
  }
}

/**
 * Make an HTTP request to the Architect Engine API
 */
async function engineApiRequest(
  method: string,
  path: string,
  body?: any
): Promise<any> {
  const url = `${ENGINE_URL}${path}`;
  
  try {
    const fetch = (await import("node-fetch")).default;
    const response = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
      },
      body: body ? JSON.stringify(body) : undefined,
    });

    if (!response.ok) {
      throw new Error(`API request failed: ${response.status} ${response.statusText}`);
    }

    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
      return await response.json();
    }
    return await response.text();
  } catch (error: any) {
    throw new Error(`Engine API request failed: ${error.message}`);
  }
}

/**
 * Define available MCP tools
 */
const tools: Tool[] = [
  {
    name: "architect_list_projects",
    description:
      "List all projects registered with the Architect Engine. Returns a list of project names and their paths.",
    inputSchema: {
      type: "object",
      properties: {},
      required: [],
    },
  },
  {
    name: "architect_register_project",
    description:
      "Register a project with the Architect Engine. This is required before executing tasks on a project.",
    inputSchema: {
      type: "object",
      properties: {
        name: {
          type: "string",
          description: "The name of the project",
        },
        path: {
          type: "string",
          description: "The absolute path to the project directory",
        },
      },
      required: ["name", "path"],
    },
  },
  {
    name: "architect_list_tasks",
    description:
      "List all available tasks for a project. Returns task IDs, descriptions, and phases.",
    inputSchema: {
      type: "object",
      properties: {
        projectName: {
          type: "string",
          description: "The name of the project",
        },
      },
      required: ["projectName"],
    },
  },
  {
    name: "architect_get_task",
    description:
      "Get detailed information about a specific task in a project.",
    inputSchema: {
      type: "object",
      properties: {
        projectName: {
          type: "string",
          description: "The name of the project",
        },
        taskName: {
          type: "string",
          description: "The name/ID of the task",
        },
      },
      required: ["projectName", "taskName"],
    },
  },
  {
    name: "architect_execute_task",
    description:
      "Execute a task within a project. This will run the task and return the results. The project must be registered first.",
    inputSchema: {
      type: "object",
      properties: {
        projectName: {
          type: "string",
          description: "The name of the project (if using API mode)",
        },
        taskName: {
          type: "string",
          description: "The name/ID of the task to execute",
        },
        args: {
          type: "array",
          items: {
            type: "string",
          },
          description: "Optional arguments to pass to the task",
        },
        projectPath: {
          type: "string",
          description:
            "The path to the project directory (for CLI mode, defaults to current directory)",
        },
        plain: {
          type: "boolean",
          description: "Use plain output mode (recommended for CI/automation)",
          default: true,
        },
      },
      required: ["taskName"],
    },
  },
  {
    name: "architect_engine_status",
    description:
      "Check if the Architect Engine is running and accessible.",
    inputSchema: {
      type: "object",
      properties: {},
      required: [],
    },
  },
  {
    name: "architect_engine_install",
    description:
      "Install the Architect Engine. Use installCi=true for CI environments.",
    inputSchema: {
      type: "object",
      properties: {
        installCi: {
          type: "boolean",
          description: "Install for CI environment",
          default: false,
        },
      },
      required: [],
    },
  },
  {
    name: "architect_engine_start",
    description:
      "Start the Architect Engine as a background process.",
    inputSchema: {
      type: "object",
      properties: {},
      required: [],
    },
  },
  {
    name: "architect_engine_stop",
    description:
      "Stop the running Architect Engine.",
    inputSchema: {
      type: "object",
      properties: {},
      required: [],
    },
  },
  {
    name: "architect_engine_clean",
    description:
      "Clean all Architect Engine data (removes ~/.architect-engine directory).",
    inputSchema: {
      type: "object",
      properties: {},
      required: [],
    },
  },
];

/**
 * Handle tool execution
 */
async function handleToolCall(name: string, args: any): Promise<any> {
  switch (name) {
    case "architect_list_projects": {
      const projects = await engineApiRequest("GET", "/api/projects");
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(projects, null, 2),
          },
        ],
      };
    }

    case "architect_register_project": {
      const { name: projectName, path } = args;
      const project = await engineApiRequest("POST", "/api/projects", {
        name: projectName,
        path,
      });
      return {
        content: [
          {
            type: "text",
            text: `Project registered successfully:\n${JSON.stringify(project, null, 2)}`,
          },
        ],
      };
    }

    case "architect_list_tasks": {
      const { projectName } = args;
      const tasks = await engineApiRequest(
        "GET",
        `/api/projects/${projectName}/tasks`
      );
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(tasks, null, 2),
          },
        ],
      };
    }

    case "architect_get_task": {
      const { projectName, taskName } = args;
      const task = await engineApiRequest(
        "GET",
        `/api/projects/${projectName}/tasks/${taskName}`
      );
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(task, null, 2),
          },
        ],
      };
    }

    case "architect_execute_task": {
      const { taskName, args: taskArgs = [], projectPath, plain = true } = args;
      const command = [taskName, ...(taskArgs || [])];
      
      try {
        const result = await executeArchitectCommand(command, {
          cwd: projectPath,
          plain,
        });
        
        return {
          content: [
            {
              type: "text",
              text: `Task executed successfully:\n\n${result.stdout}${
                result.stderr ? `\n\nStderr:\n${result.stderr}` : ""
              }`,
            },
          ],
        };
      } catch (error: any) {
        return {
          content: [
            {
              type: "text",
              text: `Task execution failed:\n${error.message}`,
            },
          ],
          isError: true,
        };
      }
    }

    case "architect_engine_status": {
      try {
        const projects = await engineApiRequest("GET", "/api/projects");
        return {
          content: [
            {
              type: "text",
              text: `Engine is running at ${ENGINE_URL}\nRegistered projects: ${projects.length}`,
            },
          ],
        };
      } catch (error: any) {
        return {
          content: [
            {
              type: "text",
              text: `Engine is not accessible: ${error.message}`,
            },
          ],
          isError: true,
        };
      }
    }

    case "architect_engine_install": {
      const { installCi = false } = args;
      const subcommand = installCi ? "install-ci" : "install";
      
      try {
        const result = await executeArchitectCommand(["engine", subcommand]);
        return {
          content: [
            {
              type: "text",
              text: `Engine installation initiated:\n${result.stdout}`,
            },
          ],
        };
      } catch (error: any) {
        return {
          content: [
            {
              type: "text",
              text: `Engine installation failed:\n${error.message}`,
            },
          ],
          isError: true,
        };
      }
    }

    case "architect_engine_start": {
      try {
        const result = await executeArchitectCommand(["engine", "start"]);
        return {
          content: [
            {
              type: "text",
              text: `Engine started:\n${result.stdout}`,
            },
          ],
        };
      } catch (error: any) {
        return {
          content: [
            {
              type: "text",
              text: `Failed to start engine:\n${error.message}`,
            },
          ],
          isError: true,
        };
      }
    }

    case "architect_engine_stop": {
      try {
        const result = await executeArchitectCommand(["engine", "stop"]);
        return {
          content: [
            {
              type: "text",
              text: `Engine stopped:\n${result.stdout}`,
            },
          ],
        };
      } catch (error: any) {
        return {
          content: [
            {
              type: "text",
              text: `Failed to stop engine:\n${error.message}`,
            },
          ],
          isError: true,
        };
      }
    }

    case "architect_engine_clean": {
      try {
        const result = await executeArchitectCommand(["engine", "clean"]);
        return {
          content: [
            {
              type: "text",
              text: `Engine data cleaned:\n${result.stdout}`,
            },
          ],
        };
      } catch (error: any) {
        return {
          content: [
            {
              type: "text",
              text: `Failed to clean engine data:\n${error.message}`,
            },
          ],
          isError: true,
        };
      }
    }

    default:
      throw new Error(`Unknown tool: ${name}`);
  }
}

/**
 * Main server initialization
 */
async function main() {
  const server = new Server(
    {
      name: "architect-mcp-server",
      version: "1.0.0",
    },
    {
      capabilities: {
        tools: {},
      },
    }
  );

  // Handle tool listing
  server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools,
  }));

  // Handle tool execution
  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    return await handleToolCall(name, args || {});
  });

  // Start server with stdio transport
  const transport = new StdioServerTransport();
  await server.connect(transport);

  console.error("Architect MCP Server running on stdio");
}

main().catch((error) => {
  console.error("Fatal error:", error);
  process.exit(1);
});
