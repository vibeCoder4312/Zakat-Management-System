import { apiClient, cleanParams } from './client';
import type {
  Allocation,
  AllocationFilters,
  AllocationParams,
  AllocationRequest,
  AllocationSummary,
  PagedResponse,
} from '../types';

export async function getAllocations(params: AllocationParams): Promise<PagedResponse<Allocation>> {
  const { data } = await apiClient.get<PagedResponse<Allocation>>('/allocations', { params: cleanParams(params) });
  return data;
}

export async function getAllocationSummary(filters: AllocationFilters): Promise<AllocationSummary> {
  const { data } = await apiClient.get<AllocationSummary>('/allocations/summary', { params: cleanParams(filters) });
  return data;
}

export async function createAllocation(body: AllocationRequest): Promise<Allocation> {
  const { data } = await apiClient.post<Allocation>('/allocations', body);
  return data;
}

export async function updateAllocation(id: number, body: AllocationRequest): Promise<Allocation> {
  const { data } = await apiClient.put<Allocation>(`/allocations/${id}`, body);
  return data;
}

export async function deleteAllocation(id: number): Promise<void> {
  await apiClient.delete(`/allocations/${id}`);
}

export async function uploadReceipt(id: number, file: File): Promise<void> {
  const form = new FormData();
  form.append('file', file);
  await apiClient.post(`/allocations/${id}/receipt`, form, { headers: { 'Content-Type': 'multipart/form-data' } });
}
