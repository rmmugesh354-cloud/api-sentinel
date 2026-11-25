import api from './api';
import { ApiResponse, EndpointRequest, HealthCheckResult, MonitoredEndpoint, PageResponse } from '../types';

export const endpointService = {
  async getEndpoints(page = 0, size = 20): Promise<PageResponse<MonitoredEndpoint>> {
    const res = await api.get<ApiResponse<PageResponse<MonitoredEndpoint>>>(`/endpoints?page=${page}&size=${size}`);
    return res.data.data;
  },

  async getAllEndpoints(): Promise<MonitoredEndpoint[]> {
    const res = await api.get<ApiResponse<MonitoredEndpoint[]>>('/endpoints/all');
    return res.data.data;
  },

  async getEndpoint(id: number): Promise<MonitoredEndpoint> {
    const res = await api.get<ApiResponse<MonitoredEndpoint>>(`/endpoints/${id}`);
    return res.data.data;
  },

  async createEndpoint(data: EndpointRequest): Promise<MonitoredEndpoint> {
    const res = await api.post<ApiResponse<MonitoredEndpoint>>('/endpoints', data);
    return res.data.data;
  },

  async updateEndpoint(id: number, data: EndpointRequest): Promise<MonitoredEndpoint> {
    const res = await api.put<ApiResponse<MonitoredEndpoint>>(`/endpoints/${id}`, data);
    return res.data.data;
  },

  async deleteEndpoint(id: number): Promise<void> {
    await api.delete<ApiResponse<null>>(`/endpoints/${id}`);
  },

  async pauseEndpoint(id: number): Promise<MonitoredEndpoint> {
    const res = await api.post<ApiResponse<MonitoredEndpoint>>(`/endpoints/${id}/pause`);
    return res.data.data;
  },

  async resumeEndpoint(id: number): Promise<MonitoredEndpoint> {
    const res = await api.post<ApiResponse<MonitoredEndpoint>>(`/endpoints/${id}/resume`);
    return res.data.data;
  },

  async triggerManualCheck(id: number): Promise<HealthCheckResult> {
    const res = await api.post<ApiResponse<HealthCheckResult>>(`/endpoints/${id}/check`);
    return res.data.data;
  },

  async getRecentChecks(id: number, page = 0, size = 20): Promise<PageResponse<HealthCheckResult>> {
    const res = await api.get<ApiResponse<PageResponse<HealthCheckResult>>>(`/endpoints/${id}/checks?page=${page}&size=${size}`);
    return res.data.data;
  },
};
