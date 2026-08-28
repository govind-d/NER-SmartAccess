import { useEffect, useState } from 'react'
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import { analyticsApi } from '../services/endpoints'
import { StatCard } from '../components/StatCard'
import { useWebSocket } from '../hooks/useWebSocket'
import type { Alert, ChartPoint, DashboardSummary } from '../types'
import { formatRelative, roadStatusColor, severityBadge } from '../utils/format'

/**
 * The analytics dashboard.
 *
 * All the summary cards come from a single GET /analytics/summary, so the numbers are
 * from one instant and cannot contradict each other. The charts are fetched separately
 * because they are heavier and are not needed for the first paint.
 *
 * Alerts arriving over WebSocket are prepended live, so the "recent alerts" panel is
 * genuinely recent rather than as old as the last page load.
 */
export default function Dashboard() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [trend, setTrend] = useState<ChartPoint[]>([])
  const [byType, setByType] = useState<ChartPoint[]>([])
  const [roadStatus, setRoadStatus] = useState<ChartPoint[]>([])
  const [disruptions, setDisruptions] = useState<ChartPoint[]>([])
  const [liveAlerts, setLiveAlerts] = useState<Alert[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    analyticsApi.summary().then(setSummary)
      .catch(() => setError('Could not load the dashboard summary'))
    analyticsApi.deliveryTrend(14).then(setTrend).catch(() => undefined)
    analyticsApi.incidentsByType(90).then(setByType).catch(() => undefined)
    analyticsApi.roadStatusDistribution().then(setRoadStatus).catch(() => undefined)
    analyticsApi.disruptionStatistics(90).then(setDisruptions).catch(() => undefined)
  }, [])

  useWebSocket<Alert>('/topic/alerts', (alert) => {
    setLiveAlerts((current) => [alert, ...current].slice(0, 10))
  })

  const alerts = [...liveAlerts, ...(summary?.recentAlerts ?? [])].slice(0, 8)

  if (error) {
    return <div className="card text-red-700">{error}</div>
  }

  if (!summary) {
    return <div className="text-slate-500">Loading dashboard...</div>
  }

  const pieColors = ['#dc2626', '#f97316', '#eab308', '#0ea5e9', '#8b5cf6', '#16a34a', '#64748b']

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
          Operations dashboard
        </h1>
        <p className="text-sm text-slate-500">
          Live picture of the North Eastern Region logistics network
        </p>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Active vehicles" value={summary.activeVehicles} />
        <StatCard label="In transit" value={summary.inTransitDeliveries} />
        <StatCard label="Delayed deliveries" value={summary.delayedDeliveries}
                  tone={summary.delayedDeliveries > 0 ? 'bad' : 'good'} />
        <StatCard label="Delivered (24 h)" value={summary.deliveredToday} tone="good" />

        <StatCard label="Open roads" value={summary.openRoads} tone="good" />
        <StatCard label="High-risk roads" value={summary.highRiskRoads}
                  tone={summary.highRiskRoads > 0 ? 'warn' : 'good'} />
        <StatCard label="Blocked roads" value={summary.blockedRoads}
                  tone={summary.blockedRoads > 0 ? 'bad' : 'good'} />
        <StatCard label="Active incidents" value={summary.activeIncidents}
                  hint={`${summary.criticalIncidents} critical`}
                  tone={summary.criticalIncidents > 0 ? 'bad' : 'default'} />

        <StatCard label="Districts cut off" value={summary.districtsCutOff}
                  tone={summary.districtsCutOff > 0 ? 'bad' : 'good'} />
        <StatCard label="Districts restricted" value={summary.districtsRestricted}
                  tone={summary.districtsRestricted > 0 ? 'warn' : 'good'} />
        <StatCard label="Total deliveries" value={summary.totalDeliveries} />
        <StatCard label="Partially accessible" value={summary.partiallyAccessibleRoads}
                  tone="warn" />
      </div>

      <div className="grid lg:grid-cols-2 gap-6">
        {/* Delivery trend */}
        <div className="card">
          <h2 className="font-semibold text-slate-900">Deliveries created (14 days)</h2>
          <ResponsiveContainer width="100%" height={240}>
            <LineChart data={trend} margin={{ top: 16, right: 8, bottom: 0, left: -20 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="label" tick={{ fontSize: 11 }} tickFormatter={(v: string) => v.slice(5)} />
              <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
              <Tooltip />
              <Line type="monotone" dataKey="value" stroke="#0f172a" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Incidents by type */}
        <div className="card">
          <h2 className="font-semibold text-slate-900">Incidents by type (90 days)</h2>
          <ResponsiveContainer width="100%" height={240}>
            <PieChart>
              <Pie data={byType} dataKey="value" nameKey="label" outerRadius={85} label>
                {byType.map((_, index) => (
                  <Cell key={index} fill={pieColors[index % pieColors.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend wrapperStyle={{ fontSize: 11 }} />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Road status */}
        <div className="card">
          <h2 className="font-semibold text-slate-900">Road status distribution</h2>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={roadStatus} margin={{ top: 16, right: 8, bottom: 0, left: -20 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="label" tick={{ fontSize: 10 }}
                     tickFormatter={(v: string) => v.replace(/_/g, ' ')} />
              <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="value">
                {roadStatus.map((point) => (
                  <Cell
                    key={point.label}
                    fill={roadStatusColor[point.label as keyof typeof roadStatusColor] ?? '#64748b'}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Disruptions per district */}
        <div className="card">
          <h2 className="font-semibold text-slate-900">Disruptions by district (90 days)</h2>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={disruptions} layout="vertical"
                      margin={{ top: 16, right: 16, bottom: 0, left: 40 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis type="number" tick={{ fontSize: 11 }} allowDecimals={false} />
              <YAxis type="category" dataKey="label" tick={{ fontSize: 10 }} width={90} />
              <Tooltip />
              <Bar dataKey="value" fill="#f97316" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Recent alerts */}
      <div className="card">
        <h2 className="font-semibold text-slate-900 mb-3">Recent alerts</h2>
        {alerts.length === 0 ? (
          <p className="text-sm text-slate-500">Nothing to report. All corridors normal.</p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {alerts.map((alert) => (
              <li key={`${alert.id}-${alert.createdAt}`} className="py-3 flex items-start gap-3">
                <span className={`badge ${severityBadge(alert.severity)} shrink-0`}>
                  {alert.severity}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-slate-900">{alert.title}</p>
                  <p className="text-xs text-slate-500 truncate">{alert.message}</p>
                </div>
                <span className="text-xs text-slate-400 shrink-0">
                  {formatRelative(alert.createdAt)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
