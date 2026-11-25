import api from './api';
import { ApiResponse, Incident, IncidentStatus, PageResponse } from '../types';

export const incidentService = {
  async getIncidents(status?: IncidentStatus, page = 0, size = 20): Promise<PageResponse<Incident>> {
    let url = `/incidents?page=${page}&size=${size}`;
    if (status) {
      url += `&status=${status}`;
    }
    const res = await api.get<ApiResponse<PageResponse<Incident>>>(url);
    return res.data.data;
  },

  async getIncident(id: number): Promise<Incident> {
    const res = await api.get<ApiResponse<Incident>>(`/incidents/${id}`);
    return res.data.data;
  },

  async acknowledgeIncident(id: number): Promise<Incident> {
    const res = await api.post<ApiResponse<Incident>>(`/incidents/${id}/acknowledge`);
    return res.data.data;
  },
};
