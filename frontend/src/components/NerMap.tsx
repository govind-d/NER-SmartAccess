import { useEffect, useState } from 'react'
import { GeoJSON, MapContainer, Marker, Polyline, Popup, TileLayer } from 'react-leaflet'
import L from 'leaflet'
import type { GeoJsonObject } from 'geojson'
import { districtsApi, incidentsApi, roadsApi, vehiclesApi } from '../services/endpoints'
import { useWebSocket } from '../hooks/useWebSocket'
import type { GeoJsonFeatureCollection, VehicleLocation } from '../types'
import { accessibilityColor, formatRelative, roadStatusColor } from '../utils/format'

/**
 * The interactive map: OpenStreetMap tiles, PostGIS geometry, live vehicles.
 *
 * Four layers, each answering a different question:
 *   districts  - which areas are cut off (choropleth from accessibility level)
 *   roads      - which corridors are open, risky or blocked (colour by status)
 *   incidents  - what has actually happened and where
 *   vehicles   - where the fleet is right now, updated over WebSocket
 *
 * The GeoJSON comes straight from the backend, which produced it with PostGIS
 * ST_AsGeoJSON, so no geometry is ever parsed or converted in the browser.
 */

// Leaflet's default marker icons are loaded by a relative URL that Vite's bundler
// rewrites, which silently breaks them. Defining the icon explicitly avoids that.
const vehicleIcon = L.divIcon({
  className: '',
  html: '<div style="background:#0f172a;width:14px;height:14px;border-radius:50%;border:2px solid white;box-shadow:0 0 0 2px #0f172a"></div>',
  iconSize: [14, 14],
  iconAnchor: [7, 7],
})

const incidentIcon = L.divIcon({
  className: '',
  html: '<div style="background:#dc2626;width:12px;height:12px;border-radius:50%;border:2px solid white"></div>',
  iconSize: [12, 12],
  iconAnchor: [6, 6],
})

/** Roughly the centre of the North Eastern Region. */
const NER_CENTER: [number, number] = [25.9, 92.5]

interface Layers {
  districts: boolean
  roads: boolean
  incidents: boolean
  vehicles: boolean
}

interface NerMapProps {
  /** An optional route to draw on top, used by the route planner. */
  highlightPath?: [number, number][]
  alternatePaths?: [number, number][][]
  height?: string
}

export function NerMap({ highlightPath, alternatePaths, height = '75vh' }: NerMapProps) {
  const [districts, setDistricts] = useState<GeoJsonFeatureCollection | null>(null)
  const [roads, setRoads] = useState<GeoJsonFeatureCollection | null>(null)
  const [incidents, setIncidents] = useState<GeoJsonFeatureCollection | null>(null)
  const [vehicles, setVehicles] = useState<VehicleLocation[]>([])
  const [roadsVersion, setRoadsVersion] = useState(0)
  const [layers, setLayers] = useState<Layers>({
    districts: true, roads: true, incidents: true, vehicles: true,
  })

  useEffect(() => {
    void districtsApi.geoJson().then(setDistricts).catch(() => setDistricts(null))
    void roadsApi.geoJson().then(setRoads).catch(() => setRoads(null))
    void incidentsApi.geoJson().then(setIncidents).catch(() => setIncidents(null))
    void vehiclesApi.live().then(setVehicles).catch(() => setVehicles([]))
  }, [])

  // Live vehicle movement: replace the matching marker, or add a new one.
  useWebSocket<VehicleLocation>('/topic/vehicles', (position) => {
    setVehicles((current) => {
      const others = current.filter((v) => v.vehicleId !== position.vehicleId)
      return [...others, position]
    })
  })

  // A road that closes must recolour immediately, so the layer is refetched.
  useWebSocket<{ roadId: number }>('/topic/roads/status', () => {
    void roadsApi.geoJson().then((data) => {
      setRoads(data)
      // Bumping the version remounts the GeoJSON layer so Leaflet recolours the lines.
      setRoadsVersion((v) => v + 1)
    }).catch(() => undefined)
  })

  // A new incident drops a pin without a refresh.
  useWebSocket<{ id: number }>('/topic/incidents', () => {
    void incidentsApi.geoJson().then(setIncidents).catch(() => undefined)
  })

  return (
    <div className="relative">
      <div className="absolute top-3 right-3 z-[500] card py-3 px-4 space-y-1 text-sm">
        <p className="font-semibold text-xs uppercase tracking-wide text-slate-500 mb-2">
          Layers
        </p>
        {(Object.keys(layers) as (keyof Layers)[]).map((key) => (
          <label key={key} className="flex items-center gap-2 capitalize cursor-pointer">
            <input
              type="checkbox"
              checked={layers[key]}
              onChange={() => setLayers((current) => ({ ...current, [key]: !current[key] }))}
            />
            {key}
          </label>
        ))}
        <div className="pt-2 mt-2 border-t border-slate-200 space-y-1">
          {Object.entries(roadStatusColor).map(([status, color]) => (
            <div key={status} className="flex items-center gap-2 text-xs">
              <span className="w-4 h-1 rounded" style={{ background: color }} />
              <span className="text-slate-600">{status.replace(/_/g, ' ')}</span>
            </div>
          ))}
        </div>
      </div>

      <MapContainer center={NER_CENTER} zoom={7} style={{ height, width: '100%' }}
                    className="rounded-xl overflow-hidden border border-slate-200">
        <TileLayer
          attribution='&copy; OpenStreetMap contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {layers.districts && districts && (
          <GeoJSON
            key={`districts-${districts.features.length}`}
            data={districts as unknown as GeoJsonObject}
            style={(feature) => ({
              color: accessibilityColor[
                (feature?.properties?.accessibility ?? 'FULLY_ACCESSIBLE') as keyof typeof accessibilityColor
              ],
              weight: 1,
              fillOpacity: 0.08,
            })}
            onEachFeature={(feature, layer) => {
              layer.bindPopup(
                `<strong>${feature.properties?.name}</strong><br/>${feature.properties?.state}` +
                `<br/>Accessibility: ${feature.properties?.accessibility}`,
              )
            }}
          />
        )}

        {layers.roads && roads && (
          <GeoJSON
            key={`roads-v${roadsVersion}`}
            data={roads as unknown as GeoJsonObject}
            style={(feature) => ({
              color: roadStatusColor[
                (feature?.properties?.status ?? 'OPEN') as keyof typeof roadStatusColor
              ],
              weight: 4,
              opacity: 0.85,
            })}
            onEachFeature={(feature, layer) => {
              layer.bindPopup(
                `<strong>${feature.properties?.name}</strong><br/>` +
                `Code: ${feature.properties?.code}<br/>` +
                `Status: ${feature.properties?.status}<br/>` +
                `Predicted risk: ${feature.properties?.riskLevel}<br/>` +
                `Length: ${feature.properties?.lengthKm} km`,
              )
            }}
          />
        )}

        {layers.incidents && incidents?.features.map((feature, index) => {
          const coordinates = feature.geometry.coordinates as [number, number]
          return (
            <Marker
              key={`incident-${feature.properties.id ?? index}`}
              position={[coordinates[1], coordinates[0]]}
              icon={incidentIcon}
            >
              <Popup>
                <strong>{String(feature.properties.incidentType)}</strong>
                <br />
                Severity: {String(feature.properties.severity)}
                <br />
                {String(feature.properties.description ?? '')}
              </Popup>
            </Marker>
          )
        })}

        {layers.vehicles && vehicles.map((vehicle) => (
          <Marker
            key={vehicle.vehicleId}
            position={[vehicle.latitude, vehicle.longitude]}
            icon={vehicleIcon}
          >
            <Popup>
              <strong>{vehicle.vehicleNumber}</strong>
              <br />
              {vehicle.speedKmph !== undefined && `${Math.round(vehicle.speedKmph)} km/h`}
              <br />
              {vehicle.deliveryStatus ?? 'Idle'}
              <br />
              <span className="text-xs">{formatRelative(vehicle.recordedAt)}</span>
            </Popup>
          </Marker>
        ))}

        {/* Route planner overlays: alternates behind, the recommendation on top. */}
        {alternatePaths?.map((path, index) => (
          <Polyline key={`alt-${index}`} positions={path}
                    pathOptions={{ color: '#94a3b8', weight: 4, dashArray: '6 8' }} />
        ))}
        {highlightPath && (
          <Polyline positions={highlightPath}
                    pathOptions={{ color: '#2563eb', weight: 6 }} />
        )}
      </MapContainer>
    </div>
  )
}
