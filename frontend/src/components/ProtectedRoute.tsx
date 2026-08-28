import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../context/AuthContext'
import type { RoleName } from '../types'

/**
 * Guards a route.
 *
 * This is convenience, not security: hiding a page in the browser stops nobody
 * determined. The real enforcement is @PreAuthorize on the backend, which this mirrors
 * so that users are not shown screens whose data they would be refused anyway.
 */
export function ProtectedRoute({
  children,
  roles,
}: {
  children: ReactNode
  roles?: RoleName[]
}) {
  const { user, loading, hasRole } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <div className="min-h-screen grid place-items-center text-slate-500">
        Loading...
      </div>
    )
  }

  if (!user) {
    // Remember where they were going, so login can send them straight back.
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }

  if (roles && roles.length > 0 && !hasRole(...roles)) {
    return (
      <div className="p-8">
        <div className="card max-w-lg">
          <h2 className="text-lg font-semibold text-slate-900">Not permitted</h2>
          <p className="mt-2 text-sm text-slate-600">
            Your role ({user.roles.join(', ')}) does not have access to this page.
          </p>
        </div>
      </div>
    )
  }

  return <>{children}</>
}
