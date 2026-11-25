import React from 'react';
import { EndpointStatus, IncidentStatus } from '../types';

interface StatusBadgeProps {
  status: EndpointStatus | IncidentStatus | string;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'md' }) => {
  const normalized = status.toUpperCase();
  const isSmall = size === 'sm';

  const getStyle = () => {
    switch (normalized) {
      case 'HEALTHY':
      case 'RESOLVED':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
      case 'DEGRADED':
      case 'ACKNOWLEDGED':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
      case 'UNHEALTHY':
      case 'OPEN':
        return 'bg-rose-500/10 text-rose-400 border-rose-500/20 animate-pulse';
      case 'PAUSED':
        return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
      default:
        return 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20';
    }
  };

  const getDotStyle = () => {
    switch (normalized) {
      case 'HEALTHY':
      case 'RESOLVED':
        return 'bg-emerald-400';
      case 'DEGRADED':
      case 'ACKNOWLEDGED':
        return 'bg-amber-400';
      case 'UNHEALTHY':
      case 'OPEN':
        return 'bg-rose-400';
      case 'PAUSED':
        return 'bg-slate-400';
      default:
        return 'bg-cyan-400';
    }
  };

  return (
    <span
      className={`inline-flex items-center gap-1.5 font-medium rounded-full border ${getStyle()} ${
        isSmall ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs'
      }`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${getDotStyle()}`} />
      {normalized}
    </span>
  );
};
