import { useCallback, useEffect, useState } from 'react'
import { incidentsApi } from '../services/endpoints'
import { useAuth } from '../context/AuthContext'
import { useWebSocket } from '../hooks/useWebSocket'
import { errorMessage } from '../services/api'
import {
  EmptyState, ErrorNote, FilterBar, LiveBadge, PageHeader, SkeletonCards, StatusBadge,
} from '../components/ui'
import { IconAlertTriangle } from '../components/icons'
import type { Incident, IncidentStatus, IncidentType, Severity } from '../types'
import { formatDateTime, formatRelative, humanise } from '../utils/format'

const TYPES: IncidentType[] = [
  'LANDSLIDE', 'FLOOD', 'ROAD_DAMAGE', 'BRIDGE_DAMAGE',
  'TRAFFIC_CONGESTION', 'ACCIDENT', 'OTHER',
]
const STATUSES: IncidentStatus[] = [
  'REPORTED', 'VERIFIED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED',
]
const SEVERITIES: Severity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

/** Severity stripe down the left edge, so the grid triages by colour before reading. */
const STRIPE: Record<Severity, string> = {
  CRITICAL: 'border-l-red-500',
  HIGH: 'border-l-orange-500',
  MEDIUM: 'border-l-amber-400',
  LOW: 'border-l-slate-300',
}

/**
 * The incident register.
 *
 * New reports arrive over WebSocket and are prepended without a refresh, which matters
 * during an active event: an official watching this page sees a landslide appear the
 * moment the field officer submits it.
 */
export default function Incidents() {
  const { hasRole } = useAuth()
  const canVerify = hasRole('ADMIN', 'AUTHORITY_OFFICIAL')

  const [incidents, setIncidents] = useState<Incident[]>([])
  const [type, setType] = useState<IncidentType | ''>('')
  const [status, setStatus] = useState<IncidentStatus | ''>('')
  const [severity, setSeverity] = useState<Severity | ''>('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(() => {
    setLoading(true)
    const params: Record<string, unknown> = { size: 50 }
    if (type) params.type = type
    if (status) params.status = status
    if (severity) params.severity = severity

    incidentsApi.search(params)
      .then((page) => setIncidents(page.content))
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false))
  }, [type, status, severity])

  useEffect(load, [load])

  useWebSocket<Incident>('/topic/incidents', (incident) => {
    setIncidents((current) => {
      const others = current.filter((existing) => existing.id !== incident.id)
      return [incident, ...others]
    })
  })

  async function verify(id: number) {
    setError(null)
    try {
      await incidentsApi.verify(id)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  const critical = incidents.filter((i) => i.severity === 'CRITICAL').length
  const filtered = Boolean(type || status || severity)

  return (
    <div className="space-y-5">
      <PageHeader
        title="Incidents"
        subtitle={
          critical > 0
            ? `${incidents.length} report(s) · ${critical} critical`
            : 'Disruptions reported from the field, newest first'
        }
        badge={<LiveBadge label="Pushed live" />}
      />

      <FilterBar>
        <select className="input w-44" value={type}
                onChange={(e) => setType(e.target.value as IncidentType | '')}>
          <option value="">All types</option>
          {TYPES.map((t) => <option key={t} value={t}>{humanise(t)}</option>)}
        </select>
        <select className="input w-40" value={status}
                onChange={(e) => setStatus(e.target.value as IncidentStatus | '')}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{humanise(s)}</option>)}
        </select>
        <select className="input w-36" value={severity}
                onChange={(e) => setSeverity(e.target.value as Severity | '')}>
          <option value="">All severities</option>
          {SEVERITIES.map((s) => <option key={s} value={s}>{humanise(s)}</option>)}
        </select>
        {filtered && (
          <button
            className="text-xs text-slate-500 hover:text-slate-900 underline underline-offset-2"
            onClick={() => { setType(''); setStatus(''); setSeverity('') }}
          >
            clear
          </button>
        )}
      </FilterBar>

      {error && <ErrorNote message={error} />}

      {loading ? (
        <SkeletonCards count={6} />
      ) : incidents.length === 0 ? (
        <div className="card p-0">
          <EmptyState
            icon={<IconAlertTriangle />}
            title={filtered ? 'No incidents match these filters' : 'No incidents reported'}
            body={
              filtered
                ? 'Clear the filters to see the whole register.'
                : 'Field officers report landslides, floods and road damage from the Report incident page.'
            }
          />
        </div>
      ) : (
        <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
          {incidents.map((incident) => (
            <div
              key={incident.id}
              className={'card card-hover border-l-4 ' + STRIPE[incident.severity]}
            >
              <div className="flex items-start justify-between gap-2">
                <StatusBadge value={incident.severity} />
                <span className="text-xs text-slate-400">
                  {formatRelative(incident.occurredAt)}
                </span>
              </div>

              <h3 className="mt-2.5 font-semibold text-slate-900">
                {humanise(incident.incidentType)}
              </h3>
              <p className="mt-1 text-sm text-slate-600 line-clamp-2">
                {incident.description || 'No description given'}
              </p>

              {incident.photoUrl && (
                <img
                  src={incident.photoUrl}
                  alt=""
                  className="mt-3 rounded-xl border border-slate-200 h-36 w-full object-cover"
                />
              )}

              <dl className="mt-3 space-y-1 text-xs">
                <div className="flex gap-2">
                  <dt className="text-slate-400 w-16 shrink-0">Road</dt>
                  <dd className="text-slate-600 truncate">
                    {incident.roadName ?? 'not matched to a monitored road'}
                  </dd>
                </div>
                <div className="flex gap-2">
                  <dt className="text-slate-400 w-16 shrink-0">District</dt>
                  <dd className="text-slate-600">{incident.districtName ?? 'unknown'}</dd>
                </div>
                <div className="flex gap-2">
                  <dt className="text-slate-400 w-16 shrink-0">Location</dt>
                  <dd className="text-slate-600 tabular">
                    {incident.latitude.toFixed(4)}, {incident.longitude.toFixed(4)}
                  </dd>
                </div>
                <div className="flex gap-2">
                  <dt className="text-slate-400 w-16 shrink-0">Reported</dt>
                  <dd className="text-slate-600 truncate">
                    {incident.reportedByName ?? 'unknown'} &middot;{' '}
                    {formatDateTime(incident.occurredAt)}
                  </dd>
                </div>
              </dl>

              <div className="mt-3.5 pt-3 border-t border-slate-100 flex items-center
                              justify-between">
                <StatusBadge value={incident.status} dot={false} />
                {canVerify && incident.status === 'REPORTED' && (
                  <button className="btn-ghost py-1 px-2.5 text-xs"
                          onClick={() => void verify(incident.id)}>
                    Verify
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
