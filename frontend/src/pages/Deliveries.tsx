import { useCallback, useEffect, useState } from 'react'
import { deliveriesApi } from '../services/endpoints'
import { useAuth } from '../context/AuthContext'
import { errorMessage } from '../services/api'
import type { Delivery, DeliveryStatus } from '../types'
import { deliveryBadge, formatDateTime, humanise } from '../utils/format'

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

  return (
    <div className="space-y-4">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
            {isDriverOnly ? 'My deliveries' : 'Deliveries'}
          </h1>
          <p className="text-sm text-slate-500">Consignments moving across the region</p>
        </div>
        {!isDriverOnly && (
          <select
            className="input w-52"
            value={filter}
            onChange={(e) => setFilter(e.target.value as DeliveryStatus | '')}
          >
            <option value="">All statuses</option>
            <option value="CREATED">Created</option>
            <option value="IN_TRANSIT">In transit</option>
            <option value="DELAYED">Delayed</option>
            <option value="DELIVERED">Delivered</option>
          </select>
        )}
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
              <th className="th">Tracking</th>
              <th className="th">Route</th>
              <th className="th">Goods</th>
              <th className="th">Vehicle</th>
              <th className="th">ETA</th>
              <th className="th">Delay</th>
              <th className="th">Status</th>
              <th className="th">Actions</th>
            </tr>
          </thead>
          <tbody>
            {deliveries.map((delivery) => (
              <tr key={delivery.id} className="hover:bg-slate-50">
                <td className="td font-mono text-xs">{delivery.trackingCode}</td>
                <td className="td">
                  {delivery.sourceDistrictName} to {delivery.destinationDistrictName}
                </td>
                <td className="td">{humanise(delivery.goodsType)}</td>
                <td className="td">{delivery.vehicleNumber ?? 'Unassigned'}</td>
                <td className="td">{formatDateTime(delivery.eta)}</td>
                <td className="td">
                  {delivery.delayMinutes > 0 ? (
                    <span className="text-red-600">{delivery.delayMinutes} min</span>
                  ) : (
                    '-'
                  )}
                </td>
                <td className="td">
                  <span className={'badge ' + deliveryBadge(delivery.status)}>
                    {humanise(delivery.status)}
                  </span>
                </td>
                <td className="td">
                  <div className="flex gap-1">
                    {NEXT_STATUSES[delivery.status].map((next) => (
                      <button
                        key={next}
                        className="btn-ghost py-1 px-2 text-xs"
                        onClick={() => void changeStatus(delivery.id, next)}
                      >
                        {humanise(next)}
                      </button>
                    ))}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && deliveries.length === 0 && (
          <p className="p-6 text-sm text-slate-500">No deliveries match this filter.</p>
        )}
      </div>
    </div>
  )
}
