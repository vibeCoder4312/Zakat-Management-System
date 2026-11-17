import { apiClient } from './client';
import type { Agent, BeneficiaryCategory, Classification, Employee, School, SourceAccount } from '../types';

const get = async <T>(path: string): Promise<T> => (await apiClient.get<T>(path)).data;

export const getYears = () => get<number[]>('/lookups/years');
export const getClassificationsLookup = () => get<Classification[]>('/lookups/classifications');
export const getCategoriesLookup = () => get<BeneficiaryCategory[]>('/lookups/categories');
export const getSourceAccountsLookup = () => get<SourceAccount[]>('/lookups/source-accounts');
export const getSchoolsLookup = () => get<School[]>('/lookups/schools');
export const getAgentsLookup = () => get<Agent[]>('/lookups/agents');
export const getEmployeesLookup = () => get<Employee[]>('/lookups/employees');
