import { Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ProtectedRoute } from './components/ProtectedRoute'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import MapPage from './pages/MapPage'
import Vehicles from './pages/Vehicles'
import Deliveries from './pages/Deliveries'
import Incidents from './pages/Incidents'
import ReportIncident from './pages/ReportIncident'
import RoutePlanner from './pages/RoutePlanner'
import RiskExplorer from './pages/RiskExplorer'
import Alerts from './pages/Alerts'
import Users from './pages/Users'
import { useAuth } from './context/AuthContext'

/**
 * Routing.
 *
 * Everything except /login sits inside <ProtectedRoute>, which redirects an anonymous
 * visitor to the login page and remembers where they were headed. Role restrictions
 * mirror the backend's @PreAuthorize rules so that nobody is shown a page whose data the
 * API would refuse them.
 */
export default function App() {
  const { user, hasRole } = useAuth()

  // A driver has no dashboard, so their home is the delivery list.
  const home = hasRole('ADMIN', 'AUTHORITY_OFFICIAL', 'LOGISTICS_MANAGER')
    ? <Dashboard />
    : <Navigate to="/deliveries" replace />

  return (
    <Routes>
      <Route
        path="/login"
        element={user ? <Navigate to="/" replace /> : <Login />}
      />

      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={home} />
        <Route path="/map" element={<MapPage />} />
        <Route path="/deliveries" element={<Deliveries />} />
        <Route
          path="/vehicles"
          element={
            <ProtectedRoute roles={['ADMIN', 'AUTHORITY_OFFICIAL', 'LOGISTICS_MANAGER']}>
              <Vehicles />
            </ProtectedRoute>
          }
        />
        <Route path="/incidents" element={<Incidents />} />
        <Route
          path="/report"
          element={
            <ProtectedRoute roles={['FIELD_OFFICER', 'AUTHORITY_OFFICIAL', 'DRIVER', 'ADMIN']}>
              <ReportIncident />
            </ProtectedRoute>
          }
        />
        <Route path="/routes" element={<RoutePlanner />} />
        <Route path="/risk" element={<RiskExplorer />} />
        <Route path="/alerts" element={<Alerts />} />
        <Route
          path="/users"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <Users />
            </ProtectedRoute>
          }
        />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
