import { apiClient, cleanParams } from './client';
import type { FullDashboard, PivotTable } from '../types';

/** An empty year list means "all years" on the backend. */
export async function getDashboard(years: number[]): Promise<FullDashboard> {
  const { data } = await apiClient.get<FullDashboard>('/dashboard', { params: cleanParams({ years }) });
  return data;
}

export async function getClassificationPivot(): Promise<PivotTable> {
  const { data } = await apiClient.get<PivotTable>('/dashboard/pivot/classification-by-year');
  return data;
}

export async function getCategoryPivot(): Promise<PivotTable> {
  const { data } = await apiClient.get<PivotTable>('/dashboard/pivot/category-by-year');
  return data;
}
