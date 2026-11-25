import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Activity,
  AlertOctagon,
  Bell,
  Settings,
  Shield,
  FileCode2,
} from 'lucide-react';

interface SidebarProps {
  unreadNotifications?: number;
  openIncidents?: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  unreadNotifications = 0,
  openIncidents = 0,
}) => {
  const navItems = [
    { to: '/', label: 'Overview', icon: LayoutDashboard },
    { to: '/endpoints', label: 'Endpoints', icon: Activity },
    {
      to: '/incidents',
      label: 'Incidents',
      icon: AlertOctagon,
      badge: openIncidents > 0 ? openIncidents : undefined,
      badgeColor: 'bg-rose-500/20 text-rose-400 border border-rose-500/30',
    },
    {
      to: '/notifications',
      label: 'Alerts',
      icon: Bell,
      badge: unreadNotifications > 0 ? unreadNotifications : undefined,
      badgeColor: 'bg-cyan-500/20 text-cyan-400 border border-cyan-500/30',
    },
    { to: '/settings', label: 'Settings', icon: Settings },
  ];

  return (
    <aside className="w-64 bg-sentinel-darkest border-r border-sentinel-border flex flex-col shrink-0 h-screen sticky top-0">
      {/* Brand Logo */}
      <div className="h-16 flex items-center px-6 border-b border-sentinel-border gap-3">
        <div className="w-9 h-9 rounded-lg bg-gradient-to-tr from-cyan-600 to-sky-400 flex items-center justify-center shadow-lg shadow-cyan-500/20">
          <Shield className="w-5 h-5 text-white" />
        </div>
        <div>
          <span className="text-base font-bold tracking-tight text-white flex items-center gap-1.5">
            API <span className="text-cyan-400">SENTINEL</span>
          </span>
          <span className="text-[10px] uppercase tracking-widest text-slate-400 font-mono block">
            Reliability Hub
          </span>
        </div>
      </div>

      {/* Navigation Links */}
      <div className="flex-1 py-6 px-3 space-y-1 overflow-y-auto">
        <div className="px-3 mb-2 text-[10px] font-semibold text-slate-500 uppercase tracking-wider">
          Platform
        </div>
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150 ${
                isActive
                  ? 'bg-cyan-500/10 text-cyan-300 border border-cyan-500/20 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
              }`
            }
          >
            <div className="flex items-center gap-3">
              <item.icon className="w-4 h-4" />
              <span>{item.label}</span>
            </div>
            {item.badge !== undefined && (
              <span className={`px-2 py-0.5 text-xs rounded-full font-mono font-medium ${item.badgeColor}`}>
                {item.badge}
              </span>
            )}
          </NavLink>
        ))}

        <div className="px-3 pt-6 mb-2 text-[10px] font-semibold text-slate-500 uppercase tracking-wider">
          Documentation
        </div>
        <a
          href="/swagger-ui.html"
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-slate-400 hover:text-slate-200 hover:bg-slate-800/40 transition-colors"
        >
          <FileCode2 className="w-4 h-4 text-emerald-400" />
          <span>Swagger OpenAPI</span>
        </a>
      </div>

      {/* Engine Status Indicator */}
      <div className="p-4 border-t border-sentinel-border">
        <div className="bg-sentinel-card border border-sentinel-border rounded-lg p-3">
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400 font-medium">Monitoring Engine</span>
            <span className="inline-flex items-center gap-1.5 text-[11px] font-mono font-semibold text-emerald-400">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
              ACTIVE
            </span>
          </div>
          <p className="mt-1 text-[11px] text-slate-400">Background probes active</p>
        </div>
      </div>
    </aside>
  );
};
