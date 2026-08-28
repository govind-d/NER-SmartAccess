import { useCallback, useEffect, useState } from 'react'
import { usersApi } from '../services/endpoints'
import { errorMessage } from '../services/api'
import type { User } from '../types'
import { formatDateTime } from '../utils/format'

/** Administrator view of accounts. Disabling is preferred over deleting, because
 *  incidents and deliveries keep pointing at their author. */
export default function Users() {
  const [users, setUsers] = useState<User[]>([])
  const [search, setSearch] = useState('')
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    usersApi.list(search ? { q: search, size: 50 } : { size: 50 })
      .then((page) => setUsers(page.content))
      .catch((err) => setError(errorMessage(err)))
  }, [search])

  useEffect(load, [load])

  async function toggle(user: User) {
    try {
      await usersApi.setEnabled(user.id, !user.enabled)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Users</h1>
          <p className="text-sm text-slate-500">Accounts and roles</p>
        </div>
        <input
          className="input w-64"
          placeholder="Search name, username or email"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {error && (
        <div className="rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="card p-0 overflow-hidden">
        <table className="w-full">
          <thead className="bg-slate-50">
            <tr>
              <th className="th">Name</th>
              <th className="th">Username</th>
              <th className="th">Email</th>
              <th className="th">District</th>
              <th className="th">Roles</th>
              <th className="th">Created</th>
              <th className="th">Status</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id} className="hover:bg-slate-50">
                <td className="td font-medium">{user.fullName}</td>
                <td className="td">{user.username}</td>
                <td className="td">{user.email}</td>
                <td className="td">{user.districtName ?? '-'}</td>
                <td className="td text-xs">{user.roles.join(', ')}</td>
                <td className="td">{formatDateTime(user.createdAt)}</td>
                <td className="td">
                  <button
                    className={'badge ' + (user.enabled
                      ? 'bg-green-100 text-green-800 border-green-300'
                      : 'bg-slate-100 text-slate-600 border-slate-300')}
                    onClick={() => void toggle(user)}
                  >
                    {user.enabled ? 'Enabled' : 'Disabled'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {users.length === 0 && (
          <p className="p-6 text-sm text-slate-500">No users match this search.</p>
        )}
      </div>
    </div>
  )
}
