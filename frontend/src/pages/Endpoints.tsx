import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { endpointService } from '../services/endpointService';
import { MonitoredEndpoint } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { EndpointForm } from './EndpointForm';
import {
  Plus,
  Play,
  Pause,
  RefreshCw,
  Trash2,
  Edit2,
  ExternalLink,
  Search,
  Activity,
} from 'lucide-react';

export const Endpoints: React.FC = () => {
  const [endpoints, setEndpoints] = useState<MonitoredEndpoint[]>([]);
  const [filteredEndpoints, setFilteredEndpoints] = useState<MonitoredEndpoint[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingEndpoint, setEditingEndpoint] = useState<MonitoredEndpoint | null>(null);

  // In-flight manual checks tracking
  const [checkingEndpointIds, setCheckingEndpointIds] = useState<Set<number>>(new Set());
  const [feedbackMessage, setFeedbackMessage] = useState<{ id: number; text: string; success: boolean } | null>(null);

  const fetchEndpoints = async () => {
    try {
      const res = await endpointService.getAllEndpoints();
      setEndpoints(res);
      setFilteredEndpoints(res);
    } catch (err) {
      console.error('Failed to load endpoints:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchEndpoints();
  }, []);

  useEffect(() => {
    if (!searchTerm.trim()) {
      setFilteredEndpoints(endpoints);
    } else {
      const term = searchTerm.toLowerCase();
      setFilteredEndpoints(
        endpoints.filter(
          (ep) =>
            ep.name.toLowerCase().includes(term) ||
            ep.url.toLowerCase().includes(term) ||
            ep.currentStatus.toLowerCase().includes(term)
        )
      );
    }
  }, [searchTerm, endpoints]);

  const handleTriggerCheck = async (id: number) => {
    setCheckingEndpointIds((prev) => new Set(prev).add(id));
    setFeedbackMessage(null);

    try {
      const result = await endpointService.triggerManualCheck(id);
      setFeedbackMessage({
        id,
        text: result.isHealthy
          ? `Check succeeded (${result.responseTimeMs}ms, HTTP ${result.httpStatusCode})`
          : `Check failed: ${result.errorReason || 'Outage detected'}`,
        success: result.isHealthy,
      });
      // Refresh list to update endpoint status
      await fetchEndpoints();
    } catch (err: any) {
      setFeedbackMessage({
        id,
        text: err.response?.data?.message || 'Manual check failed',
        success: false,
      });
    } finally {
      setCheckingEndpointIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
    }
  };

  const handleTogglePause = async (ep: MonitoredEndpoint) => {
    try {
      if (ep.isActive) {
        await endpointService.pauseEndpoint(ep.id);
      } else {
        await endpointService.resumeEndpoint(ep.id);
      }
      await fetchEndpoints();
    } catch (err) {
      console.error('Failed to toggle pause state:', err);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!window.confirm(`Are you sure you want to delete '${name}'? This will delete all historical checks and incidents.`)) {
      return;
    }
    try {
      await endpointService.deleteEndpoint(id);
      await fetchEndpoints();
    } catch (err) {
      console.error('Failed to delete endpoint:', err);
    }
  };

  const handleFormSuccess = () => {
    setIsModalOpen(false);
    setEditingEndpoint(null);
    fetchEndpoints();
  };

  return (
    <div className="space-y-6">
      {/* Header and Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white tracking-tight">Monitored Endpoints</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Configure automated health checks, expected responses, and alert thresholds
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search endpoints..."
              className="bg-sentinel-darkest border border-sentinel-border rounded-xl pl-9 pr-4 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500"
            />
          </div>

          <button
            onClick={() => {
              setEditingEndpoint(null);
              setIsModalOpen(true);
            }}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-semibold text-xs transition-colors shadow-lg shadow-cyan-500/20"
          >
            <Plus className="w-4 h-4" />
            <span>Register Endpoint</span>
          </button>
        </div>
      </div>

      {/* Endpoints Table Card */}
      <div className="bg-sentinel-card border border-sentinel-border rounded-xl overflow-hidden shadow-xl">
        {isLoading ? (
          <div className="p-12 text-center">
            <div className="w-8 h-8 border-2 border-cyan-500/30 border-t-cyan-400 rounded-full animate-spin mx-auto" />
            <p className="mt-3 text-xs text-slate-400">Loading registered endpoints...</p>
          </div>
        ) : filteredEndpoints.length === 0 ? (
          <div className="p-16 text-center">
            <div className="w-12 h-12 rounded-2xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400 mx-auto mb-4">
              <Activity className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-bold text-white">No Monitored Endpoints Found</h3>
            <p className="mt-1 text-xs text-slate-400 max-w-sm mx-auto">
              {searchTerm
                ? 'No endpoints match your search criteria. Try a different query.'
                : 'Get started by registering an HTTP/HTTPS endpoint to monitor availability and response times.'}
            </p>
            {!searchTerm && (
              <button
                onClick={() => setIsModalOpen(true)}
                className="mt-4 px-4 py-2 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-semibold text-xs inline-flex items-center gap-2 transition-colors"
              >
                <Plus className="w-4 h-4" />
                <span>Register Endpoint</span>
              </button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-sentinel-darkest text-slate-400 font-semibold border-b border-sentinel-border">
                <tr>
                  <th className="py-3.5 px-6">Endpoint</th>
                  <th className="py-3.5 px-4">Method</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4">Interval</th>
                  <th className="py-3.5 px-4">Latency SLA</th>
                  <th className="py-3.5 px-4">Last Checked</th>
                  <th className="py-3.5 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-sentinel-border">
                {filteredEndpoints.map((ep) => {
                  const isChecking = checkingEndpointIds.has(ep.id);
                  const feedback = feedbackMessage?.id === ep.id ? feedbackMessage : null;

                  return (
                    <tr key={ep.id} className="hover:bg-slate-800/20 transition-colors group">
                      <td className="py-4 px-6">
                        <div>
                          <Link
                            to={`/endpoints/${ep.id}`}
                            className="font-bold text-sm text-white group-hover:text-cyan-400 transition-colors flex items-center gap-1.5"
                          >
                            <span>{ep.name}</span>
                            <ExternalLink className="w-3.5 h-3.5 opacity-0 group-hover:opacity-100 transition-opacity" />
                          </Link>
                          <span className="font-mono text-[11px] text-slate-400 truncate block max-w-md mt-0.5" title={ep.url}>
                            {ep.url}
                          </span>
                          {feedback && (
                            <span className={`text-[11px] font-mono mt-1 block ${feedback.success ? 'text-emerald-400' : 'text-rose-400'}`}>
                              {feedback.text}
                            </span>
                          )}
                        </div>
                      </td>

                      <td className="py-4 px-4 font-mono">
                        <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-slate-800 text-slate-300 border border-slate-700">
                          {ep.httpMethod}
                        </span>
                      </td>

                      <td className="py-4 px-4">
                        <StatusBadge status={ep.currentStatus} />
                      </td>

                      <td className="py-4 px-4 font-mono text-slate-300">
                        {ep.checkIntervalSeconds}s
                      </td>

                      <td className="py-4 px-4 font-mono text-slate-300">
                        &le; {ep.latencyThresholdMillis}ms
                      </td>

                      <td className="py-4 px-4 text-slate-400 font-mono text-[11px]">
                        {ep.lastCheckedAt ? new Date(ep.lastCheckedAt).toLocaleTimeString() : 'Pending'}
                      </td>

                      <td className="py-4 px-6 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {/* Manual Check Trigger */}
                          <button
                            onClick={() => handleTriggerCheck(ep.id)}
                            disabled={isChecking || !ep.isActive}
                            title="Trigger immediate health probe"
                            className="p-1.5 rounded-lg bg-sentinel-darkest border border-sentinel-border text-slate-300 hover:text-cyan-400 hover:border-cyan-500/40 transition-colors disabled:opacity-40"
                          >
                            <RefreshCw className={`w-3.5 h-3.5 ${isChecking ? 'animate-spin text-cyan-400' : ''}`} />
                          </button>

                          {/* Pause / Resume */}
                          <button
                            onClick={() => handleTogglePause(ep)}
                            title={ep.isActive ? 'Pause monitoring' : 'Resume monitoring'}
                            className="p-1.5 rounded-lg bg-sentinel-darkest border border-sentinel-border text-slate-300 hover:text-amber-400 hover:border-amber-500/40 transition-colors"
                          >
                            {ep.isActive ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5 text-emerald-400" />}
                          </button>

                          {/* Edit */}
                          <button
                            onClick={() => {
                              setEditingEndpoint(ep);
                              setIsModalOpen(true);
                            }}
                            title="Edit endpoint configuration"
                            className="p-1.5 rounded-lg bg-sentinel-darkest border border-sentinel-border text-slate-300 hover:text-white hover:border-slate-500 transition-colors"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>

                          {/* Delete */}
                          <button
                            onClick={() => handleDelete(ep.id, ep.name)}
                            title="Delete endpoint"
                            className="p-1.5 rounded-lg bg-sentinel-darkest border border-sentinel-border text-slate-300 hover:text-rose-400 hover:border-rose-500/40 transition-colors"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal for Registering / Editing Endpoint */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setEditingEndpoint(null);
        }}
        title={editingEndpoint ? 'Edit Monitored Endpoint' : 'Register New Endpoint'}
        maxWidth="max-w-xl"
      >
        <EndpointForm
          initialData={editingEndpoint}
          onSuccess={handleFormSuccess}
          onCancel={() => {
            setIsModalOpen(false);
            setEditingEndpoint(null);
          }}
        />
      </Modal>
    </div>
  );
};
