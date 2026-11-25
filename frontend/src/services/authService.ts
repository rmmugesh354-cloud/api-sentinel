import api from './api';
import { ApiResponse, AuthResponse } from '../types';

export const authService = {
  async register(data: { fullName: string; email: string; password: string }): Promise<AuthResponse> {
    const res = await api.post<ApiResponse<AuthResponse>>('/auth/register', data);
    return res.data.data;
  },

  async login(data: { email: string; password: string }): Promise<AuthResponse> {
    const res = await api.post<ApiResponse<AuthResponse>>('/auth/login', data);
    return res.data.data;
  },

  async getCurrentUser(): Promise<AuthResponse> {
    const res = await api.get<ApiResponse<AuthResponse>>('/auth/me');
    return res.data.data;
  },

  async logout(): Promise<void> {
    try {
      await api.post('/auth/logout');
    } finally {
      localStorage.removeItem('sentinel_token');
      localStorage.removeItem('sentinel_user');
    }
  },
};
