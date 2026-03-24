import './App.css'
import { ErrorBanner } from './components/ErrorBanner'
import { SummaryStats } from './components/SummaryStats'
import { useCloudDashboard } from './hooks/useCloudDashboard'

function App() {
  const { state, stats, refresh } = useCloudDashboard()

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

        {state.error && <ErrorBanner message={state.error} onRetry={() => void refresh()} />}

        {state.loading ? (
          <p className="status-message">Loading cloud monitoring data...</p>
        ) : (
          <SummaryStats lastUpdated={state.lastUpdated} stats={stats} />
        )}
      </div>
    </div>
  )
}

export default App
