import api from './api';
import { ApiResponse, AvailabilityMetrics, DashboardSummary, LatencyMetrics } from '../types';

export const dashboardService = {
  async getSummary(): Promise<DashboardSummary> {
    const res = await api.get<ApiResponse<DashboardSummary>>('/dashboard/summary');
    return res.data.data;
  },

  async getLatencyMetrics(endpointId?: number, window = '24h'): Promise<LatencyMetrics> {
    let url = `/dashboard/latency?window=${window}`;
    if (endpointId) {
      url += `&endpointId=${endpointId}`;
    }
    const res = await api.get<ApiResponse<LatencyMetrics>>(url);
    return res.data.data;
  },

  async getAvailabilityMetrics(endpointId?: number, window = '24h'): Promise<AvailabilityMetrics> {
    let url = `/dashboard/availability?window=${window}`;
    if (endpointId) {
      url += `&endpointId=${endpointId}`;
    }
    const res = await api.get<ApiResponse<AvailabilityMetrics>>(url);
    return res.data.data;
  },
};
