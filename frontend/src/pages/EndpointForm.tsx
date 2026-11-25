import React, { useState, useEffect } from 'react';
import { endpointService } from '../services/endpointService';
import { EndpointRequest, HttpMethod, MonitoredEndpoint } from '../types';
import { AlertCircle, ShieldAlert } from 'lucide-react';

interface EndpointFormProps {
  initialData?: MonitoredEndpoint | null;
  onSuccess: (endpoint: MonitoredEndpoint) => void;
  onCancel: () => void;
}

export const EndpointForm: React.FC<EndpointFormProps> = ({
  initialData,
  onSuccess,
  onCancel,
}) => {
  const [name, setName] = useState(initialData?.name || '');
  const [url, setUrl] = useState(initialData?.url || '');
  const [httpMethod, setHttpMethod] = useState<HttpMethod>(initialData?.httpMethod || 'GET');
  const [expectedStatusCodes, setExpectedStatusCodes] = useState(initialData?.expectedStatusCodes || '200');
  const [checkIntervalSeconds, setCheckIntervalSeconds] = useState(initialData?.checkIntervalSeconds || 60);
  const [timeoutMillis, setTimeoutMillis] = useState(initialData?.timeoutMillis || 5000);
  const [latencyThresholdMillis, setLatencyThresholdMillis] = useState(initialData?.latencyThresholdMillis || 2000);
  const [consecutiveFailuresThreshold, setConsecutiveFailuresThreshold] = useState(
    initialData?.consecutiveFailuresThreshold || 3
  );

  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (initialData) {
      setName(initialData.name);
      setUrl(initialData.url);
      setHttpMethod(initialData.httpMethod);
      setExpectedStatusCodes(initialData.expectedStatusCodes);
      setCheckIntervalSeconds(initialData.checkIntervalSeconds);
      setTimeoutMillis(initialData.timeoutMillis);
      setLatencyThresholdMillis(initialData.latencyThresholdMillis);
      setConsecutiveFailuresThreshold(initialData.consecutiveFailuresThreshold);
    }
  }, [initialData]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    const payload: EndpointRequest = {
      name,
      url,
      httpMethod,
      expectedStatusCodes,
      checkIntervalSeconds: Number(checkIntervalSeconds),
      timeoutMillis: Number(timeoutMillis),
      latencyThresholdMillis: Number(latencyThresholdMillis),
      consecutiveFailuresThreshold: Number(consecutiveFailuresThreshold),
    };

    try {
      let saved: MonitoredEndpoint;
      if (initialData) {
        saved = await endpointService.updateEndpoint(initialData.id, payload);
      } else {
        saved = await endpointService.createEndpoint(payload);
      }
      onSuccess(saved);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to save endpoint. Please check your inputs.';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 text-xs">
      {error && (
        <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-start gap-2.5 text-rose-400">
          <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {/* Security notice */}
      <div className="p-3 rounded-lg bg-cyan-500/5 border border-cyan-500/15 flex items-start gap-2 text-cyan-300">
        <ShieldAlert className="w-4 h-4 shrink-0 text-cyan-400 mt-0.5" />
        <p className="leading-relaxed">
          SSRF protection is strictly enforced. Internal network IPs, loopback, and cloud metadata targets are rejected.
        </p>
      </div>

      <div>
        <label className="block font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
          Endpoint Name
        </label>
        <input
          type="text"
          required
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="e.g. Auth Service Health"
          className="w-full bg-sentinel-darkest border border-sentinel-border rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500"
        />
      </div>

      <div>
        <label className="block font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
          Target URL (HTTP or HTTPS)
        </label>
        <input
          type="url"
          required
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="https://api.github.com/zen or https://httpbin.org/status/200"
          className="w-full bg-sentinel-darkest border border-sentinel-border rounded-xl px-3.5 py-2.5 text-sm text-white font-mono focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            HTTP Method
          </label>
          <select
            value={httpMethod}
            onChange={(e) => setHttpMethod(e.target.value as HttpMethod)}
            className="w-full bg-sentinel-darkest border border-sentinel-border rounded-xl px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-cyan-500"
          >
            <option value="GET">GET</option>
            <option value="HEAD">HEAD</option>
            <option value="POST">POST</option>
          </select>
        </div>

        <div>
          <label className="block font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            Expected Status Codes
          </label>
          <input
            type="text"
            required
            value={expectedStatusCodes}
            onChange={(e) => setExpectedStatusCodes(e.target.value)}
            placeholder="200 or 200,201,204"
            className="w-full bg-sentinel-darkest border border-sentinel-border rounded-xl px-3.5 py-2 text-sm text-white font-mono focus:outline-none focus:border-cyan-500"
          />
        </div>
      </div>

      <div className="grid grid-cols-3 gap-3">
        <div>
          <label className="block font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            Check Interval (s)
          </label>
          <input
            type="number"
            min={10}
            max={86400}
            required
            value={checkIntervalSeconds}
            onChange={(e) => setCheckIntervalSeconds(Number(e.target.value))}
            className="w-full bg-sentinel-darkest border border-sentinel-border rounded-xl px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-cyan-500"
          />
        </div>

        <div>
          <label className="block font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            Timeout (ms)
          </label>
          <input
            type="number"
            min={500}
            max={15000}
            required
            value={timeoutMillis}
            onChange={(e) => setTimeoutMillis(Number(e.target.value))}
            className="w-full bg-sentinel-darkest border border-sentinel-border rounded-xl px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-cyan-500"
          />
        </div>

        <div>
          <label className="block font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
            Latency SLA (ms)
          </label>
          <input
            type="number"
            min={100}
            max={30000}
            required
            value={latencyThresholdMillis}
            onChange={(e) => setLatencyThresholdMillis(Number(e.target.value))}
            className="w-full bg-sentinel-darkest border border-sentinel-border rounded-xl px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-cyan-500"
          />
        </div>
      </div>

      <div>
        <label className="block font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
          Consecutive Failures Before Alert Incident
        </label>
        <input
          type="number"
          min={1}
          max={10}
          required
          value={consecutiveFailuresThreshold}
          onChange={(e) => setConsecutiveFailuresThreshold(Number(e.target.value))}
          className="w-full bg-sentinel-darkest border border-sentinel-border rounded-xl px-3.5 py-2 text-sm text-white font-mono focus:outline-none focus:border-cyan-500"
        />
        <p className="mt-1 text-[11px] text-slate-500">
          Prevents transient network noise from raising false positive incidents.
        </p>
      </div>

      <div className="pt-4 flex items-center justify-end gap-3 border-t border-sentinel-border">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 rounded-xl bg-sentinel-darkest border border-sentinel-border text-slate-300 hover:text-white transition-colors"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={isSubmitting}
          className="px-5 py-2 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-semibold text-sm transition-colors disabled:opacity-50 flex items-center gap-2"
        >
          {isSubmitting && <div className="w-4 h-4 border-2 border-slate-950/30 border-t-slate-950 rounded-full animate-spin" />}
          <span>{initialData ? 'Update Endpoint' : 'Register Endpoint'}</span>
        </button>
      </div>
    </form>
  );
};
