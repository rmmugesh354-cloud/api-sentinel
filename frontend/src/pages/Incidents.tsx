import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { incidentService } from '../services/incidentService';
import { Incident, IncidentStatus } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import {
  CheckCircle,
  ExternalLink,
  Check,
} from 'lucide-react';

export const Incidents: React.FC = () => {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [statusFilter, setStatusFilter] = useState<IncidentStatus | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(true);
  const [acknowledgingId, setAcknowledgingId] = useState<number | null>(null);

  const fetchIncidents = async () => {
    try {
      const res = await incidentService.getIncidents(statusFilter, 0, 50);
      setIncidents(res.content);
    } catch (err) {
      console.error('Failed to load incidents:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchIncidents();
  }, [statusFilter]);

  const handleAcknowledge = async (id: number) => {
    setAcknowledgingId(id);
    try {
      await incidentService.acknowledgeIncident(id);
      await fetchIncidents();
    } catch (err) {
      console.error('Failed to acknowledge incident:', err);
    } finally {
      setAcknowledgingId(null);
    }
  };

  const formatDuration = (seconds: number | null) => {
    if (seconds === null) return 'Ongoing';
    if (seconds < 60) return `${seconds}s`;
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    if (mins < 60) return `${mins}m ${secs}s`;
    const hrs = Math.floor(mins / 60);
    return `${hrs}h ${mins % 60}m`;
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white tracking-tight">Outage Incident History</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Real-time tracking of operational failures, deduplicated alarms, and recovery resolution
          </p>
        </div>

        {/* Filter Pills */}
        <div className="flex items-center gap-1 bg-sentinel-darkest p-1 rounded-xl border border-sentinel-border self-start">
          <button
            onClick={() => setStatusFilter(undefined)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
              statusFilter === undefined
                ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            All
          </button>
          <button
            onClick={() => setStatusFilter('OPEN')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
              statusFilter === 'OPEN'
                ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Open Alarms
          </button>
          <button
            onClick={() => setStatusFilter('ACKNOWLEDGED')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
              statusFilter === 'ACKNOWLEDGED'
                ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Acknowledged
          </button>
          <button
            onClick={() => setStatusFilter('RESOLVED')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
              statusFilter === 'RESOLVED'
                ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Resolved
          </button>
        </div>
      </div>

      {/* Incidents Table Card */}
      <div className="bg-sentinel-card border border-sentinel-border rounded-xl overflow-hidden shadow-xl">
        {isLoading ? (
          <div className="p-12 text-center">
            <div className="w-8 h-8 border-2 border-cyan-500/30 border-t-cyan-400 rounded-full animate-spin mx-auto" />
            <p className="mt-3 text-xs text-slate-400">Loading incident records...</p>
          </div>
        ) : incidents.length === 0 ? (
          <div className="p-16 text-center">
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 mx-auto mb-4">
              <CheckCircle className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-bold text-white">No Incidents Reported</h3>
            <p className="mt-1 text-xs text-slate-400 max-w-sm mx-auto">
              {statusFilter
                ? `No incidents found with status '${statusFilter}'.`
                : 'All registered endpoints are currently operating within defined health policies.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-sentinel-darkest text-slate-400 font-semibold border-b border-sentinel-border">
                <tr>
                  <th className="py-3.5 px-6">Status</th>
                  <th className="py-3.5 px-4">Affected Service</th>
                  <th className="py-3.5 px-4">Root Cause</th>
                  <th className="py-3.5 px-4">Consecutive Failures</th>
                  <th className="py-3.5 px-4">Started At</th>
                  <th className="py-3.5 px-4">Duration</th>
                  <th className="py-3.5 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-sentinel-border font-mono">
                {incidents.map((inc) => (
                  <tr key={inc.id} className="hover:bg-slate-800/20 transition-colors">
                    <td className="py-4 px-6 font-sans">
                      <StatusBadge status={inc.status} />
                    </td>

                    <td className="py-4 px-4 font-sans">
                      <Link
                        to={`/endpoints/${inc.endpointId}`}
                        className="font-bold text-white hover:text-cyan-400 transition-colors flex items-center gap-1.5"
                      >
                        <span>{inc.endpointName}</span>
                        <ExternalLink className="w-3 h-3 text-slate-500" />
                      </Link>
                      <span className="text-[11px] font-mono text-slate-500 block truncate max-w-xs mt-0.5">
                        {inc.endpointUrl}
                      </span>
                    </td>

                    <td className="py-4 px-4 text-slate-300 max-w-xs truncate" title={inc.errorSummary || inc.rootCause}>
                      <span className="text-rose-400 font-semibold">{inc.rootCause}</span>
                    </td>

                    <td className="py-4 px-4 text-slate-300">
                      {inc.failureCount}
                    </td>

                    <td className="py-4 px-4 text-slate-400 text-[11px]">
                      {new Date(inc.startedAt).toLocaleString()}
                    </td>

                    <td className="py-4 px-4 text-slate-300">
                      {formatDuration(inc.durationSeconds)}
                    </td>

                    <td className="py-4 px-6 text-right font-sans">
                      <div className="flex items-center justify-end gap-2">
                        {inc.status === 'OPEN' && (
                          <button
                            onClick={() => handleAcknowledge(inc.id)}
                            disabled={acknowledgingId === inc.id}
                            className="px-2.5 py-1 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 border border-amber-500/30 text-amber-400 font-semibold text-xs flex items-center gap-1 transition-colors"
                          >
                            <Check className="w-3.5 h-3.5" />
                            <span>{acknowledgingId === inc.id ? 'Saving...' : 'Acknowledge'}</span>
                          </button>
                        )}

                        <Link
                          to={`/incidents/${inc.id}`}
                          className="px-3 py-1 rounded-lg bg-sentinel-darkest border border-sentinel-border text-slate-300 hover:text-white hover:border-slate-500 transition-colors"
                        >
                          Audit Trail
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
