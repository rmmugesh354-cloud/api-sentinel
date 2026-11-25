import React, { createContext, useContext, useEffect, useState } from 'react';
import { authService } from '../services/authService';
import { User } from '../types';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (fullName: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('sentinel_token'));
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const initializeAuth = async () => {
      const storedToken = localStorage.getItem('sentinel_token');
      if (storedToken) {
        try {
          const profile = await authService.getCurrentUser();
          setUser({
            id: profile.id,
            email: profile.email,
            fullName: profile.fullName,
            role: profile.role,
          });
          setToken(storedToken);
        } catch {
          localStorage.removeItem('sentinel_token');
          localStorage.removeItem('sentinel_user');
          setUser(null);
          setToken(null);
        }
      }
      setIsLoading(false);
    };

    initializeAuth();
  }, []);

  const login = async (email: string, password: string) => {
    const response = await authService.login({ email, password });
    localStorage.setItem('sentinel_token', response.token);
    localStorage.setItem('sentinel_user', JSON.stringify({
      id: response.id,
      email: response.email,
      fullName: response.fullName,
      role: response.role,
    }));
    setToken(response.token);
    setUser({
      id: response.id,
      email: response.email,
      fullName: response.fullName,
      role: response.role,
    });
  };

  const register = async (fullName: string, email: string, password: string) => {
    const response = await authService.register({ fullName, email, password });
    localStorage.setItem('sentinel_token', response.token);
    localStorage.setItem('sentinel_user', JSON.stringify({
      id: response.id,
      email: response.email,
      fullName: response.fullName,
      role: response.role,
    }));
    setToken(response.token);
    setUser({
      id: response.id,
      email: response.email,
      fullName: response.fullName,
      role: response.role,
    });
  };

  const logout = async () => {
    try {
      await authService.logout();
    } finally {
      setUser(null);
      setToken(null);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
