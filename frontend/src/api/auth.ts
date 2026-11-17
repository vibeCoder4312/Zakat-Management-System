import { apiClient } from './client';
import type { AppUser, AuthResponse, ChangePasswordRequest, LoginRequest } from '../types';

export async function login(credentials: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', credentials);
  return data;
}

export async function changePassword(body: ChangePasswordRequest): Promise<{ message: string }> {
  const { data } = await apiClient.post<{ message: string }>('/auth/change-password', body);
  return data;
}

export async function getCurrentUser(): Promise<AppUser> {
  const { data } = await apiClient.get<AppUser>('/auth/me');
  return data;
}

export async function updateProfilePicture(profilePicture: string | null): Promise<AppUser> {
  const { data } = await apiClient.put<AppUser>('/auth/profile-picture', { profilePicture });
  return data;
}
