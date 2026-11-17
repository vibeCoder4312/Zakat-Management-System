import { MasterConfig, MasterPage } from './MasterPage';
import { StatusBadge } from '../../components/shared/StatusBadge';

const fund = (v: unknown) => <StatusBadge status={String(v)} size="sm" />;

const classifications: MasterConfig = {
  resource: 'classifications',
  title: 'Classifications',
  subtitle: 'Purposes money is given for. Fund type decides whether it counts against the Zakat obligation. Sort order controls the sequence in lists and dropdowns.',
  singular: 'Classification',
  idKey: 'classificationId',
  fields: [
    { key: 'name', label: 'Name', required: true },
    { key: 'code', label: 'Code' },
    { key: 'fundType', label: 'Fund Type', type: 'select', required: true, defaultValue: 'ZAKAT', render: fund,
      options: [{ value: 'ZAKAT', label: 'Zakat' }, { value: 'SADAQAH', label: 'Sadaqah' }] },
    { key: 'displayOrder', label: 'Sort Order', type: 'number', defaultValue: 99, hint: 'Lower numbers appear first in classification lists and dropdowns.' },
  ],
};

const categories: MasterConfig = {
  resource: 'beneficiary-categories',
  title: 'Beneficiary Categories',
  subtitle: 'Groups of beneficiaries (Poor Family, Institute, Madrasa...). Used for planning and reports.',
  singular: 'Category',
  idKey: 'categoryId',
  fields: [{ key: 'name', label: 'Name', required: true }],
  extraColumns: [{ key: 'beneficiaryCount', header: 'Beneficiaries', align: 'right' }],
};

const sourceAccounts: MasterConfig = {
  resource: 'source-accounts',
  title: 'Source Accounts',
  subtitle: 'Accounts payments are made from.',
  singular: 'Source Account',
  idKey: 'sourceAccountId',
  fields: [
    { key: 'name', label: 'Name', required: true },
    { key: 'region', label: 'Region', type: 'select', required: true, defaultValue: 'INDIA',
      options: [{ value: 'INDIA', label: 'India' }, { value: 'DUBAI', label: 'Dubai' }] },
  ],
};

const schools: MasterConfig = {
  resource: 'schools',
  title: 'Institutes',
  subtitle: 'Schools, colleges and madrasas that receive fee payments.',
  singular: 'Institute',
  idKey: 'schoolId',
  fields: [
    { key: 'name', label: 'Name', required: true },
    { key: 'location', label: 'Location' },
  ],
  extraColumns: [{ key: 'beneficiaryCount', header: 'Allocations', align: 'right' }],
};

const agents: MasterConfig = {
  resource: 'agents',
  title: 'Agents',
  subtitle: 'People who hand over payments on behalf of the organisation.',
  singular: 'Agent',
  idKey: 'agentId',
  fields: [
    { key: 'name', label: 'Name', required: true },
    { key: 'agentType', label: 'Agent Type', type: 'select', required: true, defaultValue: 'PARTNER',
      options: [{ value: 'PARTNER', label: 'Partner' }, { value: 'EMPLOYEE', label: 'Employee' }] },
    { key: 'contactNumber', label: 'Contact Number' },
  ],
};

const employees: MasterConfig = {
  resource: 'employees',
  title: 'Employees',
  subtitle: 'Staff who refer or verify beneficiaries.',
  singular: 'Employee',
  idKey: 'employeeId',
  fields: [
    { key: 'name', label: 'Name', required: true },
    { key: 'contactNumber', label: 'Contact Number' },
    { key: 'remarks', label: 'Remarks' },
  ],
};

export const Classifications = () => <MasterPage config={classifications} />;
export const Categories = () => <MasterPage config={categories} />;
export const SourceAccounts = () => <MasterPage config={sourceAccounts} />;
export const Schools = () => <MasterPage config={schools} />;
export const Agents = () => <MasterPage config={agents} />;
export const Employees = () => <MasterPage config={employees} />;
