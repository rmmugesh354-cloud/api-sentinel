import api from './api';
import { ApiResponse, Notification, PageResponse } from '../types';

export const notificationService = {
  async getNotifications(page = 0, size = 20): Promise<PageResponse<Notification>> {
    const res = await api.get<ApiResponse<PageResponse<Notification>>>(`/notifications?page=${page}&size=${size}`);
    return res.data.data;
  },

  async getUnreadCount(): Promise<number> {
    const res = await api.get<ApiResponse<number>>('/notifications/unread-count');
    return res.data.data;
  },

  async markAsRead(id: number): Promise<void> {
    await api.post(`/notifications/${id}/read`);
  },

  async markAllAsRead(): Promise<number> {
    const res = await api.post<ApiResponse<number>>('/notifications/read-all');
    return res.data.data;
  },
};
