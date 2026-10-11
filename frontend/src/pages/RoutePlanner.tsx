import { useEffect, useState } from 'react'
import { districtsApi, routesApi } from '../services/endpoints'
import { errorMessage } from '../services/api'
import { NerMap } from '../components/NerMap'
import { EmptyState, ErrorNote, PageHeader, StatusBadge } from '../components/ui'
import { IconRoute } from '../components/icons'
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
function duration(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h} h ${m} min` : `${m} min`
}

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

  const swap = () => {
    setOrigin(destination)
    setDestination(origin)
  }

  return (
    <div className="space-y-5">
      <PageHeader
        title="Route planner"
        subtitle="Blocked roads are excluded outright; high-risk corridors are penalised, not hidden"
      />

      {/* ------------------------- request ------------------------- */}
      <form onSubmit={plan} className="card grid md:grid-cols-12 gap-4 items-end">
        <div className="md:col-span-3">
          <label className="label" htmlFor="from">From</label>
          <select id="from" className="input" value={origin}
                  onChange={(e) => setOrigin(e.target.value)}>
            {districts.map((d) => <option key={d.code} value={d.code}>{d.name}</option>)}
          </select>
        </div>

        <div className="md:col-span-1 flex justify-center pb-1">
          <button type="button" onClick={swap} title="Swap origin and destination"
                  className="btn-ghost px-2.5 py-2">
            &#8646;
          </button>
        </div>

        <div className="md:col-span-3">
          <label className="label" htmlFor="to">To</label>
          <select id="to" className="input" value={destination}
                  onChange={(e) => setDestination(e.target.value)}>
            {districts.map((d) => <option key={d.code} value={d.code}>{d.name}</option>)}
          </select>
        </div>

        <div className="md:col-span-2">
          <label className="label" htmlFor="goods">Goods</label>
          <select id="goods" className="input" value={goodsType}
                  onChange={(e) => setGoodsType(e.target.value as GoodsType)}>
            {GOODS.map((g) => <option key={g} value={g}>{humanise(g)}</option>)}
          </select>
        </div>

        <label className="md:col-span-2 flex items-center gap-2 text-sm pb-2.5
                          cursor-pointer select-none">
          <input type="checkbox" checked={avoidHighRisk}
                 onChange={(e) => setAvoidHighRisk(e.target.checked)} />
          Avoid high risk
        </label>

        <div className="md:col-span-12">
          <button className="btn-primary px-6" disabled={busy || !origin || !destination}>
            {busy ? 'Calculating...' : 'Find best route'}
          </button>
        </div>
      </form>

      {error && <ErrorNote message={error} />}

      {!result && !error && (
        <div className="card p-0">
          <EmptyState
            icon={<IconRoute />}
            title="No route calculated yet"
            body="Pick an origin and destination. OSRM returns candidates, then the risk engine ranks them against live road, weather and incident data."
          />
        </div>
      )}

      {result && (
        <div className="grid lg:grid-cols-3 gap-5 items-start">
          <div className="lg:col-span-2">
            <NerMap
              height="58vh"
              highlightPath={result.recommended.path}
              alternatePaths={result.alternates.map((route) => route.path)}
            />
            <p className="mt-2 text-xs text-slate-400">
              Blue is the recommendation; dashed grey lines are the alternates.
            </p>
          </div>

          <div className="space-y-4">
            {/* recommended */}
            <div className="card relative overflow-hidden">
              <span className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r
                               from-blue-500 to-indigo-600" />
              <div className="flex items-center justify-between">
                <p className="eyebrow">Recommended</p>
                <StatusBadge value={result.recommended.riskLevel} />
              </div>

              <div className="mt-2 flex items-baseline gap-2">
                <span className="text-3xl font-semibold tracking-tight text-slate-900 tabular">
                  {result.recommended.distanceKm.toFixed(1)}
                </span>
                <span className="text-sm text-slate-500">km</span>
              </div>
              <p className="text-sm text-slate-600">
                about {duration(result.recommended.estimatedDurationMin)}
              </p>

              <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
                <div className="rounded-lg bg-slate-50 border border-slate-200 px-2.5 py-2">
                  <p className="text-slate-500">Risk score</p>
                  <p className="font-semibold tabular"
                     style={{ color: riskColor[result.recommended.riskLevel] }}>
                    {result.recommended.riskScore.toFixed(1)}
                  </p>
                </div>
                <div className="rounded-lg bg-slate-50 border border-slate-200 px-2.5 py-2">
                  <p className="text-slate-500">Engine</p>
                  <p className="font-semibold font-mono text-slate-900">
                    {result.recommended.provider}
                  </p>
                </div>
              </div>

              {result.recommended.warnings.length > 0 && (
                <ul className="mt-3 space-y-1">
                  {result.recommended.warnings.map((warning, index) => (
                    <li key={index}
                        className="text-xs text-amber-800 bg-amber-50 border
                                   border-amber-200 rounded-lg px-2.5 py-1.5">
                      {warning}
                    </li>
                  ))}
                </ul>
              )}

              {result.recommended.segments.length > 0 && (
                <div className="mt-4 pt-3 border-t border-slate-100">
                  <p className="eyebrow mb-2">Monitored roads on this route</p>
                  <ul className="space-y-1.5">
                    {result.recommended.segments.map((segment) => (
                      <li key={segment.sequenceNo}
                          className="flex items-center justify-between gap-2 text-sm">
                        <span className="truncate text-slate-700">{segment.roadName}</span>
                        <StatusBadge value={segment.roadStatus ?? undefined} dot={false} />
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            {/* alternates */}
            {result.alternates.length > 0 && (
              <div className="space-y-2.5">
                <p className="eyebrow">Alternates</p>
                {result.alternates.map((alternate) => (
                  <div key={alternate.routeId} className="card card-hover py-3.5">
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-sm font-semibold text-slate-900 tabular">
                        {alternate.distanceKm.toFixed(1)} km
                        <span className="font-normal text-slate-500">
                          {' '}&middot; {duration(alternate.estimatedDurationMin)}
                        </span>
                      </span>
                      <StatusBadge value={alternate.riskLevel} dot={false} />
                    </div>
                    <p className="mt-1 text-xs text-slate-500 tabular">
                      risk {alternate.riskScore.toFixed(1)}
                    </p>
                    {alternate.warnings.map((warning, index) => (
                      <p key={index} className="mt-1.5 text-xs text-amber-700">{warning}</p>
                    ))}
                  </div>
                ))}
              </div>
            )}

            {/* reasoning */}
            <div className="card bg-slate-50/80">
              <p className="eyebrow">How this was decided</p>
              <p className="mt-2 text-[11px] font-mono text-slate-600 leading-relaxed
                            break-words">
                {result.explanation.formula}
              </p>

              {result.explanation.rejectedCandidates.length > 0 && (
                <div className="mt-3 pt-3 border-t border-slate-200">
                  <p className="eyebrow mb-2">Rejected routes</p>
                  <ul className="space-y-2">
                    {result.explanation.rejectedCandidates.map((rejection, index) => (
                      <li key={index} className="text-xs">
                        <span className="badge bg-red-50 text-red-700 border-red-200">
                          {humanise(rejection.reason)}
                        </span>
                        {rejection.details.length > 0 && (
                          <p className="mt-1 text-slate-600">
                            {rejection.details.join('; ')}
                          </p>
                        )}
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
