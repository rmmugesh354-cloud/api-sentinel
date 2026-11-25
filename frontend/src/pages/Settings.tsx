import React, { useEffect, useState } from 'react';
import { settingsService } from '../services/settingsService';
import { useAuth } from '../context/AuthContext';
import { Mail, Shield, Check, AlertCircle } from 'lucide-react';

export const Settings: React.FC = () => {
  const { user } = useAuth();
  const [emailEnabled, setEmailEnabled] = useState(false);
  const [notificationEmail, setNotificationEmail] = useState('');
  const [defaultInterval, setDefaultInterval] = useState(60);
  const [defaultFailures, setDefaultFailures] = useState(3);

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    const loadSettings = async () => {
      try {
        const s = await settingsService.getSettings();
        setEmailEnabled(s.emailNotificationsEnabled);
        setNotificationEmail(s.notificationEmail || user?.email || '');
        setDefaultInterval(s.defaultCheckIntervalSeconds);
        setDefaultFailures(s.defaultConsecutiveFailures);
      } catch (err) {
        console.error('Failed to load settings:', err);
      } finally {
        setIsLoading(false);
      }
    };

    loadSettings();
  }, [user]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setSuccessMsg(null);
    setErrorMsg(null);

    try {
      await settingsService.updateSettings({
        emailNotificationsEnabled: emailEnabled,
        notificationEmail: notificationEmail.trim() || null,
        defaultCheckIntervalSeconds: Number(defaultInterval),
        defaultConsecutiveFailures: Number(defaultFailures),
      });
      setSuccessMsg('Monitoring preferences and alert settings successfully updated.');
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || 'Failed to update settings');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="w-8 h-8 border-2 border-cyan-500/30 border-t-cyan-400 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-white tracking-tight">Account & Monitoring Settings</h2>
        <p className="text-xs text-slate-400 mt-0.5">
          Configure notification dispatching preferences, SMTP alerts, and default scheduling policies
        </p>
      </div>

      {successMsg && (
        <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-xs font-semibold text-emerald-400 flex items-center gap-2">
          <Check className="w-4 h-4" />
          <span>{successMsg}</span>
        </div>
      )}

      {errorMsg && (
        <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-xs font-semibold text-rose-400 flex items-center gap-2">
          <AlertCircle className="w-4 h-4" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Profile Card */}
      <div className="bg-sentinel-card border border-sentinel-border rounded-xl p-6 shadow-xl">
        <h3 className="text-sm font-bold text-white mb-4">User Profile</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div>
            <span className="text-slate-500 block mb-1">Full Name</span>
            <span className="font-semibold text-white bg-sentinel-darkest border border-sentinel-border px-3.5 py-2 rounded-xl block">
              {user?.fullName}
            </span>
          </div>
          <div>
            <span className="text-slate-500 block mb-1">Account Email</span>
            <span className="font-mono text-white bg-sentinel-darkest border border-sentinel-border px-3.5 py-2 rounded-xl block">
              {user?.email}
            </span>
          </div>
        </div>
      </div>

      {/* Settings Form */}
      <form onSubmit={handleSave} className="bg-sentinel-card border border-sentinel-border rounded-xl p-6 shadow-xl space-y-6">
        {/* Email Notifications */}
        <div>
          <h3 className="text-sm font-bold text-white mb-1 flex items-center gap-2">
            <Mail className="w-4 h-4 text-cyan-400" />
            <span>Outage Alert Notifications</span>
          </h3>
          <p className="text-xs text-slate-400 mb-4">
            Receive automated incident alerts when an API is experiencing an outage.
          </p>

          <div className="space-y-4">
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={emailEnabled}
                onChange={(e) => setEmailEnabled(e.target.checked)}
                className="w-4 h-4 rounded text-cyan-500 bg-sentinel-darkest border-sentinel-border focus:ring-0 focus:ring-offset-0"
              />
              <span className="text-xs font-medium text-slate-200">
                Enable Email Notifications (dispatches on incident opened and resolved)
              </span>
            </label>

            {emailEnabled && (
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Notification Recipient Email
                </label>
                <input
                  type="email"
                  required={emailEnabled}
                  value={notificationEmail}
                  onChange={(e) => setNotificationEmail(e.target.value)}
                  placeholder="alerts@company.com"
                  className="w-full bg-sentinel-darkest border border-sentinel-border rounded-xl px-3.5 py-2.5 text-xs text-white font-mono focus:outline-none focus:border-cyan-500"
                />
                <p className="text-[11px] text-slate-500 mt-1">
                  If SMTP credentials are not configured on the backend, delivery attempts fail gracefully without interruption.
                </p>
              </div>
            )}
          </div>
        </div>

        <div className="border-t border-sentinel-border pt-6">
          <h3 className="text-sm font-bold text-white mb-1 flex items-center gap-2">
            <Shield className="w-4 h-4 text-cyan-400" />
            <span>Default Endpoint Thresholds</span>
          </h3>
          <p className="text-xs text-slate-400 mb-4">
            Default parameters prefilled when registering new endpoints
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Default Check Interval (seconds)
              </label>
              <input
                type="number"
                min={10}
                max={86400}
                required
                value={defaultInterval}
                onChange={(e) => setDefaultInterval(Number(e.target.value))}
                className="w-full bg-sentinel-darkest border border-sentinel-border rounded-xl px-3.5 py-2.5 text-xs text-white font-mono focus:outline-none focus:border-cyan-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Default Failure Count Threshold
              </label>
              <input
                type="number"
                min={1}
                max={10}
                required
                value={defaultFailures}
                onChange={(e) => setDefaultFailures(Number(e.target.value))}
                className="w-full bg-sentinel-darkest border border-sentinel-border rounded-xl px-3.5 py-2.5 text-xs text-white font-mono focus:outline-none focus:border-cyan-500"
              />
            </div>
          </div>
        </div>

        <div className="pt-4 flex justify-end">
          <button
            type="submit"
            disabled={isSaving}
            className="px-5 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold text-xs transition-colors flex items-center gap-2 disabled:opacity-50 shadow-lg shadow-cyan-500/20"
          >
            {isSaving && <div className="w-3.5 h-3.5 border-2 border-slate-950/30 border-t-slate-950 rounded-full animate-spin" />}
            <span>Save Preferences</span>
          </button>
        </div>
      </form>
    </div>
  );
};
