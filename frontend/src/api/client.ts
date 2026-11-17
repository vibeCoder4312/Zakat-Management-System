import axios, { AxiosError } from 'axios';
import { API_BASE_URL, JWT_KEY } from '../lib/constants';
import type { ApiError } from '../types';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  // Spring expects repeated keys for lists: ?years=2025&years=2026 (not years[]=...)
  paramsSerializer: { indexes: null },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(JWT_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    // Expired or missing token: drop it and send the user to the login screen.
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      localStorage.removeItem(JWT_KEY);
      if (window.location.pathname !== '/login') window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

/** Removes undefined / empty-string / empty-array params so they are not sent. */
export function cleanParams<T extends object>(params: T): Partial<T> {
  const out: Record<string, unknown> = {};
  Object.entries(params).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return;
    if (Array.isArray(v) && v.length === 0) return;
    out[k] = v;
  });
  return out as Partial<T>;
}

/** Human-readable message from an API error (business-rule text, field errors, or a fallback). */
export function errorMessage(err: unknown, fallback = 'Something went wrong. Please try again.'): string {
  const ax = err as AxiosError<ApiError>;
  const data = ax?.response?.data;
  if (data && typeof data === 'object') {
    if (data.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
      return Object.values(data.fieldErrors).join(' ');
    }
    if (data.message) return data.message;
  }
  if (ax?.response?.status === 403) return 'Your role does not have permission to perform this action.';
  if (ax?.message === 'Network Error') return 'Cannot reach the server. Check that the backend is running.';
  return fallback;
}

/** Downloads a binary endpoint (Excel exports, receipts) as a file. */
export async function downloadFile(url: string, filename: string, params?: object): Promise<void> {
  const response = await apiClient.get(url, { params: params ? cleanParams(params) : undefined, responseType: 'blob' });
  const disposition = String(response.headers['content-disposition'] ?? '');
  const match = /filename="?([^";]+)"?/.exec(disposition);
  const href = window.URL.createObjectURL(response.data as Blob);
  const link = document.createElement('a');
  link.href = href;
  link.download = match?.[1] ?? filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(href);
}
