import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { errorMessage } from '../services/api'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(username, password)
      // Send them back where they were headed before the guard intervened.
      const from = (location.state as { from?: string } | null)?.from ?? '/'
      navigate(from, { replace: true })
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen grid lg:grid-cols-2">
      <div className="hidden lg:flex flex-col justify-center px-16 bg-slate-900 text-white">
        <h1 className="text-3xl font-semibold tracking-tight">NER SmartLogix AI</h1>
        <p className="mt-4 text-slate-300 max-w-md">
          Smart Logistics and Accessibility Intelligence for the North Eastern Region.
          Live road status, AI disruption prediction, risk-aware routing and offline
          field reporting.
        </p>
        <ul className="mt-8 space-y-2 text-sm text-slate-400">
          <li>Real-time road and bridge monitoring across 13 districts</li>
          <li>Java rule-based landslide and flood risk engine</li>
          <li>Route recommendations that avoid blocked corridors</li>
        </ul>
      </div>

      <div className="flex items-center justify-center p-8">
        <form onSubmit={submit} className="card w-full max-w-sm">
          <h2 className="text-xl font-semibold text-slate-900">Sign in</h2>
          <p className="mt-1 text-sm text-slate-500">Use the account issued to you.</p>

          {error && (
            <div className="mt-4 rounded-lg bg-red-50 border border-red-200 px-3 py-2
                            text-sm text-red-700">
              {error}
            </div>
          )}

          <div className="mt-5">
            <label className="label" htmlFor="username">Username</label>
            <input
              id="username"
              className="input"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
            />
          </div>

          <div className="mt-4">
            <label className="label" htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              className="input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </div>

          <button className="btn-primary w-full mt-6 justify-center" disabled={busy}>
            {busy ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  )
}
