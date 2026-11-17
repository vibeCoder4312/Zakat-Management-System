import { apiClient } from './client';
import type { Assessment, AssessmentRequest, PagedResponse } from '../types';

export async function getAssessments(): Promise<Assessment[]> {
  const { data } = await apiClient.get<PagedResponse<Assessment>>('/assessments', {
    params: { page: 0, size: 100, sort: 'zakatYear,desc' },
  });
  return data.content;
}

export async function createAssessment(body: AssessmentRequest): Promise<Assessment> {
  const { data } = await apiClient.post<Assessment>('/assessments', body);
  return data;
}

export async function updateAssessment(id: number, body: AssessmentRequest): Promise<Assessment> {
  const { data } = await apiClient.put<Assessment>(`/assessments/${id}`, body);
  return data;
}

export async function deleteAssessment(id: number): Promise<void> {
  await apiClient.delete(`/assessments/${id}`);
}
