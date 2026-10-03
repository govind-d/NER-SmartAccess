import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useWebSocket } from '../hooks/useWebSocket'
import { OfflineBanner } from './OfflineBanner'
import {
  IconAlertTriangle, IconBell, IconBrain, IconDashboard, IconLogout, IconMap,
  IconPackage, IconPin, IconRoute, IconTruck, IconUsers,
} from './icons'
import type { Alert, RoleName } from '../types'
import { formatRelative, severityBadge } from '../utils/format'

interface NavItem {
  to: string
  label: string
  icon: (p: { className?: string }) => JSX.Element
  roles?: RoleName[]
}

/**
 * The application shell: sidebar, top bar, live alert toasts.
 *
 * The navigation is filtered by role, so a driver simply never sees the admin pages.
 * The alert toast subscribes to /topic/alerts once, here, rather than on every page -
 * a landslide reported while the user is looking at the vehicle list should still
 * interrupt them.
 */
const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: IconDashboard, roles: ['ADMIN', 'AUTHORITY_OFFICIAL', 'LOGISTICS_MANAGER'] },
  { to: '/map', label: 'Live map', icon: IconMap },
  { to: '/deliveries', label: 'Deliveries', icon: IconPackage },
  { to: '/vehicles', label: 'Fleet', icon: IconTruck, roles: ['ADMIN', 'AUTHORITY_OFFICIAL', 'LOGISTICS_MANAGER'] },
  { to: '/incidents', label: 'Incidents', icon: IconAlertTriangle },
  { to: '/report', label: 'Report incident', icon: IconPin, roles: ['FIELD_OFFICER', 'AUTHORITY_OFFICIAL', 'DRIVER', 'ADMIN'] },
  { to: '/routes', label: 'Route planner', icon: IconRoute },
  { to: '/risk', label: 'Risk explorer', icon: IconBrain },
  { to: '/alerts', label: 'Alerts', icon: IconBell },
  { to: '/users', label: 'Users', icon: IconUsers, roles: ['ADMIN'] },
]

/** Initials for the avatar chip, e.g. "R. Marak" -> "RM". */
function initials(name?: string): string {
  if (!name) return '?'
  return name.split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase() ?? '').join('')
}

export function Layout() {
  const { user, logout, hasRole } = useAuth()
  const navigate = useNavigate()
  const [toasts, setToasts] = useState<Alert[]>([])

  const { connected } = useWebSocket<Alert>('/topic/alerts', (alert) => {
    // Keep only the three newest so a storm of alerts cannot bury the interface.
    setToasts((current) => [alert, ...current].slice(0, 3))
    window.setTimeout(
      () => setToasts((current) => current.filter((item) => item.id !== alert.id)),
      12000,
    )
  })

  const visibleItems = NAV_ITEMS.filter((item) => !item.roles || hasRole(...item.roles))

  return (
    <div className="min-h-screen flex">
      {/* ---------------- sidebar ---------------- */}
      <aside className="w-64 shrink-0 flex flex-col text-slate-200
                        bg-gradient-to-b from-slate-900 via-slate-900 to-indigo-950">
        <div className="px-5 py-5 border-b border-white/10">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-400 to-indigo-600
                            grid place-items-center text-white font-bold text-sm shadow-lg
                            shadow-indigo-900/40">
              NE
            </div>
            <div className="leading-tight">
              <p className="font-semibold text-white tracking-tight">SmartLogix</p>
              <p className="text-[11px] text-slate-400">Accessibility Intelligence</p>
            </div>
          </div>
        </div>

        <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `nav-link ${isActive ? 'nav-link-active' : ''}`
              }
            >
              {({ isActive }) => (
                <>
                  {/* Active pages get an accent bar rather than only a background,
                      so the current section is obvious at a glance. */}
                  <span
                    className={`absolute left-0 top-1/2 -translate-y-1/2 w-1 rounded-r-full
                                bg-indigo-400 transition-all ${isActive ? 'h-6' : 'h-0'}`}
                  />
                  <item.icon className={isActive ? 'text-indigo-300' : 'text-slate-400'} />
                  <span>{item.label}</span>
                </>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-white/10">
          <div className="flex items-center gap-2 text-xs text-slate-400">
            <span
              className={`dot ${connected ? 'bg-green-400 live-dot' : 'bg-slate-500'}`}
            />
            {connected ? 'Live updates on' : 'Reconnecting...'}
          </div>
        </div>
      </aside>

      {/* ---------------- main column ---------------- */}
      <div className="flex-1 flex flex-col min-w-0">
        <OfflineBanner />

        <header className="h-16 bg-white/80 backdrop-blur border-b border-slate-200/80
                           flex items-center justify-between px-6 sticky top-0 z-40">
          <div>
            <p className="text-sm font-medium text-slate-900">
              North Eastern Region
            </p>
            <p className="text-xs text-slate-500">Logistics &amp; Accessibility Control</p>
          </div>

          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:block">
              <p className="text-sm font-medium text-slate-900 leading-tight">
                {user?.fullName}
              </p>
              <p className="text-[11px] text-slate-500">
                {user?.roles.map((r) => r.replace(/_/g, ' ')).join(', ')}
              </p>
            </div>
            <div className="w-9 h-9 rounded-full bg-gradient-to-br from-slate-700 to-slate-900
                            text-white grid place-items-center text-xs font-semibold">
              {initials(user?.fullName)}
            </div>
            <button
              className="btn-ghost py-1.5 px-3"
              title="Sign out"
              onClick={async () => {
                await logout()
                navigate('/login')
              }}
            >
              <IconLogout />
              <span className="hidden md:inline">Sign out</span>
            </button>
          </div>
        </header>

        <main className="flex-1 p-6 overflow-auto">
          <Outlet />
        </main>
      </div>

      {/* ---------------- live alert toasts ---------------- */}
      <div className="fixed bottom-6 right-6 space-y-3 w-[22rem] z-[1000]">
        {toasts.map((alert) => (
          <div
            key={alert.id}
            className="card animate-toast border-l-4 border-l-red-500 shadow-xl"
          >
            <div className="flex items-start justify-between gap-2">
              <span className={`badge ${severityBadge(alert.severity)}`}>
                {alert.severity}
              </span>
              <span className="text-[11px] text-slate-400">
                {formatRelative(alert.createdAt)}
              </span>
            </div>
            <p className="mt-2 text-sm font-semibold text-slate-900">{alert.title}</p>
            <p className="mt-1 text-xs text-slate-600 line-clamp-3">{alert.message}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
