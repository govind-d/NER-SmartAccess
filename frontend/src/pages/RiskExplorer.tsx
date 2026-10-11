import { useEffect, useState } from 'react'
import { riskApi, roadsApi } from '../services/endpoints'
import { errorMessage } from '../services/api'
import { EmptyState, ErrorNote, PageHeader, StatusBadge } from '../components/ui'
import { IconBrain } from '../components/icons'
import type { RiskAssessment, Road } from '../types'
import { humanise, riskColor } from '../utils/format'

/**
 * The AI engine, made visible.
 *
 * This is the screen to demonstrate in a viva. Pick a hill road, drag the rainfall from
 * 20 mm to 200 mm, and watch LOW_RISK become HIGH_RISK - with each rule's contribution
 * shown as a bar so the verdict can be argued with rather than taken on trust.
 *
 * Nothing here is stored: POST /risk/predict is a pure what-if, so a demonstration
 * cannot pollute the operational record.
 */

/** IMD rainfall categories, shown as tick labels under the 24 h slider. */
const RAIN_MARKS = [
  { at: 15.6, label: 'light' },
  { at: 64.5, label: 'heavy' },
  { at: 115.5, label: 'very heavy' },
  { at: 204.5, label: 'extreme' },
]

function barColour(subScore: number): string {
  if (subScore >= 85) return '#dc2626'
  if (subScore >= 50) return '#f97316'
  if (subScore >= 25) return '#eab308'
  return '#94a3b8'
}

export default function RiskExplorer() {
  const [roads, setRoads] = useState<Road[]>([])
  const [roadId, setRoadId] = useState<number | null>(null)
  const [rainfall24h, setRainfall24h] = useState(20)
  const [rainfall72h, setRainfall72h] = useState(40)
  const [congestion, setCongestion] = useState(1)
  const [assessment, setAssessment] = useState<RiskAssessment | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    roadsApi.search({ size: 100 })
      .then((page) => {
        setRoads(page.content)
        if (page.content.length > 0) {
          setRoadId(page.content[0].id)
        }
      })
      .catch(() => setError('Could not load roads'))
  }, [])

  async function predict() {
    if (!roadId) return
    setBusy(true)
    setError(null)
    try {
      setAssessment(await riskApi.predict({
        roadId,
        rainfall24h,
        rainfall72h,
        weatherCondition: rainfall24h >= 64.5 ? 'HEAVY_RAIN' : 'CLOUDY',
        trafficCongestionLevel: congestion,
      }))
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const selectedRoad = roads.find((road) => road.id === roadId)
  const maxContribution = assessment
    ? Math.max(...assessment.scoreCard.map((e) => e.contribution), 1)
    : 1

  return (
    <div className="space-y-5">
      <PageHeader
        title="Risk explorer"
        subtitle="Ask the Java rule engine what would happen under different weather. Nothing is saved."
        badge={
          <span className="badge bg-indigo-50 text-indigo-700 border-indigo-200">
            what-if
          </span>
        }
      />

      {error && <ErrorNote message={error} />}

      <div className="grid lg:grid-cols-5 gap-5 items-start">
        {/* ------------------------- inputs ------------------------- */}
        <div className="lg:col-span-2 card space-y-5">
          <div>
            <label className="label" htmlFor="road">Road</label>
            <select
              id="road"
              className="input"
              value={roadId ?? ''}
              onChange={(e) => setRoadId(Number(e.target.value))}
            >
              {roads.map((road) => (
                <option key={road.id} value={road.id}>{road.name}</option>
              ))}
            </select>
          </div>

          {selectedRoad && (
            <div className="rounded-xl bg-slate-50 border border-slate-200 p-3.5">
              <p className="eyebrow mb-2">Fixed terrain inputs</p>
              <dl className="grid grid-cols-2 gap-y-2 gap-x-3 text-xs">
                <dt className="text-slate-500">Slope</dt>
                <dd className="text-slate-900 tabular text-right">
                  {selectedRoad.slopeDegrees}&deg;
                </dd>
                <dt className="text-slate-500">Landslide susceptibility</dt>
                <dd className="text-slate-900 tabular text-right">
                  {selectedRoad.landslideSusceptibility}
                </dd>
                <dt className="text-slate-500">Flood prone</dt>
                <dd className="text-right">
                  {selectedRoad.floodProne
                    ? <span className="text-amber-700 font-medium">yes</span>
                    : <span className="text-slate-500">no</span>}
                </dd>
                <dt className="text-slate-500">Surface</dt>
                <dd className="text-right text-slate-900">
                  {humanise(selectedRoad.condition)}
                </dd>
                <dt className="text-slate-500">Current status</dt>
                <dd className="text-right">
                  <StatusBadge value={selectedRoad.status} dot={false} />
                </dd>
              </dl>
            </div>
          )}

          {/* 24 h rainfall, with the IMD categories marked on the track */}
          <div>
            <div className="flex items-baseline justify-between">
              <label className="label mb-0" htmlFor="r24">Rainfall, last 24 h</label>
              <span className="text-sm font-semibold text-slate-900 tabular">
                {rainfall24h} mm
              </span>
            </div>
            <input
              id="r24" type="range" min={0} max={300} step={5}
              className="w-full mt-2 accent-indigo-600"
              value={rainfall24h}
              onChange={(e) => setRainfall24h(Number(e.target.value))}
            />
            <div className="relative h-4 mt-0.5">
              {RAIN_MARKS.map((mark) => (
                <span
                  key={mark.label}
                  className="absolute text-[10px] text-slate-400 -translate-x-1/2 whitespace-nowrap"
                  style={{ left: `${(mark.at / 300) * 100}%` }}
                >
                  {mark.label}
                </span>
              ))}
            </div>
            <p className="mt-1 text-[11px] text-slate-400">
              India Meteorological Department categories
            </p>
          </div>

          <div>
            <div className="flex items-baseline justify-between">
              <label className="label mb-0" htmlFor="r72">Rainfall, last 72 h</label>
              <span className="text-sm font-semibold text-slate-900 tabular">
                {rainfall72h} mm
              </span>
            </div>
            <input
              id="r72" type="range" min={0} max={600} step={10}
              className="w-full mt-2 accent-indigo-600"
              value={rainfall72h}
              onChange={(e) => setRainfall72h(Number(e.target.value))}
            />
            <p className="mt-1 text-[11px] text-slate-400">
              Three-day totals saturate a hillside; a single cloudburst floods a valley
            </p>
          </div>

          <div>
            <div className="flex items-baseline justify-between">
              <label className="label mb-0" htmlFor="cong">Traffic congestion</label>
              <span className="text-sm font-semibold text-slate-900 tabular">
                {congestion}/4
              </span>
            </div>
            <input
              id="cong" type="range" min={0} max={4} step={1}
              className="w-full mt-2 accent-indigo-600"
              value={congestion}
              onChange={(e) => setCongestion(Number(e.target.value))}
            />
          </div>

          <button
            className="btn-primary w-full py-2.5"
            onClick={() => void predict()}
            disabled={busy || !roadId}
          >
            {busy ? 'Predicting...' : 'Predict risk'}
          </button>
        </div>

        {/* ------------------------- verdict ------------------------- */}
        <div className="lg:col-span-3 space-y-5">
          {!assessment ? (
            <div className="card p-0">
              <EmptyState
                icon={<IconBrain />}
                title="No prediction yet"
                body="Choose a road, set the weather, and press Predict risk. Try the same road at 20 mm and then at 200 mm."
              />
            </div>
          ) : (
            <>
              {/* headline verdict */}
              <div className="card relative overflow-hidden">
                <span
                  className="absolute inset-x-0 top-0 h-1"
                  style={{ background: riskColor[assessment.riskLevel] }}
                />
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <p className="eyebrow">Verdict</p>
                    <p
                      className="mt-1 text-3xl font-semibold tracking-tight"
                      style={{ color: riskColor[assessment.riskLevel] }}
                    >
                      {humanise(assessment.riskLevel)}
                    </p>
                    <p className="mt-1 text-sm text-slate-600">
                      {assessment.roadName}
                    </p>
                  </div>

                  {/* score as a ring, so the number reads at a glance */}
                  <div className="relative w-20 h-20 shrink-0">
                    <svg viewBox="0 0 36 36" className="w-20 h-20 -rotate-90">
                      <circle cx="18" cy="18" r="15.5" fill="none" stroke="#e2e8f0"
                              strokeWidth="3.5" />
                      <circle
                        cx="18" cy="18" r="15.5" fill="none"
                        stroke={riskColor[assessment.riskLevel]}
                        strokeWidth="3.5" strokeLinecap="round"
                        strokeDasharray={`${(assessment.disruptionScore / 100) * 97.4} 97.4`}
                      />
                    </svg>
                    <div className="absolute inset-0 grid place-items-center">
                      <span className="text-lg font-semibold text-slate-900 tabular">
                        {Math.round(assessment.disruptionScore)}
                      </span>
                    </div>
                  </div>
                </div>

                <p className="mt-3 text-sm text-slate-700">{assessment.recommendation}</p>

                <p className="mt-3 pt-3 border-t border-slate-100 text-[11px] text-slate-400">
                  model <span className="font-mono">{assessment.modelName}</span>
                  &nbsp;·&nbsp; disruption score {assessment.disruptionScore.toFixed(1)} / 100
                  &nbsp;·&nbsp; thresholds 35 and 65
                </p>
              </div>

              {/* score card */}
              <div className="card">
                <h2 className="panel-title">Why: contribution of each rule</h2>
                <p className="text-xs text-slate-500 mb-4">
                  Sub-score multiplied by the rule's configured weight. Strongest first.
                </p>

                <ul className="space-y-3.5">
                  {assessment.scoreCard.map((entry) => (
                    <li key={entry.rule}>
                      <div className="flex items-baseline justify-between gap-3">
                        <span className="text-sm font-medium text-slate-900">
                          {entry.rule.replace('Rule', '')}
                        </span>
                        <span className="text-xs text-slate-500 tabular shrink-0">
                          {entry.subScore.toFixed(0)} &times; {entry.weight} ={' '}
                          <span className="font-semibold text-slate-900">
                            {entry.contribution.toFixed(1)}
                          </span>
                        </span>
                      </div>

                      <div className="mt-1.5 h-2 rounded-full bg-slate-100 overflow-hidden">
                        <div
                          className="h-full rounded-full transition-all duration-500"
                          style={{
                            width: `${(entry.contribution / maxContribution) * 100}%`,
                            background: barColour(entry.subScore),
                          }}
                        />
                      </div>

                      <p className="mt-1 text-xs text-slate-500">{entry.reason}</p>
                    </li>
                  ))}
                </ul>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
