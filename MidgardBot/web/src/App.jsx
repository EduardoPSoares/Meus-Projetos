import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { ThemeProvider } from './context/ThemeContext'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import WhitelistPage from './pages/WhitelistPage'
import ModerationPage from './pages/ModerationPage'
import TicketsPage from './pages/TicketsPage'
import StaffPage from './pages/StaffPage'
import PlayersPage from './pages/PlayersPage'
import ReportsPage from './pages/ReportsPage'
import CallbackPage from './pages/CallbackPage'

function ProtectedRoute({ children }) {
  const { token } = useAuth()
  if (!token) return <Navigate to="/login" />
  return children
}

function RoleProtectedRoute({ children, roles }) {
  const { token, hasRole } = useAuth()
  if (!token) return <Navigate to="/login" />
  if (!hasRole(...roles)) return <Navigate to="/whitelists" />
  return children
}

function LoadingScreen() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-shadow">
      <div className="text-center animate-fade-in">
        <div className="text-4xl font-bold mb-2 text-gold">⚔️</div>
        <p className="text-text-secondary">Carregando Midgard...</p>
      </div>
    </div>
  )
}

export default function App() {
  return (
    <ThemeProvider>
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/callback" element={<CallbackPage />} />
          <Route
            path="/*"
            element={
              <ProtectedRoute>
                <Layout>
                  <Routes>
                    <Route path="/" element={<RoleProtectedRoute roles={['CEOO', 'MODERADOR', 'DEV']}><DashboardPage /></RoleProtectedRoute>} />
                    <Route path="/whitelists" element={<WhitelistPage />} />
                    <Route path="/moderation" element={<RoleProtectedRoute roles={['CEOO', 'MODERADOR', 'DEV']}><ModerationPage /></RoleProtectedRoute>} />
                    <Route path="/tickets" element={<TicketsPage />} />
                    <Route path="/staff" element={<StaffPage />} />
                    <Route path="/players" element={<PlayersPage />} />
                    <Route path="/reports" element={<ReportsPage />} />
                  </Routes>
                </Layout>
              </ProtectedRoute>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
    </ThemeProvider>
  )
}
