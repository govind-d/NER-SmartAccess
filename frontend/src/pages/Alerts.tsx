import { useCallback, useEffect, useState } from 'react'
import { alertsApi } from '../services/endpoints'
import { useAuth } from '../context/AuthContext'
import { useWebSocket } from '../hooks/useWebSocket'
import { errorMessage } from '../services/api'
import {
  EmptyState, ErrorNote, LiveBadge, PageHeader, StatusBadge,
} from '../components/ui'
import { IconBell } from '../components/icons'
import type { Alert } from '../types'
import { formatDateTime, formatRelative, humanise } from '../utils/format'

/**
 * The alert register.
 *
 * Each alert is a card with a severity stripe down its left edge, so the page can be
 * triaged by colour at a glance before any of it is read. New alerts arrive over
 * WebSocket and are prepended live.
 */
const STRIPE: Record<string, string> = {
  CRITICAL: 'border-l-red-500',
  HIGH: 'border-l-orange-500',
  MEDIUM: 'border-l-amber-400',
  LOW: 'border-l-slate-300',
}

export default function Alerts() {
  const { hasRole } = useAuth()
  const canManage = hasRole('ADMIN', 'AUTHORITY_OFFICIAL')

  const [alerts, setAlerts] = useState<Alert[]>([])
  const [showInactive, setShowInactive] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(() => {
    setLoading(true)
    alertsApi.list({ active: !showInactive, size: 50 })
      .then((page) => setAlerts(page.content))
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false))
  }, [showInactive])

  useEffect(load, [load])

  useWebSocket<Alert>('/topic/alerts', (alert) => {
    setAlerts((current) => [alert, ...current.filter((a) => a.id !== alert.id)])
  })

  async function deactivate(id: number) {
    setError(null)
    try {
      await alertsApi.deactivate(id)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  const critical = alerts.filter((a) => a.severity === 'CRITICAL').length

  return (
    <div className="space-y-5">
      <PageHeader
        title="Alerts"
        subtitle={
          critical > 0
            ? `${alerts.length} alert(s) · ${critical} critical`
            : 'Everything the platform decided people needed to know about'
        }
        badge={<LiveBadge label="Pushed live" />}
        actions={
          <label className="btn-ghost cursor-pointer">
            <input
              type="checkbox"
              checked={showInactive}
              onChange={(e) => setShowInactive(e.target.checked)}
            />
            Show closed
          </label>
        }
      />

      {error && <ErrorNote message={error} />}

      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="card h-24 animate-pulse bg-slate-100/60" />
          ))}
        </div>
      ) : alerts.length === 0 ? (
        <div className="card p-0">
          <EmptyState
            icon={<IconBell />}
            title={showInactive ? 'No closed alerts' : 'No active alerts'}
            body="Every corridor is behaving. Alerts appear here the moment an incident is reported or rainfall crosses a threshold."
          />
        </div>
      ) : (
        <div className="space-y-3">
          {alerts.map((alert) => (
            <div
              key={alert.id}
              className={
                'card card-hover border-l-4 flex items-start gap-4 ' +
                (STRIPE[alert.severity] ?? STRIPE.LOW) +
                (alert.active ? '' : ' opacity-60')
              }
            >
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <StatusBadge value={alert.severity} />
                  <span className="badge bg-slate-100 text-slate-600 border-slate-200">
                    {humanise(alert.alertType)}
                  </span>
                  {!alert.active && (
                    <span className="badge bg-slate-100 text-slate-500 border-slate-200">
                      closed
                    </span>
                  )}
                </div>

                <p className="mt-2 font-semibold text-slate-900">{alert.title}</p>
                <p className="mt-1 text-sm text-slate-600">{alert.message}</p>

                <div className="mt-2.5 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-400">
                  {alert.districtName && <span>{alert.districtName}</span>}
                  {alert.roadCode && <span className="font-mono">{alert.roadCode}</span>}
                  <span>raised {formatRelative(alert.createdAt)}</span>
                  {alert.expiresAt && <span>expires {formatDateTime(alert.expiresAt)}</span>}
                </div>
              </div>

              {canManage && alert.active && (
                <button
                  className="btn-ghost py-1 px-2.5 text-xs shrink-0"
                  onClick={() => void deactivate(alert.id)}
                >
                  Close
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
