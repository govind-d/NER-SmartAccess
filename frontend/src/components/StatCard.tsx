import type { ReactNode } from 'react'

type Tone = 'default' | 'good' | 'warn' | 'bad' | 'accent'

interface StatCardProps {
  label: string
  value: number | string
  tone?: Tone
  hint?: string
  icon?: ReactNode
}

/**
 * One summary tile.
 *
 * The tone is driven by what the number *means*, not by taste: two blocked roads is a
 * red tile, zero blocked roads is a green one. A dashboard where colour is decorative
 * teaches people to ignore colour, which defeats the point of having a red at all.
 */
const toneStyles: Record<Tone, { value: string; accent: string; iconWrap: string }> = {
  default: {
    value: 'text-slate-900',
    accent: 'from-slate-300 to-slate-400',
    iconWrap: 'bg-slate-100 text-slate-500',
  },
  accent: {
    value: 'text-indigo-700',
    accent: 'from-indigo-400 to-indigo-600',
    iconWrap: 'bg-indigo-50 text-indigo-600',
  },
  good: {
    value: 'text-emerald-700',
    accent: 'from-emerald-400 to-emerald-600',
    iconWrap: 'bg-emerald-50 text-emerald-600',
  },
  warn: {
    value: 'text-amber-700',
    accent: 'from-amber-400 to-amber-600',
    iconWrap: 'bg-amber-50 text-amber-600',
  },
  bad: {
    value: 'text-red-700',
    accent: 'from-red-400 to-red-600',
    iconWrap: 'bg-red-50 text-red-600',
  },
}

export function StatCard({ label, value, tone = 'default', hint, icon }: StatCardProps) {
  const styles = toneStyles[tone]

  return (
    <div className="card card-hover relative overflow-hidden">
      {/* Thin accent stripe along the top edge, coloured by tone. */}
      <span
        className={`absolute inset-x-0 top-0 h-1 bg-gradient-to-r ${styles.accent}`}
      />

      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="eyebrow truncate">{label}</p>
          <p className={`mt-2 text-3xl font-semibold tabular ${styles.value}`}>{value}</p>
          {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
        </div>

        {icon && (
          <div className={`shrink-0 w-9 h-9 rounded-xl grid place-items-center ${styles.iconWrap}`}>
            {icon}
          </div>
        )}
      </div>
    </div>
  )
}
