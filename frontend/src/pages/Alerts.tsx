import { useCallback, useEffect, useState } from 'react'
import { alertsApi } from '../services/endpoints'
import { useAuth } from '../context/AuthContext'
import { useWebSocket } from '../hooks/useWebSocket'
import { errorMessage } from '../services/api'
import type { Alert } from '../types'
import { formatDateTime, formatRelative, humanise, severityBadge } from '../utils/format'

export default function Alerts() {
  const { hasRole } = useAuth()
  const canManage = hasRole('ADMIN', 'AUTHORITY_OFFICIAL')

  const [alerts, setAlerts] = useState<Alert[]>([])
  const [showInactive, setShowInactive] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    alertsApi.list({ active: !showInactive, size: 50 })
      .then((page) => setAlerts(page.content))
      .catch((err) => setError(errorMessage(err)))
  }, [showInactive])

  useEffect(load, [load])

  useWebSocket<Alert>('/topic/alerts', (alert) => {
    setAlerts((current) => [alert, ...current.filter((a) => a.id !== alert.id)])
  })

  async function deactivate(id: number) {
    try {
      await alertsApi.deactivate(id)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Alerts</h1>
          <p className="text-sm text-slate-500">
            Everything the platform decided people needed to know about
          </p>
        </div>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={showInactive}
                 onChange={(e) => setShowInactive(e.target.checked)} />
          Show closed alerts
        </label>
      </div>

      {error && (
        <div className="rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="space-y-3">
        {alerts.map((alert) => (
          <div key={alert.id} className="card flex items-start gap-4">
            <span className={'badge shrink-0 ' + severityBadge(alert.severity)}>
              {alert.severity}
            </span>

            <div className="min-w-0 flex-1">
              <p className="font-semibold text-slate-900">{alert.title}</p>
              <p className="mt-1 text-sm text-slate-600">{alert.message}</p>
              <p className="mt-2 text-xs text-slate-400">
                {humanise(alert.alertType)}
                {alert.districtName ? ' - ' + alert.districtName : ''}
                {alert.roadCode ? ' - ' + alert.roadCode : ''}
                {' - raised ' + formatRelative(alert.createdAt)}
                {alert.expiresAt ? ' - expires ' + formatDateTime(alert.expiresAt) : ''}
              </p>
            </div>

            {canManage && alert.active && (
              <button className="btn-ghost py-1 text-xs shrink-0"
                      onClick={() => void deactivate(alert.id)}>
                Close
              </button>
            )}
          </div>
        ))}

        {alerts.length === 0 && (
          <div className="card text-sm text-slate-500">
            No alerts. Every corridor is behaving.
          </div>
        )}
      </div>
    </div>
  )
}
