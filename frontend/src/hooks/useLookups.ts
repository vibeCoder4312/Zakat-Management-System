import { useQuery } from '@tanstack/react-query';
import {
  getAgentsLookup,
  getCategoriesLookup,
  getClassificationsLookup,
  getEmployeesLookup,
  getSchoolsLookup,
  getSourceAccountsLookup,
  getYears,
} from '../api/lookups';

const opts = { staleTime: 10 * 60 * 1000 };

/** Reference data used by filters and forms; cached for 10 minutes and refreshed after master edits. */
export function useLookups() {
  const years = useQuery({ queryKey: ['lookups', 'years'], queryFn: getYears, ...opts });
  const classifications = useQuery({ queryKey: ['lookups', 'classifications'], queryFn: getClassificationsLookup, ...opts });
  const categories = useQuery({ queryKey: ['lookups', 'categories'], queryFn: getCategoriesLookup, ...opts });
  const sourceAccounts = useQuery({ queryKey: ['lookups', 'source-accounts'], queryFn: getSourceAccountsLookup, ...opts });
  const schools = useQuery({ queryKey: ['lookups', 'schools'], queryFn: getSchoolsLookup, ...opts });
  const agents = useQuery({ queryKey: ['lookups', 'agents'], queryFn: getAgentsLookup, ...opts });
  const employees = useQuery({ queryKey: ['lookups', 'employees'], queryFn: getEmployeesLookup, ...opts });
  return {
    years: years.data ?? [],
    classifications: classifications.data ?? [],
    categories: categories.data ?? [],
    sourceAccounts: sourceAccounts.data ?? [],
    schools: schools.data ?? [],
    agents: agents.data ?? [],
    employees: employees.data ?? [],
  };
}
