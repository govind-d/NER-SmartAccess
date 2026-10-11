import { useCallback, useEffect, useState } from 'react'
import { usersApi } from '../services/endpoints'
import { errorMessage } from '../services/api'
import {
  Avatar, EmptyState, ErrorNote, PageHeader, SkeletonRows, StatusBadge, TableCard,
} from '../components/ui'
import { IconUsers } from '../components/icons'
import type { User } from '../types'
import { formatDateTime } from '../utils/format'

/**
 * Administrator view of accounts.
 *
 * Disabling is offered in place of deleting, because incidents, deliveries and audit
 * rows keep pointing at their author - the status pill is a button for exactly that
 * reason.
 */
const ROLE_TONE: Record<string, string> = {
  ADMIN: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  AUTHORITY_OFFICIAL: 'bg-violet-50 text-violet-700 border-violet-200',
  LOGISTICS_MANAGER: 'bg-blue-50 text-blue-700 border-blue-200',
  FIELD_OFFICER: 'bg-teal-50 text-teal-700 border-teal-200',
  DRIVER: 'bg-slate-100 text-slate-700 border-slate-200',
}

export default function Users() {
  const [users, setUsers] = useState<User[]>([])
  const [search, setSearch] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(() => {
    setLoading(true)
    usersApi.list(search ? { q: search, size: 50 } : { size: 50 })
      .then((page) => setUsers(page.content))
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false))
  }, [search])

  useEffect(load, [load])

  async function toggle(user: User) {
    setError(null)
    try {
      await usersApi.setEnabled(user.id, !user.enabled)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  const enabled = users.filter((u) => u.enabled).length

  return (
    <div className="space-y-5">
      <PageHeader
        title="Users"
        subtitle={`${users.length} account(s) · ${enabled} enabled`}
        actions={
          <input
            className="input w-72"
            placeholder="Search name, username or email"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        }
      />

      {error && <ErrorNote message={error} />}

      <TableCard
        isEmpty={!loading && users.length === 0}
        empty={
          <EmptyState
            icon={<IconUsers />}
            title={search ? 'No accounts match that search' : 'No accounts yet'}
            body={search ? 'Try a shorter search term.' : undefined}
          />
        }
        footer="Click a status pill to enable or disable an account. Disabling also revokes its refresh tokens."
        head={
          <>
            <th className="th">Name</th>
            <th className="th">Username</th>
            <th className="th">Email</th>
            <th className="th">District</th>
            <th className="th">Roles</th>
            <th className="th">Created</th>
            <th className="th">Status</th>
          </>
        }
      >
        {loading ? (
          <SkeletonRows rows={6} cols={7} />
        ) : (
          users.map((user) => (
            <tr key={user.id} className="row-hover">
              <td className="td">
                <span className="flex items-center gap-2.5">
                  <Avatar name={user.fullName} tone="indigo" />
                  <span className="font-medium text-slate-900">{user.fullName}</span>
                </span>
              </td>
              <td className="td font-mono text-xs text-slate-600">{user.username}</td>
              <td className="td text-slate-600">{user.email}</td>
              <td className="td">
                {user.districtName ?? <span className="text-slate-400">-</span>}
              </td>
              <td className="td">
                <span className="flex flex-wrap gap-1">
                  {user.roles.map((role) => (
                    <span
                      key={role}
                      className={'badge ' + (ROLE_TONE[role] ?? ROLE_TONE.DRIVER)}
                    >
                      {role.replace(/_/g, ' ').toLowerCase()}
                    </span>
                  ))}
                </span>
              </td>
              <td className="td text-slate-500">{formatDateTime(user.createdAt)}</td>
              <td className="td">
                <button
                  onClick={() => void toggle(user)}
                  title={user.enabled ? 'Disable this account' : 'Enable this account'}
                  className="transition hover:opacity-75"
                >
                  <StatusBadge value={user.enabled ? 'ENABLED' : 'DISABLED'} />
                </button>
              </td>
            </tr>
          ))
        )}
      </TableCard>
    </div>
  )
}
