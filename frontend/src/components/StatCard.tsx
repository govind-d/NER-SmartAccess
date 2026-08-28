interface StatCardProps {
  label: string
  value: number | string
  tone?: 'default' | 'good' | 'warn' | 'bad'
  hint?: string
}

const toneClasses: Record<string, string> = {
  default: 'text-slate-900',
  good: 'text-green-600',
  warn: 'text-amber-600',
  bad: 'text-red-600',
}

/** One summary tile on the dashboard. */
export function StatCard({ label, value, tone = 'default', hint }: StatCardProps) {
  return (
    <div className="card">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
      <p className={`mt-2 text-3xl font-semibold ${toneClasses[tone]}`}>{value}</p>
      {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
    </div>
  )
}
