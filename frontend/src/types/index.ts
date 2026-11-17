// Types mirror the Spring Boot DTOs in backend/src/main/java/com/iqra/zakat/dto exactly.
// Field names must stay in sync with the JSON the API returns.

export type UserRole = 'ADMIN' | 'ACCOUNTANT' | 'VIEWER';
export type YesNo = 'Y' | 'N';
export type FundType = 'ZAKAT' | 'SADAQAH';
export type Currency = 'INR' | 'AED';

// ---------------------------------------------------------------- Auth
export interface LoginRequest {
  username: string;
  password: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface AppUser extends Audit {
  userId: number;
  username: string;
  fullName: string;
  email?: string | null;
  phone?: string | null;
  profilePicture?: string | null;
  role: UserRole;
  isActive: YesNo;
}

export interface CreateUserRequest {
  username: string;
  fullName: string;
  email?: string | null;
  phone?: string | null;
  role: UserRole;
  password: string;
  isActive: YesNo;
}

export interface UpdateUserRequest {
  fullName: string;
  email?: string | null;
  phone?: string | null;
  role: UserRole;
  isActive: YesNo;
  newPassword?: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  username: string;
  fullName: string;
  role: UserRole;
}

// ---------------------------------------------------------------- Common
export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiError {
  status: number;
  code: string;
  message: string;
  fieldErrors?: Record<string, string> | null;
}

interface Audit {
  createdBy?: string | null;
  createdOn?: string | null;
  updatedBy?: string | null;
  updatedOn?: string | null;
}

// ---------------------------------------------------------------- Assessment
export type AssessmentStatus = 'OPEN' | 'CLOSED';

export interface Assessment extends Audit {
  assessmentId: number;
  zakatYear: number;
  zakatAmount: number;
  carryForward: number;
  netPayable: number;
  status: AssessmentStatus;
  remarks?: string | null;
  totalAllocatedAmount: number;
  remainingBalance: number;
}

export interface AssessmentRequest {
  zakatYear: number;
  zakatAmount: number;
  carryForward: number;
  status?: AssessmentStatus;
  remarks?: string;
}

// ---------------------------------------------------------------- Masters
export interface Classification extends Audit {
  classificationId: number;
  code?: string | null;
  name: string;
  fundType: FundType;
  displayOrder?: number | null;
  isActive: YesNo;
}

export interface BeneficiaryCategory extends Audit {
  categoryId: number;
  name: string;
  isActive: YesNo;
  beneficiaryCount: number;
}

export interface SourceAccount extends Audit {
  sourceAccountId: number;
  name: string;
  region: 'INDIA' | 'DUBAI';
  isActive: YesNo;
}

export interface Agent extends Audit {
  agentId: number;
  name: string;
  agentType: 'EMPLOYEE' | 'PARTNER';
  contactNumber?: string | null;
  isActive: YesNo;
}

export interface Employee extends Audit {
  employeeId: number;
  name: string;
  contactNumber?: string | null;
  remarks?: string | null;
  isActive: YesNo;
}

export interface School extends Audit {
  schoolId: number;
  name: string;
  location?: string | null;
  isActive: YesNo;
  /** Number of allocation rows that reference this institute. */
  beneficiaryCount: number;
}

// ---------------------------------------------------------------- Beneficiary
export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'CANCELLED';

export interface Beneficiary extends Audit {
  beneficiaryId: number;
  beneficiaryCode: string;
  name: string;
  beneficiaryCategory?: string | null;
  zakatYear?: number | null;
  contactNumber?: string | null;
  address?: string | null;
  description?: string | null;
  referredBy?: string | null;
  verificationStatus: VerificationStatus;
  verifiedBy?: string | null;
  verifiedOn?: string | null;
  verificationNote?: string | null;
  isActive: YesNo;
}

export interface BeneficiaryRequest {
  beneficiaryCode?: string;
  name: string;
  beneficiaryCategory?: string;
  zakatYear?: number | null;
  contactNumber?: string;
  address?: string;
  description?: string;
  referredBy?: string;
  verificationStatus?: VerificationStatus;
  verificationNote?: string;
  isActive?: YesNo;
}

export interface BeneficiaryParams {
  page?: number;
  size?: number;
  category?: string;
  verificationStatus?: string;
  isActive?: string;
  q?: string;
}

// ---------------------------------------------------------------- Allocation
export type AllocationStatus = 'PLANNED' | 'PAID' | 'CANCELLED';
export type ApprovalStatus = 'PENDING' | 'APPROVED';
export type PaymentMethod = 'CASH' | 'ONLINE' | 'BANK';

export interface Allocation extends Audit {
  allocationId: number;
  assessmentId: number;
  zakatYear: number;
  classificationId: number;
  classification: string;
  fundType?: FundType | null;
  beneficiaryId: number;
  beneficiary: string;
  beneficiaryCategory?: string | null;
  plannedAmount: number;
  schoolId?: number | null;
  school?: string | null;
  currentStd?: string | null;
  prevStdResult?: string | null;
  paidAmount: number;
  committedAmount?: number | null;
  outstandingAmount?: number | null;
  paidOn?: string | null;
  paymentMethod?: PaymentMethod | null;
  paidSourceAccountId?: number | null;
  paidSourceAccount?: string | null;
  paidViaAgentId?: number | null;
  paidViaAgent?: string | null;
  paidViaEmployee?: string | null;
  referenceNo?: string | null;
  currencyCode: Currency;
  foreignAmount?: number | null;
  fxRate?: number | null;
  receiptReceived: YesNo;
  status: AllocationStatus;
  approvalStatus: ApprovalStatus;
  category?: string | null;
  remarks?: string | null;
}

export interface AllocationRequest {
  zakatYear: number;
  classificationId: number;
  beneficiaryId: number;
  plannedAmount: number;
  schoolId?: number | null;
  currentStd?: string;
  prevStdResult?: string;
  paidAmount?: number;
  paidOn?: string | null;
  paymentMethod?: string | null;
  paidSourceAccountId?: number | null;
  currencyCode?: Currency;
  foreignAmount?: number | null;
  fxRate?: number | null;
  receiptReceived?: YesNo;
  status?: AllocationStatus;
  approvalStatus?: ApprovalStatus;
  category?: string;
  paidViaAgent?: string;
  paidViaEmployee?: string;
  referenceNo?: string;
  remarks?: string;
}

/** Filters accepted by GET /allocations, /allocations/summary and /export/allocations.xlsx */
export interface AllocationFilters {
  years?: number[];
  classificationId?: number;
  category?: string;
  status?: string;
  approvalStatus?: string;
  beneficiaryId?: number;
  sourceAccountId?: number;
  schoolId?: number;
  paymentMethod?: string;
  q?: string;
}

export interface AllocationParams extends AllocationFilters {
  page?: number;
  size?: number;
  sort?: string;
}

export interface AllocationSummary {
  totalAllocations: number;
  totalAllocated: number;
  totalPaid: number;
  remainingAmount: number;
}

// ---------------------------------------------------------------- Planning
export interface Planning extends Audit {
  planningId: number;
  zakatYear: number;
  planType: FundType;
  classificationId?: number | null;
  classification?: string | null;
  category?: string | null;
  plannedAmount: number;
  paidAmount: number;
  remainingAmount: number;
  outstandingAmount: number;
  allocationCount: number;
  beneficiaryCount: number;
  sourceAccount?: string | null;
  lastPaymentDate?: string | null;
  paymentStatus: 'PLANNED' | 'PARTIAL' | 'PAID' | 'CANCELLED';
  cyMinus1: number;
  cyMinus2: number;
  cyMinus3: number;
  cyMinus4: number;
  cyMinus5: number;
  remarks?: string | null;
}

export interface PlanningRequest {
  zakatYear: number;
  classificationId?: number | null;
  category?: string;
  plannedAmount: number;
  remarks?: string;
}

// ---------------------------------------------------------------- Dashboard
export interface HeroData {
  yearTitle: string;
  statusPill: string;
  zakatPaid: number;
  netPayable: number;
  zakatAmount: number;
  carryForward: number;
  sadaqahPaid: number;
  obligationPercentage: number;
  plannedOutstanding: number;
  unallocated: number;
}

export interface KpiData {
  netPayable: number;
  paidCount: number;
  plannedNotPaidAmount: number;
  plannedNotPaidCount: number;
  remainingOrOverallocated: number;
  overallocated: boolean;
  distributedPercentage: number;
  beneficiariesHelped: number;
  verifiedActiveCount: number;
  awaitingVerificationCount: number;
  missingReceiptsCount: number;
}

export interface ChartItem {
  name: string;
  paid: number;
  pending: number;
  total: number;
}

export interface MonthlyItem {
  monthYear: string;
  amount: number;
  runningTotal: number;
}

export interface YearlyItem {
  year: number;
  netPayable: number;
  zakatPaid: number;
  sadaqahPaid: number;
}

export interface DonutItem {
  label: string;
  amount: number;
  percentage: number;
}

export interface AttentionItem {
  badgeType: 'PENDING_PAYMENT' | 'RECEIPT_MISSING' | 'VERIFY_BENEFICIARY' | string;
  badgeLabel: string;
  beneficiary: string;
  detail?: string | null;
  amount?: number | null;
  entityId?: number | null;
}

export interface RecentPaymentItem {
  paidOn?: string | null;
  beneficiary: string;
  classification: string;
  sourceAccount?: string | null;
  paymentMethod?: string | null;
  paidAmount: number;
}

export interface FullDashboard {
  hero: HeroData;
  kpis: KpiData;
  distributionByClassification: ChartItem[];
  distributionByCategory: ChartItem[];
  monthlyDistribution: MonthlyItem[];
  yearlyDistribution: YearlyItem[];
  beneficiaryCategoryDonut: DonutItem[];
  cashVsOnlineDonut: DonutItem[];
  sourceAccountDistribution: DonutItem[];
  needsAttention: AttentionItem[];
  recentPayments: RecentPaymentItem[];
  verificationCounts: { verified: number; pending: number; cancelled: number };
}

export interface PivotRow {
  rowLabel: string;
  values: Record<string, number>;
  total: number;
}

export interface PivotTable {
  title: string;
  years: number[];
  rows: PivotRow[];
  yearTotals: Record<string, number>;
  grandTotal: number;
}
