import { useCallback, useEffect, useState } from 'react'
import './App.css'

const API_BASE_URL = 'http://localhost:8080/api'
const REFRESH_INTERVAL = 5000

function App() {
  const [engines, setEngines] = useState([])
  const [projects, setProjects] = useState([])
  const [executions, setExecutions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fetchData = useCallback(async () => {
    try {
      const enginesRes = await fetch(`${API_BASE_URL}/engines`)
      if (!enginesRes.ok) throw new Error('Failed to fetch engines')
      const enginesData = await enginesRes.json()
      setEngines(enginesData)

      const projectsRes = await fetch(`${API_BASE_URL}/projects`)
      if (!projectsRes.ok) throw new Error('Failed to fetch projects')
      const projectsData = await projectsRes.json()
      setProjects(projectsData)

      const allExecutions = []
      for (const engine of enginesData) {
        try {
          const execRes = await fetch(`${API_BASE_URL}/executions/engine/${engine.id}`)
          if (execRes.ok) {
            const execData = await execRes.json()
            allExecutions.push(...execData)
          }
        } catch (err) {
          console.error(`Failed to fetch executions for engine ${engine.id}:`, err)
        }
      }

      allExecutions.sort((a, b) => new Date(b.startedAt) - new Date(a.startedAt))
      setExecutions(allExecutions)

      setError(null)
    } catch (err) {
      console.error('Error fetching data:', err)
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void fetchData()
    const interval = setInterval(() => {
      void fetchData()
    }, REFRESH_INTERVAL)
    return () => clearInterval(interval)
  }, [fetchData])

  const stats = {
    activeEngines: engines.filter(e => e.status === 'ACTIVE').length,
    totalProjects: projects.length,
    runningExecutions: executions.filter(e => 
      ['STARTED', 'RUNNING'].includes(e.status)
    ).length,
    totalExecutions: executions.length
  }

  return (
    <div className="app">
      <div className="container">
        <header className="page-header">
          <p className="eyebrow">Architect Cloud UI</p>
          <h1>Incubating monitoring stub</h1>
          <p className="description">
            This frontend currently polls the cloud backend REST API and surfaces a lightweight summary
            while richer dashboard workflows are still in progress.
          </p>
        </header>

        {error && (
          <div className="error-banner">
            <span>⚠️ {error}</span>
            <button onClick={fetchData}>Retry</button>
          </div>
        )}

        {loading ? (
          <p className="status-message">Loading cloud monitoring data...</p>
        ) : (
          <section className="stats-grid" aria-label="Cloud monitoring summary">
            <article className="stat-card" data-testid="stat-active-engines">
              <span className="stat-label">Active engines</span>
              <strong className="stat-value">{stats.activeEngines}</strong>
            </article>
            <article className="stat-card" data-testid="stat-total-projects">
              <span className="stat-label">Tracked projects</span>
              <strong className="stat-value">{stats.totalProjects}</strong>
            </article>
            <article className="stat-card" data-testid="stat-running-executions">
              <span className="stat-label">Running executions</span>
              <strong className="stat-value">{stats.runningExecutions}</strong>
            </article>
            <article className="stat-card" data-testid="stat-total-executions">
              <span className="stat-label">Total executions</span>
              <strong className="stat-value">{stats.totalExecutions}</strong>
            </article>
          </section>
        )}
      </div>
    </div>
  )
}

export default App
