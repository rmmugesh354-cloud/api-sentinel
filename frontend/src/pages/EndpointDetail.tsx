import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { endpointService } from '../services/endpointService';
import { dashboardService } from '../services/dashboardService';
import { MonitoredEndpoint, HealthCheckResult, LatencyMetrics } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { LatencyChart } from '../components/LatencyChart';
import { Modal } from '../components/Modal';
import { EndpointForm } from './EndpointForm';
import {
  ArrowLeft,
  RefreshCw,
  Play,
  Pause,
  Trash2,
  Edit2,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';

export const EndpointDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [endpoint, setEndpoint] = useState<MonitoredEndpoint | null>(null);
  const [checks, setChecks] = useState<HealthCheckResult[]>([]);
  const [latencyMetrics, setLatencyMetrics] = useState<LatencyMetrics | null>(null);
  const [windowTime, setWindowTime] = useState<'1h' | '24h' | '7d'>('24h');
  const [isLoading, setIsLoading] = useState(true);
  const [isChecking, setIsChecking] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);

  const endpointId = Number(id);

  const fetchEndpointDetails = async () => {
    if (!endpointId) return;
    try {
      const [ep, recentChecks, latData] = await Promise.all([
        endpointService.getEndpoint(endpointId),
        endpointService.getRecentChecks(endpointId, 0, 50),
        dashboardService.getLatencyMetrics(endpointId, windowTime),
      ]);
      setEndpoint(ep);
      setChecks(recentChecks.content);
      setLatencyMetrics(latData);
    } catch (err) {
      console.error('Failed to load endpoint details:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchEndpointDetails();
  }, [endpointId, windowTime]);

  const handleManualCheck = async () => {
    if (!endpoint) return;
    setIsChecking(true);
    setFeedback(null);
    try {
      const res = await endpointService.triggerManualCheck(endpoint.id);
      setFeedback(
        res.isHealthy
          ? `Check completed successfully in ${res.responseTimeMs}ms (HTTP ${res.httpStatusCode})`
          : `Check failed: ${res.errorReason || 'Outage detected'}`
      );
      await fetchEndpointDetails();
    } catch (err: any) {
      setFeedback(err.response?.data?.message || 'Manual probe failed');
    } finally {
      setIsChecking(false);
    }
  };

  const handleTogglePause = async () => {
    if (!endpoint) return;
    try {
      if (endpoint.isActive) {
        await endpointService.pauseEndpoint(endpoint.id);
      } else {
        await endpointService.resumeEndpoint(endpoint.id);
      }
      await fetchEndpointDetails();
    } catch (err) {
      console.error(err);
    }
  };

  const handleDelete = async () => {
    if (!endpoint) return;
    if (window.confirm(`Delete endpoint '${endpoint.name}' and all its telemetry history?`)) {
      await endpointService.deleteEndpoint(endpoint.id);
      navigate('/endpoints');
    }
  };

  if (isLoading || !endpoint) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="w-8 h-8 border-2 border-cyan-500/30 border-t-cyan-400 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Back button and Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <Link
            to="/endpoints"
            className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-cyan-400 mb-2 transition-colors font-medium"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back to Endpoints</span>
          </Link>
          <div className="flex items-center gap-3">
            <h2 className="text-xl font-bold text-white tracking-tight">{endpoint.name}</h2>
            <StatusBadge status={endpoint.currentStatus} />
          </div>
          <p className="text-xs text-slate-400 font-mono mt-1">{endpoint.url}</p>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2">
          <button
            onClick={handleManualCheck}
            disabled={isChecking || !endpoint.isActive}
            className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-semibold text-xs transition-colors shadow-lg shadow-cyan-500/20 disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isChecking ? 'animate-spin' : ''}`} />
            <span>Check Now</span>
          </button>

          <button
            onClick={handleTogglePause}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-sentinel-card border border-sentinel-border text-slate-300 hover:text-white transition-colors text-xs"
          >
            {endpoint.isActive ? (
              <>
                <Pause className="w-3.5 h-3.5 text-amber-400" />
                <span>Pause</span>
              </>
            ) : (
              <>
                <Play className="w-3.5 h-3.5 text-emerald-400" />
                <span>Resume</span>
              </>
            )}
          </button>

          <button
            onClick={() => setIsEditModalOpen(true)}
            className="p-2 rounded-xl bg-sentinel-card border border-sentinel-border text-slate-300 hover:text-white transition-colors"
            title="Edit endpoint"
          >
            <Edit2 className="w-4 h-4" />
          </button>

          <button
            onClick={handleDelete}
            className="p-2 rounded-xl bg-sentinel-card border border-sentinel-border text-rose-400 hover:bg-rose-500/10 transition-colors"
            title="Delete endpoint"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      {feedback && (
        <div className="p-3.5 rounded-xl bg-sentinel-card border border-sentinel-border text-xs font-mono text-cyan-300">
          {feedback}
        </div>
      )}

      {/* Configuration & Health SLA Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="bg-sentinel-card border border-sentinel-border rounded-xl p-3.5">
          <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider block">HTTP Method</span>
          <span className="text-sm font-bold font-mono text-white mt-1 block">{endpoint.httpMethod}</span>
        </div>
        <div className="bg-sentinel-card border border-sentinel-border rounded-xl p-3.5">
          <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider block">Expected Status</span>
          <span className="text-sm font-bold font-mono text-emerald-400 mt-1 block">{endpoint.expectedStatusCodes}</span>
        </div>
        <div className="bg-sentinel-card border border-sentinel-border rounded-xl p-3.5">
          <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider block">Check Interval</span>
          <span className="text-sm font-bold font-mono text-white mt-1 block">{endpoint.checkIntervalSeconds}s</span>
        </div>
        <div className="bg-sentinel-card border border-sentinel-border rounded-xl p-3.5">
          <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider block">Timeout Limit</span>
          <span className="text-sm font-bold font-mono text-white mt-1 block">{endpoint.timeoutMillis}ms</span>
        </div>
        <div className="bg-sentinel-card border border-sentinel-border rounded-xl p-3.5">
          <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider block">Latency SLA</span>
          <span className="text-sm font-bold font-mono text-cyan-400 mt-1 block">&le; {endpoint.latencyThresholdMillis}ms</span>
        </div>
        <div className="bg-sentinel-card border border-sentinel-border rounded-xl p-3.5">
          <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider block">Outage Threshold</span>
          <span className="text-sm font-bold font-mono text-amber-400 mt-1 block">{endpoint.consecutiveFailuresThreshold} fails</span>
        </div>
      </div>

      {/* Latency History Chart */}
      <div className="bg-sentinel-card border border-sentinel-border rounded-xl p-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
          <div>
            <h3 className="text-sm font-bold text-white">Response Latency Profile</h3>
            <p className="text-xs text-slate-400">Response time (ms) against configured latency threshold</p>
          </div>

          <div className="flex items-center gap-1 bg-sentinel-darkest p-1 rounded-xl border border-sentinel-border self-start">
            {(['1h', '24h', '7d'] as const).map((win) => (
              <button
                key={win}
                onClick={() => setWindowTime(win)}
                className={`px-3 py-1 rounded-lg text-xs font-semibold font-mono transition-colors ${
                  windowTime === win
                    ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                {win.toUpperCase()}
              </button>
            ))}
          </div>
        </div>

        {/* Statistical Percentiles for this endpoint */}
        <div className="grid grid-cols-4 gap-3 mb-6">
          <div className="bg-sentinel-darkest/50 border border-sentinel-border rounded-lg p-2.5 text-center">
            <span className="text-[10px] text-slate-400 font-semibold uppercase">p50 Latency</span>
            <span className="text-base font-bold font-mono text-cyan-400 block mt-0.5">{latencyMetrics?.p50Ms || 0} ms</span>
          </div>
          <div className="bg-sentinel-darkest/50 border border-sentinel-border rounded-lg p-2.5 text-center">
            <span className="text-[10px] text-slate-400 font-semibold uppercase">p95 Latency</span>
            <span className="text-base font-bold font-mono text-amber-400 block mt-0.5">{latencyMetrics?.p95Ms || 0} ms</span>
          </div>
          <div className="bg-sentinel-darkest/50 border border-sentinel-border rounded-lg p-2.5 text-center">
            <span className="text-[10px] text-slate-400 font-semibold uppercase">Average</span>
            <span className="text-base font-bold font-mono text-slate-200 block mt-0.5">{latencyMetrics?.averageMs?.toFixed(1) || 0} ms</span>
          </div>
          <div className="bg-sentinel-darkest/50 border border-sentinel-border rounded-lg p-2.5 text-center">
            <span className="text-[10px] text-slate-400 font-semibold uppercase">Probes Count</span>
            <span className="text-base font-bold font-mono text-emerald-400 block mt-0.5">{latencyMetrics?.sampleCount || 0}</span>
          </div>
        </div>

        <LatencyChart
          data={latencyMetrics?.timeSeries || []}
          thresholdMs={endpoint.latencyThresholdMillis}
          height={260}
        />
      </div>

      {/* Probes History Table */}
      <div className="bg-sentinel-card border border-sentinel-border rounded-xl overflow-hidden shadow-xl">
        <div className="p-5 border-b border-sentinel-border">
          <h3 className="text-sm font-bold text-white">Recent Health Check Telemetry</h3>
          <p className="text-xs text-slate-400">Detailed historical probes, status codes, and error details</p>
        </div>

        <div className="overflow-x-auto">
          {checks.length === 0 ? (
            <div className="p-12 text-center text-xs text-slate-500">
              No health check records available yet. Click "Check Now" or wait for scheduled execution.
            </div>
          ) : (
            <table className="w-full text-left text-xs font-mono">
              <thead className="bg-sentinel-darkest text-slate-400 font-semibold border-b border-sentinel-border font-sans">
                <tr>
                  <th className="py-3 px-6">Timestamp</th>
                  <th className="py-3 px-4">Result</th>
                  <th className="py-3 px-4">HTTP Status</th>
                  <th className="py-3 px-4">Latency</th>
                  <th className="py-3 px-4">Payload Size</th>
                  <th className="py-3 px-6">Diagnostic Message</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-sentinel-border">
                {checks.map((chk) => (
                  <tr key={chk.id} className="hover:bg-slate-800/20">
                    <td className="py-3 px-6 text-slate-400 whitespace-nowrap">
                      {new Date(chk.checkedAt).toLocaleString()}
                    </td>

                    <td className="py-3 px-4">
                      {chk.isHealthy ? (
                        <span className="inline-flex items-center gap-1 text-emerald-400 font-semibold">
                          <CheckCircle2 className="w-3.5 h-3.5" /> Pass
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-rose-400 font-semibold">
                          <AlertTriangle className="w-3.5 h-3.5" /> Fail
                        </span>
                      )}
                    </td>

                    <td className="py-3 px-4">
                      <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${
                        chk.httpStatusCode && chk.httpStatusCode < 400
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                          : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                      }`}>
                        {chk.httpStatusCode ? `HTTP ${chk.httpStatusCode}` : 'NO_RESPONSE'}
                      </span>
                    </td>

                    <td className="py-3 px-4 text-slate-200">
                      {chk.responseTimeMs} ms
                    </td>

                    <td className="py-3 px-4 text-slate-400">
                      {chk.responseSizeBytes > 0 ? `${(chk.responseSizeBytes / 1024).toFixed(1)} KB` : '0 B'}
                    </td>

                    <td className="py-3 px-6 text-slate-300 font-sans max-w-sm truncate" title={chk.errorDetails || chk.errorReason || ''}>
                      {chk.errorReason ? (
                        <span className="text-rose-400 font-medium">{chk.errorReason}: {chk.errorDetails}</span>
                      ) : (
                        <span className="text-slate-500">None (healthy)</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Edit Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title="Edit Monitored Endpoint"
        maxWidth="max-w-xl"
      >
        <EndpointForm
          initialData={endpoint}
          onSuccess={() => {
            setIsEditModalOpen(false);
            fetchEndpointDetails();
          }}
          onCancel={() => setIsEditModalOpen(false)}
        />
      </Modal>
    </div>
  );
};
