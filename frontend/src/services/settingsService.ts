import api from './api';
import { ApiResponse, UserSettings } from '../types';

export const settingsService = {
  async getSettings(): Promise<UserSettings> {
    const res = await api.get<ApiResponse<UserSettings>>('/settings');
    return res.data.data;
  },

  async updateSettings(data: {
    emailNotificationsEnabled: boolean;
    notificationEmail: string | null;
    defaultCheckIntervalSeconds: number;
    defaultConsecutiveFailures: number;
  }): Promise<UserSettings> {
    const res = await api.put<ApiResponse<UserSettings>>('/settings', data);
    return res.data.data;
  },
};
