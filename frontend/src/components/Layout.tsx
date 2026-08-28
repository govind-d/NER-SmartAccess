import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useWebSocket } from '../hooks/useWebSocket'
import { OfflineBanner } from './OfflineBanner'
import type { Alert, RoleName } from '../types'
import { formatRelative, severityBadge } from '../utils/format'

interface NavItem {
  to: string
  label: string
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
  { to: '/', label: 'Dashboard', roles: ['ADMIN', 'AUTHORITY_OFFICIAL', 'LOGISTICS_MANAGER'] },
  { to: '/map', label: 'Live map' },
  { to: '/deliveries', label: 'Deliveries' },
  { to: '/vehicles', label: 'Vehicles', roles: ['ADMIN', 'AUTHORITY_OFFICIAL', 'LOGISTICS_MANAGER'] },
  { to: '/incidents', label: 'Incidents' },
  { to: '/report', label: 'Report incident', roles: ['FIELD_OFFICER', 'AUTHORITY_OFFICIAL', 'DRIVER', 'ADMIN'] },
  { to: '/routes', label: 'Route planner' },
  { to: '/risk', label: 'Risk explorer' },
  { to: '/alerts', label: 'Alerts' },
  { to: '/users', label: 'Users', roles: ['ADMIN'] },
]

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
      <aside className="w-60 shrink-0 bg-slate-900 text-slate-200 flex flex-col">
        <div className="px-5 py-5 border-b border-slate-800">
          <p className="font-semibold tracking-tight text-white">NER SmartLogix</p>
          <p className="text-xs text-slate-400">Accessibility Intelligence</p>
        </div>

        <nav className="flex-1 p-3 space-y-1">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `block px-3 py-2 rounded-lg text-sm transition ${
                  isActive ? 'bg-slate-700 text-white' : 'hover:bg-slate-800'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="p-3 border-t border-slate-800 text-xs text-slate-400">
          <span
            className={`inline-block w-2 h-2 rounded-full mr-2 ${
              connected ? 'bg-green-400' : 'bg-slate-500'
            }`}
          />
          {connected ? 'Live updates on' : 'Reconnecting...'}
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <OfflineBanner />

        <header className="h-14 bg-white border-b border-slate-200 flex items-center
                           justify-between px-6">
          <div className="text-sm text-slate-500">
            North Eastern Region &middot; Logistics Control
          </div>
          <div className="flex items-center gap-4">
            <div className="text-right">
              <p className="text-sm font-medium text-slate-900">{user?.fullName}</p>
              <p className="text-xs text-slate-500">{user?.roles.join(', ')}</p>
            </div>
            <button
              className="btn-ghost py-1.5 text-sm"
              onClick={async () => {
                await logout()
                navigate('/login')
              }}
            >
              Sign out
            </button>
          </div>
        </header>

        <main className="flex-1 p-6 overflow-auto">
          <Outlet />
        </main>
      </div>

      {/* Live alert toasts */}
      <div className="fixed bottom-6 right-6 space-y-3 w-80 z-[1000]">
        {toasts.map((alert) => (
          <div key={alert.id} className="card border-l-4 border-l-red-500 shadow-lg">
            <div className="flex items-start justify-between gap-2">
              <span className={`badge ${severityBadge(alert.severity)}`}>{alert.severity}</span>
              <span className="text-xs text-slate-400">{formatRelative(alert.createdAt)}</span>
            </div>
            <p className="mt-2 text-sm font-semibold text-slate-900">{alert.title}</p>
            <p className="mt-1 text-xs text-slate-600 line-clamp-3">{alert.message}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
