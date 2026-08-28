import { useEffect, useState } from 'react'
import {
  Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import { riskApi, roadsApi } from '../services/endpoints'
import { errorMessage } from '../services/api'
import type { RiskAssessment, Road } from '../types'
import { humanise, riskColor } from '../utils/format'

/**
 * The AI engine, made visible.
 *
 * This is the screen to demonstrate in a viva. Pick a hill road, slide the rainfall from
 * 20 mm to 200 mm, and watch LOW_RISK become HIGH_RISK - with a bar chart showing exactly
 * which rules drove the change and by how much.
 *
 * Nothing here is stored: POST /risk/predict is a pure what-if, so a demonstration cannot
 * pollute the operational record.
 */
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

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
          Risk explorer
        </h1>
        <p className="text-sm text-slate-500">
          Ask the Java rule engine what would happen under different weather. Nothing is
          saved.
        </p>
      </div>

      {error && (
        <div className="rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="grid lg:grid-cols-3 gap-6">
        <div className="card space-y-4">
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
            <div className="text-xs text-slate-500 space-y-0.5">
              <p>Slope: {selectedRoad.slopeDegrees}&deg;</p>
              <p>Landslide susceptibility: {selectedRoad.landslideSusceptibility}</p>
              <p>Flood prone: {selectedRoad.floodProne ? 'yes' : 'no'}</p>
              <p>Condition: {humanise(selectedRoad.condition)}</p>
              <p>Current status: {humanise(selectedRoad.status)}</p>
            </div>
          )}

          <div>
            <label className="label" htmlFor="r24">
              Rainfall, last 24 h: <span className="font-semibold">{rainfall24h} mm</span>
            </label>
            <input id="r24" type="range" min={0} max={300} step={5} className="w-full"
                   value={rainfall24h}
                   onChange={(e) => setRainfall24h(Number(e.target.value))} />
            <p className="text-xs text-slate-500">
              IMD: 64.5 heavy, 115.5 very heavy, 204.5 extremely heavy
            </p>
          </div>

          <div>
            <label className="label" htmlFor="r72">
              Rainfall, last 72 h: <span className="font-semibold">{rainfall72h} mm</span>
            </label>
            <input id="r72" type="range" min={0} max={600} step={10} className="w-full"
                   value={rainfall72h}
                   onChange={(e) => setRainfall72h(Number(e.target.value))} />
          </div>

          <div>
            <label className="label" htmlFor="cong">
              Traffic congestion: <span className="font-semibold">{congestion}/4</span>
            </label>
            <input id="cong" type="range" min={0} max={4} step={1} className="w-full"
                   value={congestion}
                   onChange={(e) => setCongestion(Number(e.target.value))} />
          </div>

          <button className="btn-primary w-full justify-center" onClick={() => void predict()}
                  disabled={busy || !roadId}>
            {busy ? 'Predicting...' : 'Predict risk'}
          </button>
        </div>

        <div className="lg:col-span-2 space-y-4">
          {!assessment ? (
            <div className="card text-sm text-slate-500">
              Choose a road, set the weather and press Predict risk.
            </div>
          ) : (
            <>
              <div className="card">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                      Verdict
                    </p>
                    <p className="mt-1 text-3xl font-semibold"
                       style={{ color: riskColor[assessment.riskLevel] }}>
                      {humanise(assessment.riskLevel)}
                    </p>
                    <p className="text-sm text-slate-600">
                      Disruption score {assessment.disruptionScore.toFixed(1)} / 100
                    </p>
                  </div>
                  <span className="badge bg-slate-100 text-slate-600 border-slate-300">
                    {assessment.modelName}
                  </span>
                </div>
                <p className="mt-3 text-sm text-slate-700">{assessment.recommendation}</p>
              </div>

              <div className="card">
                <h2 className="font-semibold text-slate-900">
                  Why: contribution of each rule
                </h2>
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={assessment.scoreCard} layout="vertical"
                            margin={{ top: 16, right: 16, bottom: 0, left: 60 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis type="number" tick={{ fontSize: 11 }} />
                    <YAxis type="category" dataKey="rule" width={130}
                           tick={{ fontSize: 10 }}
                           tickFormatter={(v: string) => v.replace('Rule', '')} />
                    <Tooltip formatter={(value: number) => value.toFixed(1)} />
                    <Bar dataKey="contribution">
                      {assessment.scoreCard.map((entry, index) => (
                        <Cell key={index}
                              fill={entry.subScore >= 85 ? '#dc2626'
                                : entry.subScore >= 50 ? '#f97316' : '#64748b'} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>

                <ul className="mt-4 space-y-2">
                  {assessment.scoreCard.map((entry) => (
                    <li key={entry.rule} className="text-sm">
                      <span className="font-medium">{entry.rule.replace('Rule', '')}</span>
                      <span className="text-slate-500">
                        {' '}scored {entry.subScore.toFixed(0)} at weight {entry.weight}
                        {' '}(contributing {entry.contribution.toFixed(1)})
                      </span>
                      <p className="text-xs text-slate-500">{entry.reason}</p>
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
