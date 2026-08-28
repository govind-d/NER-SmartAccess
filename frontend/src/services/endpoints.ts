import { api, unwrap } from './api'
import type {
  Alert, AuthResponse, ChartPoint, DashboardSummary, Delivery, District,
  GeoJsonFeatureCollection, Incident, Page, RiskAssessment, Road,
  RouteRecommendation, User, Vehicle, VehicleLocation,
} from '../types'

/**
 * Every backend call the application makes, grouped by module.
 *
 * Components never call axios directly. If an endpoint changes, it changes here once,
 * and TypeScript points at every screen that has to adapt.
 */

export const authApi = {
  login: (username: string, password: string) =>
    unwrap<AuthResponse>(api.post('/auth/login', { username, password })),
  logout: (refreshToken: string) => api.post('/auth/logout', { refreshToken }),
  me: () => unwrap<User>(api.get('/auth/me')),
  changePassword: (currentPassword: string, newPassword: string) =>
    api.put('/auth/change-password', { currentPassword, newPassword }),
}

export const usersApi = {
  list: (params?: Record<string, unknown>) =>
    unwrap<Page<User>>(api.get('/users', { params })),
  setEnabled: (id: number, enabled: boolean) =>
    unwrap<User>(api.patch(`/users/${id}/status`, null, { params: { enabled } })),
  updateRoles: (id: number, roles: string[]) =>
    unwrap<User>(api.put(`/users/${id}/roles`, { roles })),
}

export const districtsApi = {
  list: () => unwrap<District[]>(api.get('/districts')),
  geoJson: () => api.get<GeoJsonFeatureCollection>('/districts/geojson').then((r) => r.data),
  accessibilitySummary: () => unwrap<unknown[]>(api.get('/districts/accessibility/summary')),
}

export const roadsApi = {
  search: (params?: Record<string, unknown>) =>
    unwrap<Page<Road>>(api.get('/roads', { params })),
  geoJson: (status?: string) =>
    api.get<GeoJsonFeatureCollection>('/roads/geojson', { params: { status } })
      .then((r) => r.data),
  blocked: () => unwrap<Road[]>(api.get('/roads/blocked')),
  changeStatus: (id: number, status: string, reason: string) =>
    unwrap<Road>(api.patch(`/roads/${id}/status`, { status, reason })),
}

export const vehiclesApi = {
  list: (params?: Record<string, unknown>) =>
    unwrap<Vehicle[]>(api.get('/vehicles', { params })),
  live: () => unwrap<VehicleLocation[]>(api.get('/vehicles/live')),
  history: (id: number) =>
    unwrap<VehicleLocation[]>(api.get(`/vehicles/${id}/locations/history`)),
}

export const deliveriesApi = {
  search: (params?: Record<string, unknown>) =>
    unwrap<Page<Delivery>>(api.get('/deliveries', { params })),
  mine: () => unwrap<Page<Delivery>>(api.get('/deliveries/my')),
  delayed: () => unwrap<Delivery[]>(api.get('/deliveries/delayed')),
  changeStatus: (id: number, status: string) =>
    unwrap<Delivery>(api.patch(`/deliveries/${id}/status`, { status })),
  assignRoute: (id: number, routeId: number) =>
    unwrap<Delivery>(api.patch(`/deliveries/${id}/route`, null, { params: { routeId } })),
}

export const incidentsApi = {
  search: (params?: Record<string, unknown>) =>
    unwrap<Page<Incident>>(api.get('/incidents', { params })),
  geoJson: () =>
    api.get<GeoJsonFeatureCollection>('/incidents/geojson').then((r) => r.data),
  /** Plain JSON report, used when there is no photo (and by the offline queue). */
  report: (payload: Record<string, unknown>) =>
    unwrap<Incident>(api.post('/incidents', payload)),
  /** Multipart report: the JSON goes in the "data" part, the image in "photo". */
  reportWithPhoto: (payload: Record<string, unknown>, photo: File) => {
    const form = new FormData()
    form.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
    form.append('photo', photo)
    return unwrap<Incident>(
      api.post('/incidents', form, { headers: { 'Content-Type': 'multipart/form-data' } }),
    )
  },
  verify: (id: number) => unwrap<Incident>(api.patch(`/incidents/${id}/verify`)),
}

export const fieldReportsApi = {
  /** Batch upload of everything queued while offline. */
  sync: (reports: unknown[]) =>
    unwrap<{ accepted: string[]; duplicates: string[]; failed: { clientUuid: string; reason: string }[] }>(
      api.post('/field-reports/sync', { reports }),
    ),
}

export const alertsApi = {
  recent: (limit = 10) => unwrap<Alert[]>(api.get('/alerts/recent', { params: { limit } })),
  list: (params?: Record<string, unknown>) =>
    unwrap<Page<Alert>>(api.get('/alerts', { params })),
  deactivate: (id: number) => api.patch(`/alerts/${id}/deactivate`),
}

export const riskApi = {
  forRoad: (roadId: number) => unwrap<RiskAssessment>(api.get(`/risk/roads/${roadId}`)),
  predict: (payload: Record<string, unknown>) =>
    unwrap<RiskAssessment>(api.post('/risk/predict', payload)),
  history: (roadId: number, days = 30) =>
    unwrap<RiskAssessment[]>(api.get(`/risk/roads/${roadId}/history`, { params: { days } })),
}

export const routesApi = {
  recommend: (payload: Record<string, unknown>) =>
    unwrap<RouteRecommendation>(api.post('/routes/recommend', payload)),
}

export const analyticsApi = {
  summary: () => unwrap<DashboardSummary>(api.get('/analytics/summary')),
  deliveryTrend: (days = 30) =>
    unwrap<ChartPoint[]>(api.get('/analytics/deliveries/trend', { params: { days } })),
  incidentsByType: (days = 90) =>
    unwrap<ChartPoint[]>(api.get('/analytics/incidents/by-type', { params: { days } })),
  roadStatusDistribution: () =>
    unwrap<ChartPoint[]>(api.get('/analytics/roads/status-distribution')),
  disruptionStatistics: (days = 90) =>
    unwrap<ChartPoint[]>(api.get('/analytics/disruption-statistics', { params: { days } })),
}

export const weatherApi = {
  summary: () => unwrap<unknown[]>(api.get('/weather/summary')),
}
