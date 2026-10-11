import { useEffect, useState } from 'react'
import { vehiclesApi } from '../services/endpoints'
import { useWebSocket } from '../hooks/useWebSocket'
import {
  Avatar, EmptyState, LiveBadge, PageHeader, SkeletonRows, StatusBadge, TableCard,
} from '../components/ui'
import { IconTruck } from '../components/icons'
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

  const moving = Object.values(positions).filter((p) => (p.speedKmph ?? 0) > 1).length

  return (
    <div className="space-y-5">
      <PageHeader
        title="Fleet"
        subtitle={`${vehicles.length} vehicle(s) registered${moving ? ` · ${moving} moving now` : ''}`}
        badge={<LiveBadge label="Tracking" />}
      />

      <TableCard
        isEmpty={!loading && vehicles.length === 0}
        empty={
          <EmptyState
            icon={<IconTruck />}
            title="No vehicles registered"
            body="A logistics manager can add vehicles and assign drivers to them."
          />
        }
        footer={vehicles.length > 0
          ? 'Speed and last-seen update over WebSocket as GPS pings arrive.'
          : undefined}
        head={
          <>
            <th className="th">Vehicle</th>
            <th className="th">Type</th>
            <th className="th">Driver</th>
            <th className="th">Capacity</th>
            <th className="th">Speed</th>
            <th className="th">Last seen</th>
            <th className="th">State</th>
          </>
        }
      >
        {loading ? (
          <SkeletonRows rows={5} cols={7} />
        ) : (
          vehicles.map((vehicle) => {
            const live = positions[vehicle.id]
            const speed = live?.speedKmph
            return (
              <tr key={vehicle.id} className="row-hover">
                <td className="td">
                  <span className="font-medium font-mono text-slate-900">
                    {vehicle.vehicleNumber}
                  </span>
                </td>
                <td className="td">{humanise(vehicle.vehicleType)}</td>
                <td className="td">
                  {vehicle.driverName ? (
                    <span className="flex items-center gap-2">
                      <Avatar name={vehicle.driverName} />
                      <span>{vehicle.driverName}</span>
                    </span>
                  ) : (
                    <span className="text-slate-400">Unassigned</span>
                  )}
                </td>
                <td className="td tabular">
                  {vehicle.capacityTons != null ? `${vehicle.capacityTons} t` : '-'}
                </td>
                <td className="td tabular">
                  {speed != null ? (
                    <span className="flex items-center gap-1.5">
                      <span className={`dot ${speed > 1 ? 'bg-emerald-500' : 'bg-slate-300'}`} />
                      {Math.round(speed)} km/h
                    </span>
                  ) : (
                    <span className="text-slate-400">-</span>
                  )}
                </td>
                <td className="td text-slate-500">
                  {formatRelative(live?.recordedAt ?? vehicle.lastSeenAt)}
                </td>
                <td className="td">
                  <StatusBadge value={vehicle.active ? 'ACTIVE' : 'INACTIVE'} />
                </td>
              </tr>
            )
          })
        )}
      </TableCard>
    </div>
  )
}
