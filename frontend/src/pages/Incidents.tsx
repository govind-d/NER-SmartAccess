import { useCallback, useEffect, useState } from 'react'
import { incidentsApi } from '../services/endpoints'
import { useAuth } from '../context/AuthContext'
import { useWebSocket } from '../hooks/useWebSocket'
import { errorMessage } from '../services/api'
import type { Incident, IncidentStatus, IncidentType, Severity } from '../types'
import { formatDateTime, formatRelative, humanise, severityBadge } from '../utils/format'

const TYPES: IncidentType[] = [
  'LANDSLIDE', 'FLOOD', 'ROAD_DAMAGE', 'BRIDGE_DAMAGE',
  'TRAFFIC_CONGESTION', 'ACCIDENT', 'OTHER',
]
const STATUSES: IncidentStatus[] = [
  'REPORTED', 'VERIFIED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED',
]

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
    try {
      await incidentsApi.verify(id)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Incidents</h1>
        <p className="text-sm text-slate-500">
          Disruptions reported from the field, newest first
        </p>
      </div>

      <div className="flex flex-wrap gap-3">
        <select className="input w-48" value={type}
                onChange={(e) => setType(e.target.value as IncidentType | '')}>
          <option value="">All types</option>
          {TYPES.map((t) => <option key={t} value={t}>{humanise(t)}</option>)}
        </select>
        <select className="input w-48" value={status}
                onChange={(e) => setStatus(e.target.value as IncidentStatus | '')}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{humanise(s)}</option>)}
        </select>
        <select className="input w-40" value={severity}
                onChange={(e) => setSeverity(e.target.value as Severity | '')}>
          <option value="">All severities</option>
          {(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as Severity[]).map((s) => (
            <option key={s} value={s}>{humanise(s)}</option>
          ))}
        </select>
      </div>

      {error && (
        <div className="rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
        {incidents.map((incident) => (
          <div key={incident.id} className="card">
            <div className="flex items-start justify-between gap-2">
              <span className={'badge ' + severityBadge(incident.severity)}>
                {incident.severity}
              </span>
              <span className="text-xs text-slate-400">
                {formatRelative(incident.occurredAt)}
              </span>
            </div>

            <h3 className="mt-2 font-semibold text-slate-900">
              {humanise(incident.incidentType)}
            </h3>
            <p className="mt-1 text-sm text-slate-600">
              {incident.description || 'No description given'}
            </p>

            <dl className="mt-3 text-xs text-slate-500 space-y-0.5">
              <div>Road: {incident.roadName ?? 'not matched to a monitored road'}</div>
              <div>District: {incident.districtName ?? 'unknown'}</div>
              <div>
                Location: {incident.latitude.toFixed(4)}, {incident.longitude.toFixed(4)}
              </div>
              <div>Reported by: {incident.reportedByName ?? 'unknown'}</div>
              <div>Occurred: {formatDateTime(incident.occurredAt)}</div>
            </dl>

            {incident.photoUrl && (
              <img
                src={incident.photoUrl}
                alt="Incident"
                className="mt-3 rounded-lg border border-slate-200 max-h-40 w-full object-cover"
              />
            )}

            <div className="mt-3 flex items-center justify-between">
              <span className="badge bg-slate-100 text-slate-700 border-slate-300">
                {humanise(incident.status)}
              </span>
              {canVerify && incident.status === 'REPORTED' && (
                <button className="btn-ghost py-1 text-xs"
                        onClick={() => void verify(incident.id)}>
                  Verify
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {!loading && incidents.length === 0 && (
        <div className="card text-sm text-slate-500">
          No incidents match these filters.
        </div>
      )}
    </div>
  )
}
