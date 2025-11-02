import { useState, useEffect } from 'react'
import './App.css'

const API_BASE_URL = 'http://localhost:8080/api'
const REFRESH_INTERVAL = 5000

function App() {
  const [engines, setEngines] = useState([])
  const [projects, setProjects] = useState([])
  const [executions, setExecutions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fetchData = async () => {
    try {
      // Fetch engines
      const enginesRes = await fetch(`${API_BASE_URL}/engines`)
      if (!enginesRes.ok) throw new Error('Failed to fetch engines')
      const enginesData = await enginesRes.json()
      setEngines(enginesData)

      // Fetch projects
      const projectsRes = await fetch(`${API_BASE_URL}/projects`)
      if (!projectsRes.ok) throw new Error('Failed to fetch projects')
      const projectsData = await projectsRes.json()
      setProjects(projectsData)

      // Fetch executions for all engines
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
      
      // Sort by most recent first
      allExecutions.sort((a, b) => new Date(b.startedAt) - new Date(a.startedAt))
      setExecutions(allExecutions)

      setError(null)
    } catch (err) {
      console.error('Error fetching data:', err)
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
    const interval = setInterval(fetchData, REFRESH_INTERVAL)
    return () => clearInterval(interval)
  }, [])

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
        {error && (
          <div className="error-banner">
            <span>⚠️ {error}</span>
            <button onClick={fetchData}>Retry</button>
          </div>
        )}

      </div>
    </div>
  )
}

export default App
