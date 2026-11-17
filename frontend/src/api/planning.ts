import { apiClient, cleanParams } from './client';
import type { Allocation, Planning, PlanningRequest } from '../types';

export async function getPlanning(years: number[]): Promise<Planning[]> {
  const { data } = await apiClient.get<Planning[]>('/planning', { params: cleanParams({ years }) });
  return data;
}

export async function getPlanningAllocations(planningId: number): Promise<Allocation[]> {
  const { data } = await apiClient.get<Allocation[]>(`/planning/${planningId}/allocations`);
  return data;
}

export async function createPlanning(body: PlanningRequest): Promise<Planning> {
  const { data } = await apiClient.post<Planning>('/planning', body);
  return data;
}

export async function updatePlanning(id: number, body: PlanningRequest): Promise<Planning> {
  const { data } = await apiClient.put<Planning>(`/planning/${id}`, body);
  return data;
}

export async function deletePlanning(id: number): Promise<void> {
  await apiClient.delete(`/planning/${id}`);
}
