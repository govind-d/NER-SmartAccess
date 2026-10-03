import { useEffect, useState } from 'react'
import {
  Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import { analyticsApi } from '../services/endpoints'
import { StatCard } from '../components/StatCard'
import { useWebSocket } from '../hooks/useWebSocket'
import {
  IconAlertTriangle, IconMap, IconPackage, IconPin, IconTruck,
} from '../components/icons'
import type { Alert, ChartPoint, DashboardSummary } from '../types'
import { formatRelative, roadStatusColor, severityBadge } from '../utils/format'

/**
 * The analytics dashboard.
 *
 * All the summary cards come from a single GET /analytics/summary, so the numbers are
 * from one instant and cannot contradict each other. The charts are fetched separately
 * because they are heavier and are not needed for the first paint.
 *
 * The tiles are grouped into fleet, network and districts rather than presented as one
 * undifferentiated grid: a manager scanning for delays and an official scanning for
 * blocked roads are looking for different things.
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

  const alerts = [...liveAlerts, ...(summary?.recentAlerts ?? [])].slice(0, 6)

  if (error) {
    return <div className="card text-red-700">{error}</div>
  }

  if (!summary) {
    return (
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="card h-28 animate-pulse bg-slate-100/70" />
        ))}
      </div>
    )
  }

  const pieColors = ['#dc2626', '#f97316', '#eab308', '#0ea5e9', '#8b5cf6', '#16a34a', '#64748b']
  const axis = { fontSize: 11, fill: '#64748b' }

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
            Operations dashboard
          </h1>
          <p className="mt-0.5 text-sm text-slate-500">
            Live picture of the North Eastern Region logistics network
          </p>
        </div>
        <span className="badge bg-emerald-50 text-emerald-700 border-emerald-200">
          <span className="dot bg-emerald-500 live-dot" />
          Updating live
        </span>
      </div>

      {/* ---------------- fleet ---------------- */}
      <section className="space-y-3">
        <h2 className="eyebrow">Fleet &amp; deliveries</h2>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard label="Active vehicles" value={summary.activeVehicles}
                    tone="accent" icon={<IconTruck />} />
          <StatCard label="In transit" value={summary.inTransitDeliveries}
                    icon={<IconPackage />} />
          <StatCard label="Delayed" value={summary.delayedDeliveries}
                    tone={summary.delayedDeliveries > 0 ? 'bad' : 'good'}
                    hint={`${summary.totalDeliveries} total`} icon={<IconAlertTriangle />} />
          <StatCard label="Delivered (24 h)" value={summary.deliveredToday}
                    tone="good" icon={<IconPackage />} />
        </div>
      </section>

      {/* ---------------- network ---------------- */}
      <section className="space-y-3">
        <h2 className="eyebrow">Road network</h2>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard label="Open roads" value={summary.openRoads}
                    tone={summary.openRoads > 0 ? 'good' : 'bad'} icon={<IconMap />} />
          <StatCard label="Partially accessible" value={summary.partiallyAccessibleRoads}
                    tone="warn" />
          <StatCard label="High risk" value={summary.highRiskRoads}
                    tone={summary.highRiskRoads > 0 ? 'warn' : 'good'} />
          <StatCard label="Blocked" value={summary.blockedRoads}
                    tone={summary.blockedRoads > 0 ? 'bad' : 'good'} />
        </div>
      </section>

      {/* ---------------- incidents and districts ---------------- */}
      <section className="space-y-3">
        <h2 className="eyebrow">Incidents &amp; districts</h2>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard label="Active incidents" value={summary.activeIncidents}
                    tone={summary.criticalIncidents > 0 ? 'bad' : 'default'}
                    hint={`${summary.criticalIncidents} critical`} icon={<IconPin />} />
          <StatCard label="Districts cut off" value={summary.districtsCutOff}
                    tone={summary.districtsCutOff > 0 ? 'bad' : 'good'} />
          <StatCard label="Districts restricted" value={summary.districtsRestricted}
                    tone={summary.districtsRestricted > 0 ? 'warn' : 'good'} />
          <StatCard label="Total deliveries" value={summary.totalDeliveries} />
        </div>
      </section>

      {/* ---------------- charts ---------------- */}
      <div className="grid lg:grid-cols-2 gap-5">
        <div className="card">
          <h2 className="panel-title">Deliveries created</h2>
          <p className="text-xs text-slate-500 mb-2">Last 14 days</p>
          <ResponsiveContainer width="100%" height={230}>
            <AreaChart data={trend} margin={{ top: 12, right: 8, bottom: 0, left: -22 }}>
              <defs>
                <linearGradient id="deliveryFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#6366f1" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="#6366f1" stopOpacity={0.02} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
              <XAxis dataKey="label" tick={axis} tickLine={false} axisLine={false}
                     tickFormatter={(v: string) => v.slice(5)} />
              <YAxis tick={axis} tickLine={false} axisLine={false} allowDecimals={false} />
              <Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0', fontSize: 12 }} />
              <Area type="monotone" dataKey="value" stroke="#6366f1" strokeWidth={2}
                    fill="url(#deliveryFill)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="card">
          <h2 className="panel-title">Incidents by type</h2>
          <p className="text-xs text-slate-500 mb-2">Last 90 days</p>
          <ResponsiveContainer width="100%" height={230}>
            <PieChart>
              <Pie data={byType} dataKey="value" nameKey="label"
                   innerRadius={48} outerRadius={82} paddingAngle={3}>
                {byType.map((_, index) => (
                  <Cell key={index} fill={pieColors[index % pieColors.length]} />
                ))}
              </Pie>
              <Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0', fontSize: 12 }} />
              <Legend wrapperStyle={{ fontSize: 11 }} />
            </PieChart>
          </ResponsiveContainer>
        </div>

        <div className="card">
          <h2 className="panel-title">Road status distribution</h2>
          <p className="text-xs text-slate-500 mb-2">Every monitored corridor</p>
          <ResponsiveContainer width="100%" height={230}>
            <BarChart data={roadStatus} margin={{ top: 12, right: 8, bottom: 0, left: -22 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
              <XAxis dataKey="label" tick={{ ...axis, fontSize: 10 }} tickLine={false}
                     axisLine={false} tickFormatter={(v: string) => v.replace(/_/g, ' ')} />
              <YAxis tick={axis} tickLine={false} axisLine={false} allowDecimals={false} />
              <Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0', fontSize: 12 }} />
              <Bar dataKey="value" radius={[6, 6, 0, 0]}>
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

        <div className="card">
          <h2 className="panel-title">Disruptions by district</h2>
          <p className="text-xs text-slate-500 mb-2">Last 90 days</p>
          <ResponsiveContainer width="100%" height={230}>
            <BarChart data={disruptions} layout="vertical"
                      margin={{ top: 12, right: 16, bottom: 0, left: 44 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" horizontal={false} />
              <XAxis type="number" tick={axis} tickLine={false} axisLine={false}
                     allowDecimals={false} />
              <YAxis type="category" dataKey="label" tick={{ ...axis, fontSize: 10 }}
                     tickLine={false} axisLine={false} width={92} />
              <Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0', fontSize: 12 }} />
              <Bar dataKey="value" fill="#f97316" radius={[0, 6, 6, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* ---------------- recent alerts ---------------- */}
      <div className="card">
        <h2 className="panel-title mb-3">Recent alerts</h2>
        {alerts.length === 0 ? (
          <p className="text-sm text-slate-500">
            Nothing to report. All corridors behaving normally.
          </p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {alerts.map((alert) => (
              <li key={`${alert.id}-${alert.createdAt}`}
                  className="py-3 flex items-start gap-3">
                <span className={`badge shrink-0 ${severityBadge(alert.severity)}`}>
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
