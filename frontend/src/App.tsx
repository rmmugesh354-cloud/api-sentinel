import React, { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Sidebar } from './components/Sidebar';
import { Navbar } from './components/Navbar';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Dashboard } from './pages/Dashboard';
import { Endpoints } from './pages/Endpoints';
import { EndpointDetail } from './pages/EndpointDetail';
import { Incidents } from './pages/Incidents';
import { IncidentDetail } from './pages/IncidentDetail';
import { Notifications } from './pages/Notifications';
import { Settings } from './pages/Settings';
import { notificationService } from './services/notificationService';
import { dashboardService } from './services/dashboardService';

const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const location = useLocation();
  const [unreadCount, setUnreadCount] = useState(0);
  const [openIncidents, setOpenIncidents] = useState(0);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const fetchGlobalBadges = async () => {
    try {
      const [uCount, summary] = await Promise.all([
        notificationService.getUnreadCount(),
        dashboardService.getSummary(),
      ]);
      setUnreadCount(uCount);
      setOpenIncidents(Number(summary.openIncidents));
    } catch {
      // Ignored if user not logged in or network error
    }
  };

  useEffect(() => {
    fetchGlobalBadges();
    const interval = setInterval(fetchGlobalBadges, 15000);
    return () => clearInterval(interval);
  }, [location.pathname]);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await fetchGlobalBadges();
    // Dispatch a custom event so child pages can listen and re-fetch if desired
    window.dispatchEvent(new Event('sentinel-refresh'));
    setTimeout(() => setIsRefreshing(false), 600);
  };

  const getPageTitle = (pathname: string) => {
    if (pathname === '/') return 'System Overview Dashboard';
    if (pathname.startsWith('/endpoints/')) return 'Endpoint Performance Telemetry';
    if (pathname === '/endpoints') return 'Monitored API Endpoints';
    if (pathname.startsWith('/incidents/')) return 'Incident Investigation Timeline';
    if (pathname === '/incidents') return 'Outage Incidents';
    if (pathname === '/notifications') return 'Alert Notifications';
    if (pathname === '/settings') return 'Platform & Alert Settings';
    return 'API Sentinel';
  };

  return (
    <div className="flex min-h-screen bg-sentinel-bg">
      <Sidebar unreadNotifications={unreadCount} openIncidents={openIncidents} />
      <div className="flex-1 flex flex-col min-w-0">
        <Navbar
          title={getPageTitle(location.pathname)}
          unreadCount={unreadCount}
          onRefresh={handleRefresh}
          isRefreshing={isRefreshing}
        />
        <main className="flex-1 p-8 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
};

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-sentinel-darkest flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-cyan-500/30 border-t-cyan-400 rounded-full animate-spin" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Layout>{children}</Layout>;
};

const PublicOnlyRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-sentinel-darkest flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-cyan-500/30 border-t-cyan-400 rounded-full animate-spin" />
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Auth Routes */}
          <Route
            path="/login"
            element={
              <PublicOnlyRoute>
                <Login />
              </PublicOnlyRoute>
            }
          />
          <Route
            path="/register"
            element={
              <PublicOnlyRoute>
                <Register />
              </PublicOnlyRoute>
            }
          />

          {/* Protected Observability Routes */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/endpoints"
            element={
              <ProtectedRoute>
                <Endpoints />
              </ProtectedRoute>
            }
          />
          <Route
            path="/endpoints/:id"
            element={
              <ProtectedRoute>
                <EndpointDetail />
              </ProtectedRoute>
            }
          />
          <Route
            path="/incidents"
            element={
              <ProtectedRoute>
                <Incidents />
              </ProtectedRoute>
            }
          />
          <Route
            path="/incidents/:id"
            element={
              <ProtectedRoute>
                <IncidentDetail />
              </ProtectedRoute>
            }
          />
          <Route
            path="/notifications"
            element={
              <ProtectedRoute>
                <Notifications />
              </ProtectedRoute>
            }
          />
          <Route
            path="/settings"
            element={
              <ProtectedRoute>
                <Settings />
              </ProtectedRoute>
            }
          />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
