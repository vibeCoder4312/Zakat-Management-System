import React, { createContext, useCallback, useEffect, useState } from 'react';
import { JWT_KEY } from '../lib/constants';
import { AuthResponse, LoginRequest, UserRole } from '../types';
import { login as apiLogin } from '../api/auth';

interface UserInfo {
  username: string;
  role: UserRole;
}

interface AuthContextType {
  user: UserInfo | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
  hasRole: (roles: UserRole[]) => boolean;
}

export const AuthContext = createContext<AuthContextType>({
  user: null,
  token: null,
  isAuthenticated: false,
  login: async () => {},
  logout: () => {},
  hasRole: () => false,
});

function decodeJwtPayload(token: string): { sub?: string; role?: UserRole; exp?: number } | null {
  try {
    const base64Url = token.split('.')[1];
    if (!base64Url) return null;
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(JWT_KEY));
  const [user, setUser] = useState<UserInfo | null>(() => {
    const savedToken = localStorage.getItem(JWT_KEY);
    if (!savedToken) return null;
    const payload = decodeJwtPayload(savedToken);
    if (!payload || !payload.exp || payload.exp * 1000 < Date.now()) {
      localStorage.removeItem(JWT_KEY);
      return null;
    }
    return {
      username: payload.sub || '',
      role: (payload.role as UserRole) || 'VIEWER',
    };
  });

  const logout = useCallback(() => {
    localStorage.removeItem(JWT_KEY);
    setToken(null);
    setUser(null);
  }, []);

  const login = useCallback(async (credentials: LoginRequest) => {
    const data: AuthResponse = await apiLogin(credentials);
    localStorage.setItem(JWT_KEY, data.token);
    setToken(data.token);
    setUser({
      username: data.username,
      role: data.role,
    });
  }, []);

  const hasRole = useCallback(
    (roles: UserRole[]) => {
      if (!user) return false;
      return roles.includes(user.role);
    },
    [user]
  );

  useEffect(() => {
    if (!token) return;
    const payload = decodeJwtPayload(token);
    if (!payload || !payload.exp || payload.exp * 1000 < Date.now()) {
      logout();
    }
  }, [token, logout]);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!user && !!token,
        login,
        logout,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
