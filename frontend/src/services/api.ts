import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('sentinel_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401 && !window.location.pathname.startsWith('/login') && !window.location.pathname.startsWith('/register')) {
      localStorage.removeItem('sentinel_token');
      localStorage.removeItem('sentinel_user');
      window.location.href = '/login?expired=true';
    }
    return Promise.reject(error);
  }
);

export default api;
