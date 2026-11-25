import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardService } from '../services/dashboardService';
import {
  Activity,
  CheckCircle2,
  AlertTriangle,
  AlertOctagon,
  Gauge,
  ArrowUpRight,
  TrendingUp,
} from 'lucide-react';
import { StatCard } from '../components/StatCard';
import { StatusBadge } from '../components/StatusBadge';
import { LatencyChart } from '../components/LatencyChart';
import { DashboardSummary, LatencyMetrics, AvailabilityMetrics } from '../types';

export const Dashboard: React.FC = () => {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [latencyMetrics, setLatencyMetrics] = useState<LatencyMetrics | null>(null);
  const [availability, setAvailability] = useState<AvailabilityMetrics | null>(null);
  const [window, setWindow] = useState<'1h' | '24h' | '7d' | '30d'>('24h');
  const [isLoading, setIsLoading] = useState(true);

  const fetchDashboardData = async () => {
    try {
      const [sumData, latData, availData] = await Promise.all([
        dashboardService.getSummary(),
        dashboardService.getLatencyMetrics(undefined, window),
        dashboardService.getAvailabilityMetrics(undefined, window),
      ]);
      setSummary(sumData);
      setLatencyMetrics(latData);
      setAvailability(availData);
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, [window]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="w-8 h-8 border-2 border-cyan-500/30 border-t-cyan-400 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Top Bar with Time Horizon Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white tracking-tight">System Reliability Overview</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Real-time health telemetry and performance percentiles across all registered APIs
          </p>
        </div>

        <div className="flex items-center gap-1 bg-sentinel-darkest p-1 rounded-xl border border-sentinel-border self-start">
          {(['1h', '24h', '7d', '30d'] as const).map((win) => (
            <button
              key={win}
              onClick={() => setWindow(win)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold font-mono transition-colors ${
                window === win
                  ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              {win.toUpperCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Primary KPI Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Monitored Endpoints"
          value={summary?.totalEndpoints || 0}
          subtext={`${summary?.healthyEndpoints || 0} healthy, ${summary?.pausedEndpoints || 0} paused`}
          icon={Activity}
          variant="cyan"
        />
        <StatCard
          title="Healthy Services"
          value={summary?.healthyEndpoints || 0}
          subtext="Responding as expected"
          icon={CheckCircle2}
          variant="green"
        />
        <StatCard
          title="Degraded / Failing"
          value={(summary?.degradedEndpoints || 0) + (summary?.unhealthyEndpoints || 0)}
          subtext={`${summary?.unhealthyEndpoints || 0} outages, ${summary?.degradedEndpoints || 0} degraded`}
          icon={AlertTriangle}
          variant={(summary?.unhealthyEndpoints || 0) > 0 ? 'red' : 'amber'}
        />
        <StatCard
          title="Open Incidents"
          value={summary?.openIncidents || 0}
          subtext={summary?.openIncidents === 0 ? 'All systems operational' : 'Active outage alarms'}
          icon={AlertOctagon}
          variant={summary?.openIncidents === 0 ? 'green' : 'red'}
        />
      </div>

      {/* Latency Percentiles & Availability Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Availability Card */}
        <div className="bg-sentinel-card border border-sentinel-border rounded-xl p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Availability Uptime ({window.toUpperCase()})
              </span>
              <Gauge className="w-5 h-5 text-emerald-400" />
            </div>
            <div className="mt-4">
              <div className="flex items-baseline gap-2">
                <span className="text-4xl font-extrabold text-white tracking-tight font-mono">
                  {availability?.availabilityPercentage.toFixed(2)}%
                </span>
                <span className="text-xs text-emerald-400 font-semibold flex items-center">
                  <TrendingUp className="w-3.5 h-3.5 mr-0.5" /> Normal
                </span>
              </div>
              <div className="mt-4 w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                <div
                  className="bg-emerald-400 h-2 rounded-full transition-all duration-500"
                  style={{ width: `${Math.min(100, Math.max(0, availability?.availabilityPercentage || 100))}%` }}
                />
              </div>
            </div>
          </div>
          <div className="mt-6 pt-4 border-t border-sentinel-border/50 grid grid-cols-2 gap-4 text-xs">
            <div>
              <p className="text-slate-500">Successful Probes</p>
              <p className="font-mono font-semibold text-white mt-0.5">{availability?.successfulChecks || 0}</p>
            </div>
            <div>
              <p className="text-slate-500">Failed Probes</p>
              <p className="font-mono font-semibold text-rose-400 mt-0.5">{availability?.failedChecks || 0}</p>
            </div>
          </div>
        </div>

        {/* Tail Latency Breakdown (p50, p95, p99) */}
        <div className="lg:col-span-2 bg-sentinel-card border border-sentinel-border rounded-xl p-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-sm font-semibold text-white">Response Time Percentiles</h3>
              <p className="text-xs text-slate-400">Exact statistical tail latencies ({window.toUpperCase()})</p>
            </div>
            <span className="text-xs font-mono text-slate-500">
              {latencyMetrics?.sampleCount || 0} samples
            </span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-2">
            <div className="bg-sentinel-darkest/60 border border-sentinel-border rounded-xl p-3.5 text-center">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">p50 (Median)</span>
              <span className="text-xl font-bold font-mono text-cyan-400 mt-1 block">
                {latencyMetrics?.p50Ms || 0} <span className="text-xs font-normal text-slate-400">ms</span>
              </span>
            </div>
            <div className="bg-sentinel-darkest/60 border border-sentinel-border rounded-xl p-3.5 text-center">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">p95 (Tail)</span>
              <span className="text-xl font-bold font-mono text-amber-400 mt-1 block">
                {latencyMetrics?.p95Ms || 0} <span className="text-xs font-normal text-slate-400">ms</span>
              </span>
            </div>
            <div className="bg-sentinel-darkest/60 border border-sentinel-border rounded-xl p-3.5 text-center">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">p99 (Peak)</span>
              <span className="text-xl font-bold font-mono text-rose-400 mt-1 block">
                {latencyMetrics?.p99Ms || 0} <span className="text-xs font-normal text-slate-400">ms</span>
              </span>
            </div>
            <div className="bg-sentinel-darkest/60 border border-sentinel-border rounded-xl p-3.5 text-center">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">Average</span>
              <span className="text-xl font-bold font-mono text-slate-200 mt-1 block">
                {latencyMetrics?.averageMs?.toFixed(1) || 0} <span className="text-xs font-normal text-slate-400">ms</span>
              </span>
            </div>
          </div>

          <div className="mt-4 pt-3">
            <LatencyChart data={latencyMetrics?.timeSeries || []} height={200} />
          </div>
        </div>
      </div>

      {/* Tables Row: Recent Failures & Active Incidents */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Failures Table */}
        <div className="bg-sentinel-card border border-sentinel-border rounded-xl overflow-hidden">
          <div className="p-5 border-b border-sentinel-border flex items-center justify-between">
            <div>
              <h3 className="text-sm font-semibold text-white">Recent Health Check Failures</h3>
              <p className="text-xs text-slate-400">Detected anomalies and threshold violations</p>
            </div>
          </div>

          <div className="overflow-x-auto">
            {(!summary?.recentFailures || summary.recentFailures.length === 0) ? (
              <div className="p-8 text-center text-xs text-slate-500">
                No recent health check failures recorded. All endpoints passing health policies.
              </div>
            ) : (
              <table className="w-full text-left text-xs">
                <thead className="bg-sentinel-darkest text-slate-400 font-semibold border-b border-sentinel-border">
                  <tr>
                    <th className="py-3 px-4">Time</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4">Latency</th>
                    <th className="py-3 px-4">Failure Reason</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-sentinel-border font-mono">
                  {summary.recentFailures.map((check) => (
                    <tr key={check.id} className="hover:bg-slate-800/20">
                      <td className="py-3 px-4 text-slate-400 whitespace-nowrap">
                        {new Date(check.checkedAt).toLocaleTimeString()}
                      </td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                          {check.httpStatusCode ? `HTTP ${check.httpStatusCode}` : 'NO_RESP'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-slate-300">{check.responseTimeMs}ms</td>
                      <td className="py-3 px-4 text-slate-300 truncate max-w-xs" title={check.errorDetails || check.errorReason || ''}>
                        {check.errorReason || 'Outage detected'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Recent Incidents */}
        <div className="bg-sentinel-card border border-sentinel-border rounded-xl overflow-hidden">
          <div className="p-5 border-b border-sentinel-border flex items-center justify-between">
            <div>
              <h3 className="text-sm font-semibold text-white">Outage Incidents</h3>
              <p className="text-xs text-slate-400">Tracked operational outage events</p>
            </div>
            <Link to="/incidents" className="text-xs text-cyan-400 hover:text-cyan-300 font-semibold flex items-center gap-1">
              <span>View All</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="divide-y divide-sentinel-border">
            {(!summary?.recentIncidents || summary.recentIncidents.length === 0) ? (
              <div className="p-8 text-center text-xs text-slate-500">
                No incidents reported. Operational reliability is optimal.
              </div>
            ) : (
              summary.recentIncidents.slice(0, 5).map((inc) => (
                <div key={inc.id} className="p-4 hover:bg-slate-800/20 flex items-center justify-between gap-4">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-xs text-white">{inc.endpointName}</span>
                      <StatusBadge status={inc.status} size="sm" />
                    </div>
                    <p className="text-xs text-slate-400 mt-1 font-mono truncate max-w-md">{inc.rootCause}</p>
                    <span className="text-[11px] text-slate-500 mt-1 block">
                      Started: {new Date(inc.startedAt).toLocaleString()}
                    </span>
                  </div>
                  <Link
                    to={`/incidents/${inc.id}`}
                    className="px-3 py-1.5 rounded-lg bg-sentinel-darkest border border-sentinel-border text-xs text-slate-300 hover:text-white hover:border-slate-500 transition-colors shrink-0"
                  >
                    Details
                  </Link>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
