import React from 'react';

interface StatCardProps {
  title: string;
  value: string | number;
  subtext?: string;
  icon: React.ComponentType<{ className?: string }>;
  variant?: 'cyan' | 'green' | 'red' | 'amber' | 'slate';
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtext,
  icon: Icon,
  variant = 'cyan',
}) => {
  const getIconColor = () => {
    switch (variant) {
      case 'green':
        return 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20';
      case 'red':
        return 'text-rose-400 bg-rose-500/10 border-rose-500/20';
      case 'amber':
        return 'text-amber-400 bg-amber-500/10 border-amber-500/20';
      case 'slate':
        return 'text-slate-400 bg-slate-500/10 border-slate-500/20';
      default:
        return 'text-cyan-400 bg-cyan-500/10 border-cyan-500/20';
    }
  };

  return (
    <div className="bg-sentinel-card border border-sentinel-border rounded-xl p-5 hover:border-sentinel-borderLight transition-all duration-200">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium text-slate-400 uppercase tracking-wider">{title}</p>
        <div className={`p-2 rounded-lg border ${getIconColor()}`}>
          <Icon className="w-5 h-5" />
        </div>
      </div>
      <div className="mt-4">
        <h3 className="text-2xl font-bold tracking-tight text-white">{value}</h3>
        {subtext && <p className="mt-1 text-xs text-slate-400">{subtext}</p>}
      </div>
    </div>
  );
};
