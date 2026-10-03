import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { errorMessage } from '../services/api'
import { IconAlertTriangle, IconBrain, IconMap, IconRoute } from '../components/icons'

const HIGHLIGHTS = [
  { icon: IconMap, title: 'Live accessibility map', body: 'Road, bridge and district status across 13 NER districts, updated as it changes.' },
  { icon: IconBrain, title: 'Disruption risk engine', body: 'A Java rule engine scores landslide and flood risk, and explains every verdict.' },
  { icon: IconRoute, title: 'Risk-aware routing', body: 'Blocked corridors are excluded outright; risky ones are penalised, not hidden.' },
  { icon: IconAlertTriangle, title: 'Offline field reporting', body: 'Reports are queued on the device and sync themselves when the signal returns.' },
]

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
    <div className="min-h-screen grid lg:grid-cols-[1.1fr_1fr]">
      {/* ---------------- brand panel ---------------- */}
      <div className="relative hidden lg:flex flex-col justify-center px-14 xl:px-20
                      overflow-hidden text-white
                      bg-gradient-to-br from-slate-900 via-slate-900 to-indigo-950">
        {/* Soft light bloom, and a faint grid suggesting a map without being literal. */}
        <div className="absolute -top-32 -left-24 w-[28rem] h-[28rem] rounded-full
                        bg-indigo-500/20 blur-3xl" />
        <div className="absolute bottom-[-10rem] right-[-6rem] w-[26rem] h-[26rem] rounded-full
                        bg-cyan-400/10 blur-3xl" />
        <div
          className="absolute inset-0 opacity-[0.07]"
          style={{
            backgroundImage:
              'linear-gradient(to right, white 1px, transparent 1px), linear-gradient(to bottom, white 1px, transparent 1px)',
            backgroundSize: '46px 46px',
          }}
        />

        <div className="relative">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-br from-indigo-400 to-indigo-600
                            grid place-items-center font-bold shadow-lg shadow-indigo-900/50">
              NE
            </div>
            <span className="text-sm font-medium tracking-wide text-slate-300">
              NER SmartLogix AI
            </span>
          </div>

          <h1 className="mt-10 text-4xl xl:text-5xl font-semibold tracking-tight leading-[1.1]">
            Keeping the North East
            <span className="block bg-gradient-to-r from-indigo-300 to-cyan-200
                             bg-clip-text text-transparent">
              connected and supplied.
            </span>
          </h1>

          <p className="mt-5 max-w-lg text-slate-300 leading-relaxed">
            Smart logistics and accessibility intelligence for a region where landslides,
            floods and terrain decide whether medicine arrives on time.
          </p>

          <div className="mt-12 grid sm:grid-cols-2 gap-5 max-w-xl">
            {HIGHLIGHTS.map((item) => (
              <div key={item.title} className="flex gap-3">
                <div className="shrink-0 w-9 h-9 rounded-xl bg-white/10 grid place-items-center
                                text-indigo-200">
                  <item.icon />
                </div>
                <div>
                  <p className="text-sm font-medium text-white">{item.title}</p>
                  <p className="mt-0.5 text-xs text-slate-400 leading-relaxed">{item.body}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* ---------------- sign-in panel ---------------- */}
      <div className="flex items-center justify-center p-6 sm:p-10">
        <form onSubmit={submit} className="card w-full max-w-sm p-7">
          {/* Small brand mark for the mobile layout, where the panel above is hidden. */}
          <div className="lg:hidden flex items-center gap-2 mb-6">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 to-indigo-700
                            grid place-items-center text-white font-bold text-sm">
              NE
            </div>
            <span className="font-semibold text-slate-900">NER SmartLogix AI</span>
          </div>

          <h2 className="text-2xl font-semibold tracking-tight text-slate-900">
            Welcome back
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            Sign in with the account issued to you.
          </p>

          {error && (
            <div className="mt-5 rounded-xl bg-red-50 border border-red-200 px-3.5 py-2.5
                            text-sm text-red-700">
              {error}
            </div>
          )}

          <div className="mt-6">
            <label className="label" htmlFor="username">Username</label>
            <input
              id="username"
              className="input"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              placeholder="officer1"
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
              placeholder="••••••••"
              required
            />
          </div>

          <button className="btn-primary w-full mt-7 py-2.5" disabled={busy}>
            {busy ? 'Signing in...' : 'Sign in'}
          </button>

          <p className="mt-6 text-center text-[11px] text-slate-400">
            Demonstration system &middot; sample data for the North Eastern Region
          </p>
        </form>
      </div>
    </div>
  )
}
