import { useEffect, useState } from 'react'
import { districtsApi, routesApi } from '../services/endpoints'
import { errorMessage } from '../services/api'
import { NerMap } from '../components/NerMap'
import type { District, GoodsType, RouteRecommendation } from '../types'
import { humanise, riskColor } from '../utils/format'

const GOODS: GoodsType[] = [
  'MEDICINE', 'FOOD', 'AGRI_PRODUCE', 'CONSTRUCTION_MATERIAL',
  'FUEL', 'RELIEF_SUPPLIES', 'OTHER',
]

/**
 * The route planner.
 *
 * This screen is where the whole platform comes together: OSRM supplies the geometry,
 * PostGIS matches it to monitored roads, the Java risk engine scores each road, and the
 * result is a recommendation with its reasoning shown rather than hidden.
 *
 * The rejected candidates are displayed on purpose. "Why did it send me the long way
 * round?" has an answer here, which is what makes the recommendation trustworthy.
 */
export default function RoutePlanner() {
  const [districts, setDistricts] = useState<District[]>([])
  const [origin, setOrigin] = useState('')
  const [destination, setDestination] = useState('')
  const [goodsType, setGoodsType] = useState<GoodsType>('MEDICINE')
  const [avoidHighRisk, setAvoidHighRisk] = useState(true)
  const [result, setResult] = useState<RouteRecommendation | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    districtsApi.list().then((list) => {
      setDistricts(list)
      if (list.length > 1) {
        setOrigin(list[0].code)
        setDestination(list[1].code)
      }
    }).catch(() => setError('Could not load districts'))
  }, [])

  async function plan(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    setResult(null)

    const from = districts.find((d) => d.code === origin)
    const to = districts.find((d) => d.code === destination)
    if (!from?.centroidLatitude || !to?.centroidLatitude) {
      setError('Those districts have no coordinates recorded')
      setBusy(false)
      return
    }

    try {
      const recommendation = await routesApi.recommend({
        origin: { latitude: from.centroidLatitude, longitude: from.centroidLongitude },
        destination: { latitude: to.centroidLatitude, longitude: to.centroidLongitude },
        goodsType,
        avoidHighRisk,
      })
      setResult(recommendation)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
          Route planner
        </h1>
        <p className="text-sm text-slate-500">
          Risk-aware routing: blocked roads are excluded, high-risk corridors are penalised.
        </p>
      </div>

      <form onSubmit={plan} className="card grid md:grid-cols-5 gap-4 items-end">
        <div>
          <label className="label" htmlFor="from">From</label>
          <select id="from" className="input" value={origin}
                  onChange={(e) => setOrigin(e.target.value)}>
            {districts.map((d) => <option key={d.code} value={d.code}>{d.name}</option>)}
          </select>
        </div>
        <div>
          <label className="label" htmlFor="to">To</label>
          <select id="to" className="input" value={destination}
                  onChange={(e) => setDestination(e.target.value)}>
            {districts.map((d) => <option key={d.code} value={d.code}>{d.name}</option>)}
          </select>
        </div>
        <div>
          <label className="label" htmlFor="goods">Goods</label>
          <select id="goods" className="input" value={goodsType}
                  onChange={(e) => setGoodsType(e.target.value as GoodsType)}>
            {GOODS.map((g) => <option key={g} value={g}>{humanise(g)}</option>)}
          </select>
        </div>
        <label className="flex items-center gap-2 text-sm pb-2">
          <input type="checkbox" checked={avoidHighRisk}
                 onChange={(e) => setAvoidHighRisk(e.target.checked)} />
          Avoid high-risk roads
        </label>
        <button className="btn-primary justify-center" disabled={busy || !origin || !destination}>
          {busy ? 'Calculating...' : 'Find best route'}
        </button>
      </form>

      {error && (
        <div className="rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

      {result && (
        <div className="grid lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <NerMap
              height="60vh"
              highlightPath={result.recommended.path}
              alternatePaths={result.alternates.map((route) => route.path)}
            />
          </div>

          <div className="space-y-4">
            <div className="card border-l-4 border-l-blue-600">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Recommended
              </p>
              <p className="mt-1 text-2xl font-semibold">
                {result.recommended.distanceKm.toFixed(1)} km
              </p>
              <p className="text-sm text-slate-600">
                about {Math.floor(result.recommended.estimatedDurationMin / 60)} h{' '}
                {result.recommended.estimatedDurationMin % 60} min
              </p>
              <p className="mt-3 text-sm">
                Risk score{' '}
                <span className="font-semibold"
                      style={{ color: riskColor[result.recommended.riskLevel] }}>
                  {result.recommended.riskScore.toFixed(1)} ({humanise(result.recommended.riskLevel)})
                </span>
              </p>
              <p className="text-xs text-slate-500 mt-1">
                Computed by {result.recommended.provider}
              </p>

              {result.recommended.warnings.length > 0 && (
                <ul className="mt-3 space-y-1 text-xs text-amber-700">
                  {result.recommended.warnings.map((warning, index) => (
                    <li key={index}>{warning}</li>
                  ))}
                </ul>
              )}

              {result.recommended.segments.length > 0 && (
                <div className="mt-4">
                  <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">
                    Monitored roads on this route
                  </p>
                  <ul className="mt-2 space-y-1 text-sm">
                    {result.recommended.segments.map((segment) => (
                      <li key={segment.sequenceNo} className="flex justify-between gap-2">
                        <span className="truncate">{segment.roadName}</span>
                        <span className="text-xs shrink-0"
                              style={{ color: segment.riskLevel ? riskColor[segment.riskLevel] : undefined }}>
                          {segment.roadStatus}
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            {result.alternates.map((alternate) => (
              <div key={alternate.routeId} className="card">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Alternate
                </p>
                <p className="mt-1 text-lg font-semibold">
                  {alternate.distanceKm.toFixed(1)} km &middot; {alternate.estimatedDurationMin} min
                </p>
                <p className="text-sm" style={{ color: riskColor[alternate.riskLevel] }}>
                  Risk {alternate.riskScore.toFixed(1)} ({humanise(alternate.riskLevel)})
                </p>
                {alternate.warnings.map((warning, index) => (
                  <p key={index} className="mt-1 text-xs text-amber-700">{warning}</p>
                ))}
              </div>
            ))}

            <div className="card bg-slate-50">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                How this was decided
              </p>
              <p className="mt-2 text-xs font-mono text-slate-600 break-words">
                {result.explanation.formula}
              </p>
              {result.explanation.rejectedCandidates.length > 0 && (
                <div className="mt-3">
                  <p className="text-xs font-semibold text-slate-500">Rejected routes</p>
                  <ul className="mt-1 space-y-1 text-xs text-slate-600">
                    {result.explanation.rejectedCandidates.map((rejection, index) => (
                      <li key={index}>
                        <span className="font-medium">{humanise(rejection.reason)}</span>
                        {rejection.details.length > 0 && ' - ' + rejection.details.join('; ')}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
