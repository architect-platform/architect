interface ErrorBannerProps {
  message: string
  onRetry: () => void
}

export function ErrorBanner({ message, onRetry }: ErrorBannerProps) {
  return (
    <div className="error-banner" role="alert">
      <span>⚠️ {message}</span>
      <button onClick={onRetry}>Retry</button>
    </div>
  )
}
