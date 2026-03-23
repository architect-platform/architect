export const JSON_RPC_VERSION = "2.0";
export const APP_VERSION = "1.0.0";

export const METHODS = {
  init: "init",
  listTasks: "listTasks",
  executeTask: "executeTask",
  shutdown: "shutdown",
} as const;

export const EVENT_TYPES = {
  output: "output",
  progress: "progress",
  error: "error",
  completed: "completed",
} as const;

export type JsonPrimitive = string | number | boolean | null;
export type JsonValue = JsonPrimitive | JsonObject | JsonArray;
export interface JsonObject {
  [key: string]: JsonValue;
}
export type JsonArray = JsonValue[];

export interface JsonRpcRequest<TParams = unknown> {
  jsonrpc: typeof JSON_RPC_VERSION;
  id: number;
  method: string;
  params?: TParams;
}

export interface JsonRpcError {
  code: number;
  message: string;
  data?: JsonValue;
}

export interface JsonRpcResponse<TResult = unknown> {
  jsonrpc: typeof JSON_RPC_VERSION;
  id: number | null;
  result?: TResult;
  error?: JsonRpcError;
}

export interface InitParams {
  config?: JsonObject;
  protocol_version: string;
}

export interface InitResult {
  ok: boolean;
  name?: string;
  version?: string;
}

export interface TaskDescriptor {
  id: string;
  description?: string;
  phase?: string;
  dependencies?: string[];
  permissions?: string[];
  requires_confirmation?: boolean;
}

export interface ExecuteTaskParams {
  id: string;
  args?: string[];
  env?: Record<string, string>;
}

export interface TaskEvent {
  type: (typeof EVENT_TYPES)[keyof typeof EVENT_TYPES];
  data?: string;
  progress?: number;
  exitCode?: number;
}

export const JSON_RPC_ERROR = {
  parseError: -32700,
  invalidRequest: -32600,
  methodNotFound: -32601,
  internalError: -32603,
} as const;