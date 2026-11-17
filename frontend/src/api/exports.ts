import { downloadFile } from './client';
import type { AllocationFilters, BeneficiaryParams } from '../types';

export const exportAllocations = (filters: AllocationFilters) =>
  downloadFile('/export/allocations.xlsx', 'allocations.xlsx', filters);

export const exportBeneficiaries = (params: BeneficiaryParams) =>
  downloadFile('/export/beneficiaries.xlsx', 'beneficiaries.xlsx', { ...params, page: undefined, size: undefined });

export const exportAssessments = () => downloadFile('/export/assessments.xlsx', 'zakat-assessments.xlsx');

export const downloadReceipt = (allocationId: number) =>
  downloadFile(`/allocations/${allocationId}/receipt`, `receipt-${allocationId}`);
