import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Bell, LogOut, User as UserIcon, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';

interface NavbarProps {
  title: string;
  unreadCount?: number;
  onRefresh?: () => void;
  isRefreshing?: boolean;
}

export const Navbar: React.FC<NavbarProps> = ({
  title,
  unreadCount = 0,
  onRefresh,
  isRefreshing = false,
}) => {
  const { user, logout } = useAuth();
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);

  return (
    <header className="h-16 bg-sentinel-darkest/80 backdrop-blur-md border-b border-sentinel-border px-8 flex items-center justify-between sticky top-0 z-30">
      {/* Title */}
      <div className="flex items-center gap-3">
        <h1 className="text-lg font-bold text-white tracking-tight">{title}</h1>
      </div>

      {/* Action Controls */}
      <div className="flex items-center gap-4">
        {onRefresh && (
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="p-2 rounded-lg bg-sentinel-card border border-sentinel-border text-slate-400 hover:text-white hover:border-slate-600 transition-colors disabled:opacity-50"
            title="Refresh dashboard data"
          >
            <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin text-cyan-400' : ''}`} />
          </button>
        )}

        {/* Notifications Icon Link */}
        <Link
          to="/notifications"
          className="relative p-2 rounded-lg bg-sentinel-card border border-sentinel-border text-slate-400 hover:text-white hover:border-slate-600 transition-colors"
          title="Alerts and notifications"
        >
          <Bell className="w-4 h-4" />
          {unreadCount > 0 && (
            <span className="absolute -top-1 -right-1 px-1.5 py-0.2 text-[10px] font-mono font-bold bg-rose-500 text-white rounded-full">
              {unreadCount > 99 ? '99+' : unreadCount}
            </span>
          )}
        </Link>

        {/* User Dropdown */}
        <div className="relative">
          <button
            onClick={() => setUserDropdownOpen(!userDropdownOpen)}
            className="flex items-center gap-2.5 px-3 py-1.5 rounded-lg bg-sentinel-card border border-sentinel-border hover:border-slate-600 transition-colors"
          >
            <div className="w-6 h-6 rounded-full bg-cyan-500/20 text-cyan-400 flex items-center justify-center font-bold text-xs">
              {user?.fullName?.charAt(0) || 'U'}
            </div>
            <div className="text-left hidden sm:block">
              <span className="text-xs font-semibold text-slate-200 block leading-tight">{user?.fullName}</span>
              <span className="text-[10px] text-slate-500 block leading-tight">{user?.email}</span>
            </div>
          </button>

          {userDropdownOpen && (
            <div className="absolute right-0 mt-2 w-48 bg-sentinel-card border border-sentinel-border rounded-xl shadow-xl py-1 z-50">
              <div className="px-4 py-2 border-b border-sentinel-border">
                <p className="text-xs font-medium text-slate-300">{user?.fullName}</p>
                <p className="text-[11px] text-slate-500 truncate">{user?.email}</p>
              </div>
              <Link
                to="/settings"
                onClick={() => setUserDropdownOpen(false)}
                className="flex items-center gap-2 px-4 py-2 text-xs text-slate-300 hover:bg-slate-800/60 transition-colors"
              >
                <UserIcon className="w-3.5 h-3.5 text-slate-400" />
                <span>Account Settings</span>
              </Link>
              <button
                onClick={() => {
                  setUserDropdownOpen(false);
                  logout();
                }}
                className="w-full flex items-center gap-2 px-4 py-2 text-xs text-rose-400 hover:bg-rose-500/10 transition-colors"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Sign Out</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
