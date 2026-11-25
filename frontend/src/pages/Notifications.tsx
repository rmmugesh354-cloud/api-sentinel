import React, { useEffect, useState } from 'react';
import { notificationService } from '../services/notificationService';
import { Notification } from '../types';
import { Bell, CheckCheck, Mail, AlertTriangle, CheckCircle } from 'lucide-react';

export const Notifications: React.FC = () => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [isLoading, setIsLoading] = useState(true);

  const fetchNotifications = async () => {
    try {
      const [listRes, count] = await Promise.all([
        notificationService.getNotifications(0, 50),
        notificationService.getUnreadCount(),
      ]);
      setNotifications(listRes.content);
      setUnreadCount(count);
    } catch (err) {
      console.error('Failed to load notifications:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const handleMarkAsRead = async (id: number) => {
    try {
      await notificationService.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (err) {
      console.error(err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white tracking-tight">Alert Notifications Feed</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Audit history of dispatched incident alarms and recovery alerts across all configured channels
          </p>
        </div>

        {unreadCount > 0 && (
          <button
            onClick={handleMarkAllAsRead}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-sentinel-card border border-sentinel-border text-xs text-cyan-400 hover:text-cyan-300 hover:border-cyan-500/40 transition-colors self-start"
          >
            <CheckCheck className="w-4 h-4" />
            <span>Mark All as Read ({unreadCount})</span>
          </button>
        )}
      </div>

      {/* Notifications List Card */}
      <div className="bg-sentinel-card border border-sentinel-border rounded-xl overflow-hidden shadow-xl">
        {isLoading ? (
          <div className="p-12 text-center">
            <div className="w-8 h-8 border-2 border-cyan-500/30 border-t-cyan-400 rounded-full animate-spin mx-auto" />
            <p className="mt-3 text-xs text-slate-400">Loading alerts...</p>
          </div>
        ) : notifications.length === 0 ? (
          <div className="p-16 text-center">
            <div className="w-12 h-12 rounded-2xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400 mx-auto mb-4">
              <Bell className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-bold text-white">No Alert Notifications</h3>
            <p className="mt-1 text-xs text-slate-400 max-w-sm mx-auto">
              You will receive real-time notifications here when endpoints trigger outage incidents or recover.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-sentinel-border">
            {notifications.map((n) => {
              const isAlert = n.title.toLowerCase().includes('triggered') || n.title.toLowerCase().includes('failing');

              return (
                <div
                  key={n.id}
                  onClick={() => !n.isRead && handleMarkAsRead(n.id)}
                  className={`p-5 flex items-start gap-4 transition-colors cursor-pointer ${
                    n.isRead ? 'hover:bg-slate-800/20 opacity-80' : 'bg-cyan-500/[0.03] hover:bg-cyan-500/[0.06]'
                  }`}
                >
                  <div
                    className={`p-2.5 rounded-xl shrink-0 mt-0.5 ${
                      isAlert
                        ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                        : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                    }`}
                  >
                    {isAlert ? <AlertTriangle className="w-4 h-4" /> : <CheckCircle className="w-4 h-4" />}
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1">
                      <div className="flex items-center gap-2">
                        <h4 className="text-sm font-semibold text-white tracking-tight">{n.title}</h4>
                        {!n.isRead && (
                          <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
                        )}
                      </div>
                      <span className="text-[11px] text-slate-500 font-mono">
                        {new Date(n.createdAt).toLocaleString()}
                      </span>
                    </div>

                    <p className="mt-1 text-xs text-slate-300 leading-relaxed font-sans">{n.message}</p>

                    <div className="mt-3 flex items-center gap-3 text-[11px] font-mono">
                      <span className="flex items-center gap-1 text-slate-400">
                        {n.channel === 'EMAIL' ? <Mail className="w-3.5 h-3.5 text-cyan-400" /> : <Bell className="w-3.5 h-3.5 text-slate-400" />}
                        <span>{n.channel}</span>
                      </span>

                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          n.status === 'SENT'
                            ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                            : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                        }`}
                      >
                        {n.status}
                      </span>

                      {n.errorDetails && (
                        <span className="text-slate-500 truncate" title={n.errorDetails}>
                          ({n.errorDetails})
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
