import React from 'react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ReferenceLine,
  CartesianGrid,
} from 'recharts';
import { LatencyPoint } from '../types';

interface LatencyChartProps {
  data: LatencyPoint[];
  thresholdMs?: number;
  height?: number;
}

export const LatencyChart: React.FC<LatencyChartProps> = ({
  data,
  thresholdMs,
  height = 280,
}) => {
  if (!data || data.length === 0) {
    return (
      <div
        className="flex items-center justify-center bg-sentinel-darkest/50 rounded-xl border border-dashed border-sentinel-border text-slate-500 text-xs font-mono"
        style={{ height }}
      >
        No latency data collected in this window
      </div>
    );
  }

  const formattedData = data.map((d) => ({
    ...d,
    time: new Date(d.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
  }));

  return (
    <div style={{ width: '100%', height }}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={formattedData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
          <defs>
            <linearGradient id="latencyGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#06B6D4" stopOpacity={0.4} />
              <stop offset="95%" stopColor="#06B6D4" stopOpacity={0.0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="#1E293B" vertical={false} />
          <XAxis
            dataKey="time"
            stroke="#64748B"
            fontSize={11}
            tickLine={false}
            axisLine={{ stroke: '#1E293B' }}
          />
          <YAxis
            stroke="#64748B"
            fontSize={11}
            tickLine={false}
            axisLine={{ stroke: '#1E293B' }}
            unit="ms"
          />
          <Tooltip
            content={({ active, payload }) => {
              if (active && payload && payload.length) {
                const item = payload[0].payload as LatencyPoint & { time: string };
                return (
                  <div className="bg-sentinel-card border border-sentinel-border rounded-lg p-3 shadow-2xl text-xs">
                    <p className="font-semibold text-slate-200">{new Date(item.timestamp).toLocaleString()}</p>
                    <div className="mt-1 space-y-0.5 font-mono">
                      <p className="text-cyan-400">Latency: {item.latencyMs} ms</p>
                      <p className={item.isHealthy ? 'text-emerald-400' : 'text-rose-400'}>
                        Status: {item.statusCode ? `HTTP ${item.statusCode}` : 'Failed'}
                      </p>
                    </div>
                  </div>
                );
              }
              return null;
            }}
          />
          {thresholdMs && (
            <ReferenceLine
              y={thresholdMs}
              stroke="#F59E0B"
              strokeDasharray="4 4"
              label={{
                value: `Threshold (${thresholdMs}ms)`,
                fill: '#F59E0B',
                fontSize: 10,
                position: 'top',
              }}
            />
          )}
          <Area
            type="monotone"
            dataKey="latencyMs"
            stroke="#06B6D4"
            strokeWidth={2}
            fillOpacity={1}
            fill="url(#latencyGradient)"
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};
