export type EngineStatus = 'ACTIVE' | 'INACTIVE' | 'OFFLINE'

export type ExecutionStatus = 'STARTED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'SKIPPED'

export interface EngineInstance {
  id: string
  hostname: string
  port: number
  version: string | null
  status: EngineStatus
  createdAt: string
  lastHeartbeat: string
}

export interface Project {
  id: string
  name: string
  path: string
  engineId: string
  description: string | null
  createdAt: string
}

export interface Execution {
  id: string
  projectId: string
  engineId: string
  taskId: string
  status: ExecutionStatus
  message: string | null
  errorDetails: string | null
  startedAt: string
  completedAt: string | null
}

export interface CloudDashboardData {
  engines: EngineInstance[]
  projects: Project[]
  executions: Execution[]
}

export interface CloudDashboardState extends CloudDashboardData {
  loading: boolean
  error: string | null
  lastUpdated: string | null
}

export interface CloudDashboardStats {
  activeEngines: number
  totalProjects: number
  runningExecutions: number
  totalExecutions: number
}

export const EMPTY_CLOUD_DASHBOARD_DATA: CloudDashboardData = {
  engines: [],
  projects: [],
  executions: []
}

export const EMPTY_CLOUD_DASHBOARD_STATE: CloudDashboardState = {
  ...EMPTY_CLOUD_DASHBOARD_DATA,
  loading: true,
  error: null,
  lastUpdated: null
}

export function computeDashboardStats(
  engines: EngineInstance[],
  projects: Project[],
  executions: Execution[]
): CloudDashboardStats {
  return {
    activeEngines: engines.filter((engine) => engine.status === 'ACTIVE').length,
    totalProjects: projects.length,
    runningExecutions: executions.filter((execution) =>
      execution.status === 'STARTED' || execution.status === 'RUNNING'
    ).length,
    totalExecutions: executions.length
  }
}
