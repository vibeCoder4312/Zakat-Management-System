import { apiClient, cleanParams } from './client';
import type { Beneficiary, BeneficiaryParams, BeneficiaryRequest, PagedResponse, VerificationStatus } from '../types';

export async function getBeneficiaries(params: BeneficiaryParams): Promise<PagedResponse<Beneficiary>> {
  const { data } = await apiClient.get<PagedResponse<Beneficiary>>('/beneficiaries', { params: cleanParams(params) });
  return data;
}

/** Verified + active beneficiaries only: the ones an allocation may be made to. */
export async function searchVerifiedBeneficiaries(q: string, limit = 20): Promise<Beneficiary[]> {
  const { data } = await apiClient.get<Beneficiary[]>('/beneficiaries/typeahead', { params: cleanParams({ q, limit }) });
  return data;
}

export async function createBeneficiary(body: BeneficiaryRequest): Promise<Beneficiary> {
  const { data } = await apiClient.post<Beneficiary>('/beneficiaries', body);
  return data;
}

export async function updateBeneficiary(id: number, body: BeneficiaryRequest): Promise<Beneficiary> {
  const { data } = await apiClient.put<Beneficiary>(`/beneficiaries/${id}`, body);
  return data;
}

export async function setVerification(id: number, verificationStatus: VerificationStatus, verificationNote?: string): Promise<Beneficiary> {
  const { data } = await apiClient.patch<Beneficiary>(`/beneficiaries/${id}/verification`, { verificationStatus, verificationNote });
  return data;
}

export async function deleteBeneficiary(id: number): Promise<void> {
  await apiClient.delete(`/beneficiaries/${id}`);
}
