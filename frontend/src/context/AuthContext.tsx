import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { tokenStore } from '../services/api'
import { authApi } from '../services/endpoints'
import type { RoleName, User } from '../types'

/**
 * Who is logged in, available anywhere in the application.
 *
 * React context is used rather than passing the user down through props because almost
 * every screen needs it - the sidebar to decide which links to show, the pages to decide
 * which buttons to render, and the router to decide whether to let anyone in at all.
 */

interface AuthState {
  user: User | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
  hasRole: (...roles: RoleName[]) => boolean
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  // On a page refresh the tokens survive in localStorage but the user object does not,
  // so it is fetched again before anything else renders.
  useEffect(() => {
    if (!tokenStore.access()) {
      setLoading(false)
      return
    }
    authApi.me()
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const auth = await authApi.login(username, password)
    tokenStore.save(auth.accessToken, auth.refreshToken)
    setUser(auth.user)
  }, [])

  const logout = useCallback(async () => {
    const refreshToken = tokenStore.refresh()
    try {
      if (refreshToken) {
        await authApi.logout(refreshToken)
      }
    } finally {
      // Whatever the server says, the local session ends.
      tokenStore.clear()
      setUser(null)
    }
  }, [])

  const hasRole = useCallback(
    (...roles: RoleName[]) => !!user && roles.some((role) => user.roles.includes(role)),
    [user],
  )

  const value = useMemo(
    () => ({ user, loading, login, logout, hasRole }),
    [user, loading, login, logout, hasRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider')
  }
  return context
}
