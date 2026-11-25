import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { incidentService } from '../services/incidentService';
import { Incident } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import {
  ArrowLeft,
  Clock,
  Check,
  ExternalLink,
} from 'lucide-react';

export const IncidentDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [incident, setIncident] = useState<Incident | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isAcknowledging, setIsAcknowledging] = useState(false);

  const incidentId = Number(id);

  const fetchIncident = async () => {
    if (!incidentId) return;
    try {
      const res = await incidentService.getIncident(incidentId);
      setIncident(res);
    } catch (err) {
      console.error('Failed to load incident:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchIncident();
  }, [incidentId]);

  const handleAcknowledge = async () => {
    if (!incident) return;
    setIsAcknowledging(true);
    try {
      const updated = await incidentService.acknowledgeIncident(incident.id);
      setIncident(updated);
    } catch (err) {
      console.error(err);
    } finally {
      setIsAcknowledging(false);
    }
  };

  const formatDuration = (seconds: number | null) => {
    if (seconds === null) return 'Outage Active';
    if (seconds < 60) return `${seconds} seconds`;
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${secs}s`;
  };

  if (isLoading || !incident) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="w-8 h-8 border-2 border-cyan-500/30 border-t-cyan-400 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Back button and Header */}
      <div>
        <Link
          to="/incidents"
          className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-cyan-400 mb-2 transition-colors font-medium"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Incidents</span>
        </Link>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <h2 className="text-xl font-bold text-white tracking-tight">Incident #{incident.id}</h2>
            <StatusBadge status={incident.status} />
          </div>

          {incident.status === 'OPEN' && (
            <button
              onClick={handleAcknowledge}
              disabled={isAcknowledging}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-amber-500/15 hover:bg-amber-500/25 border border-amber-500/30 text-amber-300 font-semibold text-xs transition-colors"
            >
              <Check className="w-4 h-4" />
              <span>{isAcknowledging ? 'Acknowledging...' : 'Acknowledge Incident'}</span>
            </button>
          )}
        </div>
      </div>

      {/* Incident Summary Card */}
      <div className="bg-sentinel-card border border-sentinel-border rounded-xl p-6 shadow-xl space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-sentinel-darkest/60 border border-sentinel-border rounded-xl p-4">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">Target Service</span>
            <Link
              to={`/endpoints/${incident.endpointId}`}
              className="font-bold text-sm text-cyan-400 hover:underline mt-1 block flex items-center gap-1"
            >
              <span>{incident.endpointName}</span>
              <ExternalLink className="w-3 h-3" />
            </Link>
          </div>

          <div className="bg-sentinel-darkest/60 border border-sentinel-border rounded-xl p-4">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">Root Cause</span>
            <span className="font-bold font-mono text-sm text-rose-400 mt-1 block">{incident.rootCause}</span>
          </div>

          <div className="bg-sentinel-darkest/60 border border-sentinel-border rounded-xl p-4">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">Total Duration</span>
            <span className="font-bold font-mono text-sm text-slate-200 mt-1 block">
              {formatDuration(incident.durationSeconds)}
            </span>
          </div>

          <div className="bg-sentinel-darkest/60 border border-sentinel-border rounded-xl p-4">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">Failure Probes</span>
            <span className="font-bold font-mono text-sm text-amber-400 mt-1 block">{incident.failureCount} checks</span>
          </div>
        </div>

        {incident.errorSummary && (
          <div className="bg-sentinel-darkest/80 border border-sentinel-border rounded-xl p-4">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">
              Error Diagnostics Summary
            </span>
            <p className="text-xs font-mono text-slate-300 leading-relaxed break-words">{incident.errorSummary}</p>
          </div>
        )}
      </div>

      {/* Audit Event Timeline */}
      <div className="bg-sentinel-card border border-sentinel-border rounded-xl p-6 shadow-xl">
        <h3 className="text-sm font-bold text-white mb-6 flex items-center gap-2">
          <Clock className="w-4 h-4 text-cyan-400" />
          <span>Incident Progression Timeline</span>
        </h3>

        <div className="relative pl-6 space-y-8 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-sentinel-border">
          {incident.events.map((ev, index) => {
            const isResolved = ev.eventType === 'INCIDENT_RESOLVED';
            const isAck = ev.eventType === 'INCIDENT_ACKNOWLEDGED';

            const dotColor = isResolved
              ? 'bg-emerald-400 ring-4 ring-emerald-500/20'
              : isAck
              ? 'bg-amber-400 ring-4 ring-amber-500/20'
              : 'bg-rose-400 ring-4 ring-rose-500/20';

            return (
              <div key={ev.id || index} className="relative group">
                <span className={`absolute -left-[27px] top-1 w-3 h-3 rounded-full ${dotColor}`} />
                <div className="bg-sentinel-darkest/60 border border-sentinel-border rounded-xl p-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1 mb-1.5">
                    <span className="text-xs font-bold text-white uppercase tracking-wider font-mono">
                      {ev.eventType.replace('_', ' ')}
                    </span>
                    <span className="text-[11px] text-slate-500 font-mono">
                      {new Date(ev.createdAt).toLocaleString()}
                    </span>
                  </div>
                  <p className="text-xs text-slate-300 font-mono leading-relaxed">{ev.description}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
