import type { ReactNode } from 'react'

/**
 * The small shared pieces every page needs: a page header, a table shell, status
 * pills, empty states and loading skeletons.
 *
 * These exist so the eleven screens look like one product rather than eleven. Before
 * this file each page hand-rolled its own heading markup and its own "nothing here"
 * message, and they had all drifted slightly apart.
 */

// ---------------------------------------------------------------- page header

export function PageHeader({
  title,
  subtitle,
  actions,
  badge,
}: {
  title: string
  subtitle?: string
  actions?: ReactNode
  badge?: ReactNode
}) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div>
        <div className="flex items-center gap-2.5">
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">{title}</h1>
          {badge}
        </div>
        {subtitle && <p className="mt-0.5 text-sm text-slate-500">{subtitle}</p>}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  )
}

/** The pulsing green chip that says data is arriving over WebSocket. */
export function LiveBadge({ label = 'Live' }: { label?: string }) {
  return (
    <span className="badge bg-emerald-50 text-emerald-700 border-emerald-200">
      <span className="dot bg-emerald-500 live-dot" />
      {label}
    </span>
  )
}

// ---------------------------------------------------------------- table shell

/**
 * Wraps a table in a card with a sticky header and its own horizontal scroll, so a
 * wide table never makes the whole page scroll sideways.
 */
export function TableCard({
  head,
  children,
  empty,
  isEmpty,
  footer,
}: {
  head: ReactNode
  children: ReactNode
  empty?: ReactNode
  isEmpty?: boolean
  footer?: ReactNode
}) {
  return (
    <div className="card p-0 overflow-hidden">
      <div className="scroll-x">
        <table className="w-full min-w-[42rem]">
          <thead className="bg-slate-50/80 sticky top-0 z-10">
            <tr>{head}</tr>
          </thead>
          <tbody>{children}</tbody>
        </table>
      </div>
      {isEmpty && (empty ?? <EmptyState title="Nothing here yet" />)}
      {footer && (
        <div className="px-4 py-3 border-t border-slate-100 bg-slate-50/50 text-xs
                        text-slate-500">
          {footer}
        </div>
      )}
    </div>
  )
}

// ---------------------------------------------------------------- status pills

const STATUS_STYLES: Record<string, string> = {
  // road status
  OPEN: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  PARTIALLY_ACCESSIBLE: 'bg-amber-50 text-amber-700 border-amber-200',
  HIGH_RISK: 'bg-orange-50 text-orange-700 border-orange-200',
  BLOCKED: 'bg-red-50 text-red-700 border-red-200',
  // delivery lifecycle
  CREATED: 'bg-slate-100 text-slate-700 border-slate-200',
  IN_TRANSIT: 'bg-blue-50 text-blue-700 border-blue-200',
  DELAYED: 'bg-red-50 text-red-700 border-red-200',
  DELIVERED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  // severity
  LOW: 'bg-slate-100 text-slate-700 border-slate-200',
  MEDIUM: 'bg-amber-50 text-amber-700 border-amber-200',
  HIGH: 'bg-orange-50 text-orange-700 border-orange-200',
  CRITICAL: 'bg-red-50 text-red-700 border-red-200',
  // predicted risk
  LOW_RISK: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  MEDIUM_RISK: 'bg-amber-50 text-amber-700 border-amber-200',
  // district accessibility
  FULLY_ACCESSIBLE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  RESTRICTED: 'bg-orange-50 text-orange-700 border-orange-200',
  CUT_OFF: 'bg-red-50 text-red-700 border-red-200',
  // vehicle / account state
  ACTIVE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  INACTIVE: 'bg-slate-100 text-slate-500 border-slate-200',
  ENABLED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  DISABLED: 'bg-slate-100 text-slate-500 border-slate-200',
  // incident lifecycle
  REPORTED: 'bg-blue-50 text-blue-700 border-blue-200',
  VERIFIED: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  IN_PROGRESS: 'bg-amber-50 text-amber-700 border-amber-200',
  RESOLVED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  REJECTED: 'bg-slate-100 text-slate-500 border-slate-200',
}

const DOT_STYLES: Record<string, string> = {
  OPEN: 'bg-emerald-500',
  PARTIALLY_ACCESSIBLE: 'bg-amber-500',
  HIGH_RISK: 'bg-orange-500',
  BLOCKED: 'bg-red-500',
  IN_TRANSIT: 'bg-blue-500',
  DELAYED: 'bg-red-500',
  DELIVERED: 'bg-emerald-500',
  CRITICAL: 'bg-red-500',
  HIGH: 'bg-orange-500',
  MEDIUM: 'bg-amber-500',
  CUT_OFF: 'bg-red-500',
  ACTIVE: 'bg-emerald-500',
  ENABLED: 'bg-emerald-500',
  RESTRICTED: 'bg-orange-500',
}

/**
 * One pill for any enum in the system. Colour carries meaning consistently - a red
 * pill always means "bad", never decoration - which is what lets someone scan a
 * table without reading every word of it.
 */
export function StatusBadge({ value, dot = true }: { value?: string; dot?: boolean }) {
  if (!value) {
    return <span className="text-slate-400">-</span>
  }
  const style = STATUS_STYLES[value] ?? 'bg-slate-100 text-slate-700 border-slate-200'
  const dotStyle = DOT_STYLES[value]
  const label = value.charAt(0) + value.slice(1).toLowerCase().replace(/_/g, ' ')

  return (
    <span className={'badge ' + style}>
      {dot && dotStyle && <span className={'dot ' + dotStyle} />}
      {label}
    </span>
  )
}

// ---------------------------------------------------------------- empty + loading

export function EmptyState({
  title,
  body,
  icon,
  action,
}: {
  title: string
  body?: string
  icon?: ReactNode
  action?: ReactNode
}) {
  return (
    <div className="px-6 py-14 text-center">
      {icon && (
        <div className="mx-auto w-11 h-11 rounded-2xl bg-slate-100 text-slate-400
                        grid place-items-center mb-3">
          {icon}
        </div>
      )}
      <p className="text-sm font-medium text-slate-900">{title}</p>
      {body && <p className="mt-1 text-sm text-slate-500 max-w-sm mx-auto">{body}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

/** Grey blocks shaped like the content that is coming, instead of the word "Loading". */
export function SkeletonRows({ rows = 5, cols = 5 }: { rows?: number; cols?: number }) {
  return (
    <>
      {Array.from({ length: rows }).map((_, r) => (
        <tr key={r}>
          {Array.from({ length: cols }).map((__, c) => (
            <td key={c} className="td">
              <div
                className="h-3.5 rounded bg-slate-100 animate-pulse"
                style={{ width: `${55 + ((r + c) % 4) * 12}%` }}
              />
            </td>
          ))}
        </tr>
      ))}
    </>
  )
}

export function SkeletonCards({ count = 6 }: { count?: number }) {
  return (
    <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="card h-40 animate-pulse bg-slate-100/60" />
      ))}
    </div>
  )
}

/** Small round monogram used for drivers and users. */
export function Avatar({ name, tone = 'slate' }: { name?: string; tone?: 'slate' | 'indigo' }) {
  const initials = name
    ? name.split(/\s+/).slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '').join('')
    : '?'
  const styles = tone === 'indigo'
    ? 'from-indigo-500 to-indigo-700'
    : 'from-slate-600 to-slate-800'
  return (
    <span className={`w-7 h-7 shrink-0 rounded-full bg-gradient-to-br ${styles}
                      text-white grid place-items-center text-[10px] font-semibold`}>
      {initials}
    </span>
  )
}

/** A filter bar that sits above a table and groups its controls together. */
export function FilterBar({ children }: { children: ReactNode }) {
  return (
    <div className="card py-3 px-4 flex flex-wrap items-center gap-2.5">
      <span className="eyebrow mr-1">Filter</span>
      {children}
    </div>
  )
}

/** An error strip, identical everywhere so a failure never looks like a layout bug. */
export function ErrorNote({ message }: { message: string }) {
  return (
    <div className="rounded-xl bg-red-50 border border-red-200 px-3.5 py-2.5 text-sm
                    text-red-700">
      {message}
    </div>
  )
}
