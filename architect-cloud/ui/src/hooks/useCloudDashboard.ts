import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchCloudDashboardData } from '../api/cloudApi'
import {
  computeDashboardStats,
  EMPTY_CLOUD_DASHBOARD_STATE,
  type CloudDashboardState
} from '../types/cloud'

const REFRESH_INTERVAL = 5000

export interface UseCloudDashboardResult {
  state: CloudDashboardState
  stats: ReturnType<typeof computeDashboardStats>
  refresh: () => Promise<void>
}

export function useCloudDashboard(): UseCloudDashboardResult {
  const [state, setState] = useState<CloudDashboardState>(EMPTY_CLOUD_DASHBOARD_STATE)

  const refresh = useCallback(async () => {
    try {
      const data = await fetchCloudDashboardData()
      setState({
        ...data,
        loading: false,
        error: null,
        lastUpdated: new Date().toISOString()
      })
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unknown cloud UI error'
      setState((currentState) => ({
        ...currentState,
        loading: false,
        error: message
      }))
      console.error('Error fetching cloud dashboard data:', error)
    }
  }, [])

  useEffect(() => {
    void refresh()
    const intervalId = window.setInterval(() => {
      void refresh()
    }, REFRESH_INTERVAL)

    return () => window.clearInterval(intervalId)
  }, [refresh])

  const stats = useMemo(
    () => computeDashboardStats(state.engines, state.projects, state.executions),
    [state.engines, state.executions, state.projects]
  )

  return {
    state,
    stats,
    refresh
  }
}
