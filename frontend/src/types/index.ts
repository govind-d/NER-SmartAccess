// Mirrors of the backend DTOs. Keeping them in one file makes it obvious when the API
// contract changes: the compiler points at every screen that has to be updated.

export type RoleName =
  | 'ADMIN'
  | 'AUTHORITY_OFFICIAL'
  | 'FIELD_OFFICER'
  | 'LOGISTICS_MANAGER'
  | 'DRIVER'

export type RoadStatus = 'OPEN' | 'PARTIALLY_ACCESSIBLE' | 'HIGH_RISK' | 'BLOCKED'
export type RiskLevel = 'LOW_RISK' | 'MEDIUM_RISK' | 'HIGH_RISK'
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type DeliveryStatus = 'CREATED' | 'IN_TRANSIT' | 'DELAYED' | 'DELIVERED'
export type AccessibilityLevel =
  | 'FULLY_ACCESSIBLE'
  | 'PARTIALLY_ACCESSIBLE'
  | 'RESTRICTED'
  | 'CUT_OFF'
export type IncidentType =
  | 'LANDSLIDE'
  | 'FLOOD'
  | 'ROAD_DAMAGE'
  | 'BRIDGE_DAMAGE'
  | 'TRAFFIC_CONGESTION'
  | 'ACCIDENT'
  | 'OTHER'
export type IncidentStatus = 'REPORTED' | 'VERIFIED' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED'
export type GoodsType =
  | 'MEDICINE'
  | 'FOOD'
  | 'AGRI_PRODUCE'
  | 'CONSTRUCTION_MATERIAL'
  | 'FUEL'
  | 'RELIEF_SUPPLIES'
  | 'OTHER'

/** Every successful response from the backend has this shape. */
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

/** Spring Data pages, as serialised by Jackson. */
export interface Page<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export interface User {
  id: number
  username: string
  email: string
  fullName: string
  phone?: string
  districtCode?: string
  districtName?: string
  enabled: boolean
  roles: RoleName[]
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInSeconds: number
  user: User
}

export interface District {
  id: number
  code: string
  name: string
  state: string
  centroidLatitude?: number
  centroidLongitude?: number
  population?: number
  areaSqKm?: number
  accessibilityLevel: AccessibilityLevel
  openRoads: number
  blockedRoads: number
  highRiskRoads: number
}

export interface Road {
  id: number
  code: string
  name: string
  roadType: string
  districtCode: string
  districtName: string
  lengthKm: number
  status: RoadStatus
  condition: string
  currentRiskLevel: RiskLevel
  slopeDegrees: number
  landslideSusceptibility: number
  floodProne: boolean
  historicalBlockDaysPerYear: number
  statusUpdatedAt?: string
}

export interface Vehicle {
  id: number
  vehicleNumber: string
  vehicleType: string
  capacityTons?: number
  driverId?: number
  driverName?: string
  driverPhone?: string
  active: boolean
  lastLatitude?: number
  lastLongitude?: number
  lastSeenAt?: string
}

export interface VehicleLocation {
  vehicleId: number
  vehicleNumber: string
  latitude: number
  longitude: number
  speedKmph?: number
  heading?: number
  deliveryId?: number
  deliveryStatus?: DeliveryStatus
  recordedAt: string
}

export interface Delivery {
  id: number
  trackingCode: string
  vehicleId?: number
  vehicleNumber?: string
  driverName?: string
  routeId?: number
  sourceDistrictCode: string
  sourceDistrictName: string
  destinationDistrictCode: string
  destinationDistrictName: string
  sourceLabel?: string
  destinationLabel?: string
  goodsType: GoodsType
  weightTons?: number
  status: DeliveryStatus
  dispatchedAt?: string
  eta?: string
  deliveredAt?: string
  delayMinutes: number
}

export interface Incident {
  id: number
  clientUuid?: string
  incidentType: IncidentType
  severity: Severity
  description?: string
  latitude: number
  longitude: number
  roadCode?: string
  roadName?: string
  districtCode?: string
  districtName?: string
  reportedByName?: string
  photoUrl?: string
  status: IncidentStatus
  occurredAt: string
  reportedAt?: string
  verifiedAt?: string
}

export interface Alert {
  id: number
  alertType: string
  severity: Severity
  title: string
  message: string
  districtCode?: string
  districtName?: string
  roadCode?: string
  incidentId?: number
  deliveryId?: number
  latitude?: number
  longitude?: number
  active: boolean
  createdAt: string
  expiresAt?: string
}

export interface ScoreCardEntry {
  rule: string
  subScore: number
  weight: number
  contribution: number
  reason: string
}

export interface RiskAssessment {
  roadId: number
  roadCode: string
  roadName: string
  riskLevel: RiskLevel
  disruptionScore: number
  disruptionProbability: number
  modelName: string
  scoreCard: ScoreCardEntry[]
  recommendation: string
  assessedAt: string
}

export interface RouteSegment {
  sequenceNo: number
  roadCode?: string
  roadName: string
  distanceKm: number
  durationMin: number
  riskLevel?: RiskLevel
  roadStatus?: string
}

export interface Route {
  routeId: number
  distanceKm: number
  estimatedDurationMin: number
  riskScore: number
  riskLevel: RiskLevel
  provider: string
  /** [latitude, longitude] pairs, ready for Leaflet's Polyline. */
  path: [number, number][]
  segments: RouteSegment[]
  warnings: string[]
}

export interface RouteRecommendation {
  recommended: Route
  alternates: Route[]
  explanation: {
    formula: string
    modelName: string
    rejectedCandidates: { reason: string; details: string[] }[]
  }
}

export interface DashboardSummary {
  activeVehicles: number
  totalDeliveries: number
  inTransitDeliveries: number
  delayedDeliveries: number
  deliveredToday: number
  openRoads: number
  partiallyAccessibleRoads: number
  highRiskRoads: number
  blockedRoads: number
  activeIncidents: number
  criticalIncidents: number
  districtsCutOff: number
  districtsRestricted: number
  recentAlerts: Alert[]
}

export interface ChartPoint {
  label: string
  value: number
}

/** GeoJSON, exactly as Leaflet's L.geoJSON() consumes it. */
export interface GeoJsonFeatureCollection {
  type: 'FeatureCollection'
  features: {
    type: 'Feature'
    geometry: { type: string; coordinates: unknown }
    properties: Record<string, unknown>
  }[]
}
