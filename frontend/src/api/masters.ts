import { apiClient, cleanParams } from './client';
import type { PagedResponse } from '../types';

/**
 * The six master screens share one REST shape:
 *   GET /{resource}?page&size&q   POST /{resource}   PUT /{resource}/{id}   DELETE /{resource}/{id}
 */
export type MasterResource =
  | 'classifications'
  | 'beneficiary-categories'
  | 'source-accounts'
  | 'agents'
  | 'employees'
  | 'schools';

export async function listMaster<T>(resource: MasterResource, q?: string): Promise<T[]> {
  const { data } = await apiClient.get<PagedResponse<T>>(`/${resource}`, { params: cleanParams({ page: 0, size: 500, q }) });
  return data.content;
}

export async function createMaster<T>(resource: MasterResource, body: Record<string, unknown>): Promise<T> {
  const { data } = await apiClient.post<T>(`/${resource}`, body);
  return data;
}

export async function updateMaster<T>(resource: MasterResource, id: number, body: Record<string, unknown>): Promise<T> {
  const { data } = await apiClient.put<T>(`/${resource}/${id}`, body);
  return data;
}

export async function deleteMaster(resource: MasterResource, id: number): Promise<void> {
  await apiClient.delete(`/${resource}/${id}`);
}
