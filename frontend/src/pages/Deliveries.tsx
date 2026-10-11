import { useCallback, useEffect, useState } from 'react'
import { deliveriesApi } from '../services/endpoints'
import { useAuth } from '../context/AuthContext'
import { errorMessage } from '../services/api'
import {
  EmptyState, ErrorNote, FilterBar, PageHeader, SkeletonRows, StatusBadge, TableCard,
} from '../components/ui'
import { IconPackage } from '../components/icons'
import type { Delivery, DeliveryStatus } from '../types'
import { formatDateTime, humanise } from '../utils/format'

/**
 * Consignments.
 *
 * A driver sees only their own (GET /deliveries/my); managers see everything. The status
 * buttons offer only the transitions the backend will actually accept, so an illegal
 * move is impossible rather than merely rejected - the same state machine, expressed
 * once on each side.
 */
const NEXT_STATUSES: Record<DeliveryStatus, DeliveryStatus[]> = {
  CREATED: ['IN_TRANSIT'],
  IN_TRANSIT: ['DELAYED', 'DELIVERED'],
  DELAYED: ['IN_TRANSIT', 'DELIVERED'],
  DELIVERED: [],
}

const FILTERS: (DeliveryStatus | '')[] = ['', 'CREATED', 'IN_TRANSIT', 'DELAYED', 'DELIVERED']

export default function Deliveries() {
  const { hasRole } = useAuth()
  const isDriverOnly =
    hasRole('DRIVER') && !hasRole('ADMIN', 'LOGISTICS_MANAGER', 'AUTHORITY_OFFICIAL')

  const [deliveries, setDeliveries] = useState<Delivery[]>([])
  const [filter, setFilter] = useState<DeliveryStatus | ''>('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(() => {
    setLoading(true)
    const request = isDriverOnly
      ? deliveriesApi.mine()
      : deliveriesApi.search(filter ? { status: filter } : undefined)
    request
      .then((page) => setDeliveries(page.content))
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false))
  }, [filter, isDriverOnly])

  useEffect(load, [load])

  async function changeStatus(id: number, status: DeliveryStatus) {
    setError(null)
    try {
      await deliveriesApi.changeStatus(id, status)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  const delayed = deliveries.filter((d) => d.status === 'DELAYED').length

  return (
    <div className="space-y-5">
      <PageHeader
        title={isDriverOnly ? 'My deliveries' : 'Deliveries'}
        subtitle={
          delayed > 0
            ? `${deliveries.length} consignment(s) · ${delayed} delayed`
            : `${deliveries.length} consignment(s) moving across the region`
        }
      />

      {!isDriverOnly && (
        <FilterBar>
          {FILTERS.map((f) => (
            <button
              key={f || 'all'}
              onClick={() => setFilter(f)}
              className={
                'badge transition ' +
                (filter === f
                  ? 'bg-slate-900 text-white border-slate-900'
                  : 'bg-white text-slate-600 border-slate-300 hover:border-slate-400')
              }
            >
              {f ? humanise(f) : 'All'}
            </button>
          ))}
        </FilterBar>
      )}

      {error && <ErrorNote message={error} />}

      <TableCard
        isEmpty={!loading && deliveries.length === 0}
        empty={
          <EmptyState
            icon={<IconPackage />}
            title={filter ? `No ${humanise(filter).toLowerCase()} deliveries` : 'No deliveries yet'}
            body={
              isDriverOnly
                ? 'Consignments assigned to your vehicle will appear here.'
                : 'A logistics manager can create a consignment and assign a vehicle to it.'
            }
          />
        }
        footer={deliveries.length > 0
          ? 'Only transitions the backend accepts are offered; an illegal move returns 422.'
          : undefined}
        head={
          <>
            <th className="th">Tracking</th>
            <th className="th">Route</th>
            <th className="th">Goods</th>
            <th className="th">Vehicle</th>
            <th className="th">ETA</th>
            <th className="th">Delay</th>
            <th className="th">Status</th>
            <th className="th">Advance</th>
          </>
        }
      >
        {loading ? (
          <SkeletonRows rows={5} cols={8} />
        ) : (
          deliveries.map((delivery) => (
            <tr key={delivery.id} className="row-hover">
              <td className="td font-mono text-xs text-slate-900">{delivery.trackingCode}</td>
              <td className="td">
                <span className="flex items-center gap-1.5 whitespace-nowrap">
                  <span>{delivery.sourceDistrictName}</span>
                  <span className="text-slate-300">&rarr;</span>
                  <span className="font-medium text-slate-900">
                    {delivery.destinationDistrictName}
                  </span>
                </span>
              </td>
              <td className="td">{humanise(delivery.goodsType)}</td>
              <td className="td font-mono text-xs">
                {delivery.vehicleNumber ?? <span className="text-slate-400">Unassigned</span>}
              </td>
              <td className="td text-slate-500 whitespace-nowrap">
                {formatDateTime(delivery.eta)}
              </td>
              <td className="td tabular">
                {delivery.delayMinutes > 0 ? (
                  <span className="text-red-600 font-medium">+{delivery.delayMinutes}m</span>
                ) : (
                  <span className="text-slate-400">-</span>
                )}
              </td>
              <td className="td"><StatusBadge value={delivery.status} /></td>
              <td className="td">
                <div className="flex gap-1">
                  {NEXT_STATUSES[delivery.status].length === 0 ? (
                    <span className="text-xs text-slate-400">complete</span>
                  ) : (
                    NEXT_STATUSES[delivery.status].map((next) => (
                      <button
                        key={next}
                        className="btn-ghost py-1 px-2 text-xs"
                        onClick={() => void changeStatus(delivery.id, next)}
                      >
                        {humanise(next)}
                      </button>
                    ))
                  )}
                </div>
              </td>
            </tr>
          ))
        )}
      </TableCard>
    </div>
  )
}
