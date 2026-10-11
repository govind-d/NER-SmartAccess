import { useEffect, useState } from 'react'
import { incidentsApi } from '../services/endpoints'
import { errorMessage } from '../services/api'
import { newClientUuid, offlineDb } from '../utils/offlineDb'
import { useOfflineSync } from '../hooks/useOfflineSync'
import { EmptyState, PageHeader, StatusBadge } from '../components/ui'
import { IconPin, IconWifiOff } from '../components/icons'
import type { IncidentType, Severity } from '../types'
import { formatDateTime, humanise } from '../utils/format'

const INCIDENT_TYPES: IncidentType[] = [
  'LANDSLIDE', 'FLOOD', 'ROAD_DAMAGE', 'BRIDGE_DAMAGE',
  'TRAFFIC_CONGESTION', 'ACCIDENT', 'OTHER',
]

/**
 * Severity, with its consequence stated next to it.
 *
 * An officer choosing CRITICAL is closing a national highway. That is not something to
 * discover afterwards, so each option says what the backend will do with it.
 */
const SEVERITIES: { value: Severity; effect: string }[] = [
  { value: 'LOW', effect: 'logged only' },
  { value: 'MEDIUM', effect: 'marks the road partially accessible' },
  { value: 'HIGH', effect: 'marks the road high risk' },
  { value: 'CRITICAL', effect: 'closes the road immediately' },
]

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
  const [message, setMessage] =
    useState<{ kind: 'ok' | 'queued' | 'error'; text: string } | null>(null)

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
    <div className="space-y-5 max-w-4xl">
      <PageHeader
        title="Report an incident"
        subtitle="Works without a network. Reports are stored on this device and sent when the signal returns."
        badge={
          online ? (
            <span className="badge bg-emerald-50 text-emerald-700 border-emerald-200">
              <span className="dot bg-emerald-500" />
              Online
            </span>
          ) : (
            <span className="badge bg-slate-800 text-slate-100 border-slate-700">
              <IconWifiOff width={12} height={12} />
              Offline
            </span>
          )
        }
      />

      {message && (
        <div
          className={
            'rounded-xl border px-3.5 py-2.5 text-sm ' +
            (message.kind === 'ok'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
              : message.kind === 'queued'
                ? 'bg-amber-50 border-amber-200 text-amber-900'
                : 'bg-red-50 border-red-200 text-red-700')
          }
        >
          {message.text}
        </div>
      )}

      <form onSubmit={submit} className="card space-y-6">
        {/* ---------------- what ---------------- */}
        <div>
          <p className="eyebrow mb-2.5">What has happened</p>
          <div className="flex flex-wrap gap-2">
            {INCIDENT_TYPES.map((type) => (
              <button
                key={type}
                type="button"
                onClick={() => setIncidentType(type)}
                className={
                  'badge transition ' +
                  (incidentType === type
                    ? 'bg-slate-900 text-white border-slate-900'
                    : 'bg-white text-slate-600 border-slate-300 hover:border-slate-400')
                }
              >
                {humanise(type)}
              </button>
            ))}
          </div>
        </div>

        {/* ---------------- how bad ---------------- */}
        <div>
          <p className="eyebrow mb-2.5">How serious</p>
          <div className="grid sm:grid-cols-2 gap-2">
            {SEVERITIES.map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => setSeverity(option.value)}
                className={
                  'text-left rounded-xl border px-3 py-2.5 transition ' +
                  (severity === option.value
                    ? 'border-slate-900 bg-slate-50 ring-2 ring-slate-900/10'
                    : 'border-slate-200 hover:border-slate-300')
                }
              >
                <StatusBadge value={option.value} />
                <p className="mt-1.5 text-xs text-slate-500">{option.effect}</p>
              </button>
            ))}
          </div>
        </div>

        {/* ---------------- description ---------------- */}
        <div>
          <label className="label" htmlFor="description">Description</label>
          <textarea
            id="description"
            className="input h-24"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Hillside collapsed across both lanes near km 14, about 20 m of debris"
          />
        </div>

        {/* ---------------- where ---------------- */}
        <div>
          <p className="eyebrow mb-2.5">Where</p>
          <div className="grid sm:grid-cols-3 gap-3 items-end">
            <div>
              <label className="label" htmlFor="lat">Latitude</label>
              <input id="lat" className="input tabular" value={latitude}
                     onChange={(e) => setLatitude(e.target.value)} required />
            </div>
            <div>
              <label className="label" htmlFor="lon">Longitude</label>
              <input id="lon" className="input tabular" value={longitude}
                     onChange={(e) => setLongitude(e.target.value)} required />
            </div>
            <button type="button" className="btn-ghost py-2.5" onClick={locate}
                    disabled={locating}>
              <IconPin />
              {locating ? 'Locating...' : 'Use my location'}
            </button>
          </div>
          <p className="mt-1.5 text-xs text-slate-500">
            The server matches these coordinates to the nearest monitored road within
            500 m, so you do not need to know the road code.
          </p>
        </div>

        {/* ---------------- photo ---------------- */}
        <div>
          <label className="label" htmlFor="photo">Photo (optional)</label>
          <div className="flex items-center gap-3">
            <input
              id="photo"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              className="input"
              onChange={(e) => setPhoto(e.target.files?.[0] ?? null)}
            />
            {photo && (
              <img
                src={URL.createObjectURL(photo)}
                alt=""
                className="w-14 h-14 rounded-xl object-cover border border-slate-200 shrink-0"
              />
            )}
          </div>
          <p className="mt-1.5 text-xs text-slate-500">
            Photos are only sent when online; the text report is queued either way.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 pt-1">
          <button className="btn-primary px-6 py-2.5" disabled={busy || coordinatesMissing}>
            {busy ? 'Submitting...' : online ? 'Submit report' : 'Save on this device'}
          </button>
          {coordinatesMissing && (
            <span className="text-xs text-amber-700">
              Coordinates are required - press "Use my location"
            </span>
          )}
        </div>
      </form>

      {/* ---------------- the queue ---------------- */}
      <div className="card p-0 overflow-hidden">
        <div className="px-5 py-3.5 flex items-center justify-between border-b
                        border-slate-100">
          <div className="flex items-center gap-2">
            <h2 className="panel-title">Waiting to sync</h2>
            {pendingCount > 0 && (
              <span className="badge bg-amber-50 text-amber-700 border-amber-200 tabular">
                {pendingCount}
              </span>
            )}
          </div>
          <button className="btn-ghost py-1 px-2.5 text-xs" onClick={() => void sync()}
                  disabled={!online || syncing || pendingCount === 0}>
            {syncing ? 'Syncing...' : 'Sync now'}
          </button>
        </div>

        {pending.length === 0 ? (
          <EmptyState
            title="Nothing pending"
            body="Every report on this device has reached the server."
          />
        ) : (
          <ul className="divide-y divide-slate-100">
            {pending.map((report) => (
              <li key={report.clientUuid} className="px-5 py-3 flex items-start gap-3">
                <span className="dot bg-amber-500 mt-1.5 shrink-0" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-slate-900">
                    {humanise(report.reportType)}
                    <span className="font-normal text-slate-500 tabular">
                      {' '}at {report.latitude.toFixed(4)}, {report.longitude.toFixed(4)}
                    </span>
                  </p>
                  <p className="text-xs text-slate-400">
                    captured {formatDateTime(report.capturedAt)}
                    {report.lastError ? ` · last error: ${report.lastError}` : ''}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        )}

        <div className="px-5 py-3 border-t border-slate-100 bg-slate-50/50 text-xs
                        text-slate-500">
          Queued in IndexedDB and keyed by a client-generated UUID, so re-sending the
          same report can never create a duplicate.
        </div>
      </div>
    </div>
  )
}
