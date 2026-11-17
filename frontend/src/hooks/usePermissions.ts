import { useAuth } from './useAuth';

/** Mirrors the backend SecurityConfig role matrix so the UI only offers actions the API will accept. */
export function usePermissions() {
  const { user } = useAuth();
  const role = user?.role;
  const isAdmin = role === 'ADMIN';
  const isAccountant = role === 'ACCOUNTANT';
  return {
    canEditMasters: isAdmin,
    canEditAssessments: isAdmin,
    canEditPlanning: isAdmin,
    canEditBeneficiaries: isAdmin || isAccountant,
    canEditAllocations: isAdmin || isAccountant,
    canExport: isAdmin || isAccountant,
    canManageUsers: isAdmin,
  };
}
