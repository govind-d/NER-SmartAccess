import { useEffect, useState } from 'react'
import { vehiclesApi } from '../services/endpoints'
import { useWebSocket } from '../hooks/useWebSocket'
import type { Vehicle, VehicleLocation } from '../types'
import { formatRelative, humanise } from '../utils/format'

export default function Vehicles() {
  const [vehicles, setVehicles] = useState<Vehicle[]>([])
  const [positions, setPositions] = useState<Record<number, VehicleLocation>>({})
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    vehiclesApi.list().then(setVehicles).finally(() => setLoading(false))
  }, [])

  // Live positions overwrite the snapshot loaded above, so the table stays current
  // without polling.
  useWebSocket<VehicleLocation>('/topic/vehicles', (position) => {
    setPositions((current) => ({ ...current, [position.vehicleId]: position }))
  })

  if (loading) {
    return <p className="text-slate-500">Loading fleet...</p>
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Fleet</h1>

      <div className="card p-0 overflow-hidden">
        <table className="w-full">
          <thead className="bg-slate-50">
            <tr>
              <th className="th">Vehicle</th>
              <th className="th">Type</th>
              <th className="th">Driver</th>
              <th className="th">Capacity</th>
              <th className="th">Speed</th>
              <th className="th">Last seen</th>
              <th className="th">State</th>
            </tr>
          </thead>
          <tbody>
            {vehicles.map((vehicle) => {
              const live = positions[vehicle.id]
              return (
                <tr key={vehicle.id} className="hover:bg-slate-50">
                  <td className="td font-medium">{vehicle.vehicleNumber}</td>
                  <td className="td">{humanise(vehicle.vehicleType)}</td>
                  <td className="td">{vehicle.driverName ?? 'Unassigned'}</td>
                  <td className="td">{vehicle.capacityTons ?? '-'} t</td>
                  <td className="td">
                    {live?.speedKmph !== undefined
                      ? Math.round(live.speedKmph) + ' km/h'
                      : '-'}
                  </td>
                  <td className="td">
                    {formatRelative(live?.recordedAt ?? vehicle.lastSeenAt)}
                  </td>
                  <td className="td">
                    <span className={'badge ' + (vehicle.active
                      ? 'bg-green-100 text-green-800 border-green-300'
                      : 'bg-slate-100 text-slate-600 border-slate-300')}>
                      {vehicle.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
        {vehicles.length === 0 && (
          <p className="p-6 text-sm text-slate-500">No vehicles registered yet.</p>
        )}
      </div>
    </div>
  )
}
