import { useEffect, useState } from 'react'
import { incidentsApi } from '../services/endpoints'
import { errorMessage } from '../services/api'
import { newClientUuid, offlineDb } from '../utils/offlineDb'
import { useOfflineSync } from '../hooks/useOfflineSync'
import type { IncidentType, Severity } from '../types'
import { formatDateTime, humanise } from '../utils/format'

const INCIDENT_TYPES: IncidentType[] = [
  'LANDSLIDE', 'FLOOD', 'ROAD_DAMAGE', 'BRIDGE_DAMAGE',
  'TRAFFIC_CONGESTION', 'ACCIDENT', 'OTHER',
]
const SEVERITIES: Severity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

/**
 * The field officer's form - the offline-first heart of the application.
 *
 * What makes it work without a network:
 *  - the phone's GPS fills in the coordinates, so no map or address lookup is needed;
 *  - a clientUuid is generated before anything is sent, making a retry harmless;
 *  - if the submit fails or the device is offline, the report goes into IndexedDB and
 *    the queue is flushed automatically when the signal returns.
 *
 * The officer is always told which of the two happened. "Did it send?" is the one
 * question this screen must never leave unanswered.
 */
export default function ReportIncident() {
  const { online, pending, pendingCount, sync, syncing, refreshPending } = useOfflineSync()

  const [incidentType, setIncidentType] = useState<IncidentType>('LANDSLIDE')
  const [severity, setSeverity] = useState<Severity>('HIGH')
  const [description, setDescription] = useState('')
  const [latitude, setLatitude] = useState<string>('')
  const [longitude, setLongitude] = useState<string>('')
  const [photo, setPhoto] = useState<File | null>(null)
  const [locating, setLocating] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<{ kind: 'ok' | 'queued' | 'error'; text: string } | null>(null)

  useEffect(() => {
    locate()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function locate() {
    if (!navigator.geolocation) {
      return
    }
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLatitude(position.coords.latitude.toFixed(6))
        setLongitude(position.coords.longitude.toFixed(6))
        setLocating(false)
      },
      () => setLocating(false),
      { enableHighAccuracy: true, timeout: 10000 },
    )
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setMessage(null)

    const clientUuid = newClientUuid()
    const capturedAt = new Date().toISOString()
    const payload = {
      clientUuid,
      incidentType,
      severity,
      description,
      latitude: Number(latitude),
      longitude: Number(longitude),
      occurredAt: capturedAt,
    }

    // Offline: never even attempt the request, just queue it.
    if (!navigator.onLine) {
      await queueLocally(clientUuid, capturedAt)
      setBusy(false)
      return
    }

    try {
      if (photo) {
        await incidentsApi.reportWithPhoto(payload, photo)
      } else {
        await incidentsApi.report(payload)
      }
      setMessage({ kind: 'ok', text: 'Report submitted. Officials have been alerted.' })
      resetForm()
    } catch (err) {
      // The request failed even though the browser thought it was online - a weak signal,
      // a timeout, a server restart. Queue it rather than losing it.
      await queueLocally(clientUuid, capturedAt, errorMessage(err))
    } finally {
      setBusy(false)
      void refreshPending()
    }
  }

  async function queueLocally(clientUuid: string, capturedAt: string, reason?: string) {
    await offlineDb.queue({
      clientUuid,
      reportType: incidentType,
      description,
      latitude: Number(latitude),
      longitude: Number(longitude),
      capturedAt,
    })
    setMessage({
      kind: 'queued',
      text: reason
        ? 'Could not reach the server, so the report is saved on this device and will sync automatically.'
        : 'You are offline. The report is saved on this device and will sync automatically.',
    })
    resetForm()
    void refreshPending()
  }

  function resetForm() {
    setDescription('')
    setPhoto(null)
  }

  const coordinatesMissing = !latitude || !longitude

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
          Report an incident
        </h1>
        <p className="text-sm text-slate-500">
          Works without a network. Reports are stored on this device and sent when the
          signal returns.
        </p>
      </div>

      {message && (
        <div
          className={
            'rounded-lg border px-3 py-2 text-sm ' +
            (message.kind === 'ok'
              ? 'bg-green-50 border-green-200 text-green-800'
              : message.kind === 'queued'
                ? 'bg-amber-50 border-amber-200 text-amber-900'
                : 'bg-red-50 border-red-200 text-red-700')
          }
        >
          {message.text}
        </div>
      )}

      <form onSubmit={submit} className="card space-y-4">
        <div className="grid sm:grid-cols-2 gap-4">
          <div>
            <label className="label" htmlFor="type">Incident type</label>
            <select
              id="type"
              className="input"
              value={incidentType}
              onChange={(e) => setIncidentType(e.target.value as IncidentType)}
            >
              {INCIDENT_TYPES.map((type) => (
                <option key={type} value={type}>{humanise(type)}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="label" htmlFor="severity">Severity</label>
            <select
              id="severity"
              className="input"
              value={severity}
              onChange={(e) => setSeverity(e.target.value as Severity)}
            >
              {SEVERITIES.map((level) => (
                <option key={level} value={level}>{humanise(level)}</option>
              ))}
            </select>
            <p className="mt-1 text-xs text-slate-500">
              CRITICAL closes the road automatically; HIGH marks it high risk.
            </p>
          </div>
        </div>

        <div>
          <label className="label" htmlFor="description">What has happened</label>
          <textarea
            id="description"
            className="input h-24"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Hillside collapsed across both lanes near km 14, about 20 m of debris"
          />
        </div>

        <div className="grid sm:grid-cols-3 gap-4 items-end">
          <div>
            <label className="label" htmlFor="lat">Latitude</label>
            <input id="lat" className="input" value={latitude}
                   onChange={(e) => setLatitude(e.target.value)} required />
          </div>
          <div>
            <label className="label" htmlFor="lon">Longitude</label>
            <input id="lon" className="input" value={longitude}
                   onChange={(e) => setLongitude(e.target.value)} required />
          </div>
          <button type="button" className="btn-ghost justify-center" onClick={locate}
                  disabled={locating}>
            {locating ? 'Locating...' : 'Use my location'}
          </button>
        </div>

        <div>
          <label className="label" htmlFor="photo">Photo (optional)</label>
          <input
            id="photo"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            className="input"
            onChange={(e) => setPhoto(e.target.files?.[0] ?? null)}
          />
          <p className="mt-1 text-xs text-slate-500">
            Photos are only sent when online; the text report is queued either way.
          </p>
        </div>

        <div className="flex items-center gap-3 pt-2">
          <button className="btn-primary" disabled={busy || coordinatesMissing}>
            {busy ? 'Submitting...' : online ? 'Submit report' : 'Save for later'}
          </button>
          <span className={'text-xs ' + (online ? 'text-green-600' : 'text-amber-600')}>
            {online ? 'Online' : 'Offline - reports will be queued'}
          </span>
        </div>
      </form>

      {/* The queue, visible so the officer can see exactly what has not gone up yet. */}
      <div className="card">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-slate-900">
            Waiting to sync ({pendingCount})
          </h2>
          <button className="btn-ghost py-1 text-xs" onClick={() => void sync()}
                  disabled={!online || syncing || pendingCount === 0}>
            {syncing ? 'Syncing...' : 'Sync now'}
          </button>
        </div>

        {pending.length === 0 ? (
          <p className="mt-3 text-sm text-slate-500">
            Nothing pending. Every report on this device has reached the server.
          </p>
        ) : (
          <ul className="mt-3 divide-y divide-slate-100">
            {pending.map((report) => (
              <li key={report.clientUuid} className="py-2 text-sm">
                <span className="font-medium">{humanise(report.reportType)}</span>
                <span className="text-slate-500">
                  {' '}at {report.latitude.toFixed(4)}, {report.longitude.toFixed(4)}
                </span>
                <span className="block text-xs text-slate-400">
                  captured {formatDateTime(report.capturedAt)}
                  {report.lastError ? ' - last error: ' + report.lastError : ''}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
