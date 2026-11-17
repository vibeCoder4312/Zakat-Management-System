import { apiClient, cleanParams } from './client';
import type { AppUser, CreateUserRequest, PagedResponse, UpdateUserRequest } from '../types';

export interface UserListParams {
  page?: number;
  size?: number;
  q?: string;
}

export async function getUsers(params?: UserListParams): Promise<PagedResponse<AppUser>> {
  const { data } = await apiClient.get<PagedResponse<AppUser>>('/users', {
    params: cleanParams({ page: 0, size: 50, ...params }),
  });
  return data;
}

export async function getUser(id: number): Promise<AppUser> {
  const { data } = await apiClient.get<AppUser>(`/users/${id}`);
  return data;
}

export async function createUser(body: CreateUserRequest): Promise<AppUser> {
  const { data } = await apiClient.post<AppUser>('/users', body);
  return data;
}

export async function updateUser(id: number, body: UpdateUserRequest): Promise<AppUser> {
  const { data } = await apiClient.put<AppUser>(`/users/${id}`, body);
  return data;
}

export async function deleteUser(id: number): Promise<void> {
  await apiClient.delete(`/users/${id}`);
}
