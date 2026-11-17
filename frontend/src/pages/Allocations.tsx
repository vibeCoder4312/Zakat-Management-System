import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, FileDown, Paperclip, Pencil, Plus, Trash2 } from 'lucide-react';
import { PageHeader } from '../components/layout/PageHeader';
import { Column, DataTable } from '../components/shared/DataTable';
import { Modal } from '../components/shared/Modal';
import { ConfirmDialog } from '../components/shared/ConfirmDialog';
import { StatusBadge } from '../components/shared/StatusBadge';
import { KpiCard } from '../components/shared/KpiCard';
import { ErrorBanner, Field, Select, YES_NO, btnPrimary, btnSecondary, iconBtn, inputClass, toNumber } from '../components/shared/Form';
import {
  createAllocation,
  deleteAllocation,
  getAllocations,
  getAllocationSummary,
  updateAllocation,
  uploadReceipt,
} from '../api/allocations';
import { searchVerifiedBeneficiaries } from '../api/beneficiaries';
import { downloadReceipt, exportAllocations } from '../api/exports';
import { errorMessage } from '../api/client';
import { useLookups } from '../hooks/useLookups';
import { usePermissions } from '../hooks/usePermissions';
import { formatINR } from '../lib/formatCurrency';
import type {
  Allocation,
  AllocationFilters,
  AllocationRequest,
  AllocationStatus,
  ApprovalStatus,
  Beneficiary,
  Currency,
  YesNo,
} from '../types';

const STATUS_OPTIONS = [
  { value: 'PLANNED', label: 'Planned' },
  { value: 'PAID', label: 'Paid' },
  { value: 'CANCELLED', label: 'Cancelled' },
];
const APPROVAL_OPTIONS = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
];
const METHOD_OPTIONS = [
  { value: 'CASH', label: 'Cash' },
  { value: 'ONLINE', label: 'Online' },
  { value: 'BANK', label: 'Bank' },
];

function useDebounced<T>(value: T, ms = 350): T {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

interface FormState {
  zakatYear: string;
  classificationId: string;
  beneficiaryId: number | null;
  beneficiaryName: string;
  category: string;
  plannedAmount: string;
  paidAmount: string;
  paidOn: string;
  paymentMethod: string;
  paidSourceAccountId: string;
  currencyCode: Currency;
  foreignAmount: string;
  fxRate: string;
  schoolId: string;
  currentStd: string;
  prevStdResult: string;
  paidViaAgent: string;
  paidViaEmployee: string;
  receiptReceived: YesNo;
  status: AllocationStatus;
  approvalStatus: ApprovalStatus;
  referenceNo: string;
  remarks: string;
}

const str = (v: unknown) => (v === null || v === undefined ? '' : String(v));

function toForm(a: Allocation | null, defaultYear: number): FormState {
  return {
    zakatYear: a ? String(a.zakatYear) : String(defaultYear),
    classificationId: str(a?.classificationId),
    beneficiaryId: a?.beneficiaryId ?? null,
    beneficiaryName: a?.beneficiary ?? '',
    category: str(a?.category),
    plannedAmount: str(a?.plannedAmount),
    paidAmount: a ? str(a.paidAmount) : '',
    paidOn: str(a?.paidOn),
    paymentMethod: str(a?.paymentMethod),
    paidSourceAccountId: str(a?.paidSourceAccountId),
    currencyCode: a?.currencyCode ?? 'INR',
    foreignAmount: str(a?.foreignAmount),
    fxRate: str(a?.fxRate),
    schoolId: str(a?.schoolId),
    currentStd: str(a?.currentStd),
    prevStdResult: str(a?.prevStdResult),
    paidViaAgent: str(a?.paidViaAgent),
    paidViaEmployee: str(a?.paidViaEmployee),
    receiptReceived: a?.receiptReceived ?? 'N',
    status: a?.status ?? 'PLANNED',
    approvalStatus: a?.approvalStatus ?? 'PENDING',
    referenceNo: str(a?.referenceNo),
    remarks: str(a?.remarks),
  };
}

function toRequest(f: FormState): AllocationRequest {
  const blank = (v: string) => (v.trim() === '' ? undefined : v.trim());
  return {
    zakatYear: Number(f.zakatYear),
    classificationId: Number(f.classificationId),
    beneficiaryId: f.beneficiaryId as number,
    category: blank(f.category),
    plannedAmount: toNumber(f.plannedAmount) ?? 0,
    paidAmount: toNumber(f.paidAmount) ?? 0,
    paidOn: blank(f.paidOn) ?? null,
    paymentMethod: blank(f.paymentMethod) ?? null,
    paidSourceAccountId: toNumber(f.paidSourceAccountId) ?? null,
    currencyCode: f.currencyCode,
    foreignAmount: f.currencyCode === 'INR' ? null : toNumber(f.foreignAmount) ?? null,
    fxRate: f.currencyCode === 'INR' ? null : toNumber(f.fxRate) ?? null,
    schoolId: toNumber(f.schoolId) ?? null,
    currentStd: blank(f.currentStd),
    prevStdResult: blank(f.prevStdResult),
    paidViaAgent: blank(f.paidViaAgent),
    paidViaEmployee: blank(f.paidViaEmployee),
    receiptReceived: f.receiptReceived,
    status: f.status,
    approvalStatus: f.approvalStatus,
    referenceNo: blank(f.referenceNo),
    remarks: blank(f.remarks),
  };
}

/** Search box limited to Verified + Active beneficiaries (the only ones the API accepts). */
const BeneficiaryPicker: React.FC<{ value: string; onPick: (b: Beneficiary) => void }> = ({ value, onPick }) => {
  const [text, setText] = useState(value);
  const [open, setOpen] = useState(false);
  const q = useDebounced(text, 250);
  const ref = useRef<HTMLDivElement>(null);
  const { data = [], isFetching } = useQuery({
    queryKey: ['beneficiary-typeahead', q],
    queryFn: () => searchVerifiedBeneficiaries(q, 15),
    enabled: open,
  });

  useEffect(() => setText(value), [value]);
  useEffect(() => {
    const close = (e: MouseEvent) => ref.current && !ref.current.contains(e.target as Node) && setOpen(false);
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  return (
    <div className="relative" ref={ref}>
      <input
        className={inputClass}
        value={text}
        placeholder="Type a name or BEN- code..."
        onFocus={() => setOpen(true)}
        onChange={(e) => {
          setText(e.target.value);
          setOpen(true);
        }}
      />
      {open && (
        <ul className="absolute z-20 mt-1 w-full max-h-56 overflow-y-auto bg-white border border-[#E1E8E5] rounded-md shadow-lg text-xs">
          {isFetching && <li className="px-3 py-2 text-gray-500">Searching...</li>}
          {!isFetching && data.length === 0 && <li className="px-3 py-2 text-gray-500">No verified, active beneficiary matches.</li>}
          {data.map((b) => (
            <li key={b.beneficiaryId}>
              <button
                type="button"
                className="w-full text-left px-3 py-1.5 hover:bg-[#F2F6F4]"
                onClick={() => {
                  onPick(b);
                  setText(b.name);
                  setOpen(false);
                }}
              >
                <span className="font-medium">{b.name}</span>{' '}
                <span className="text-[10px] text-gray-500">
                  {b.beneficiaryCode} · {b.beneficiaryCategory ?? 'No category'}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export const Allocations: React.FC = () => {
  const { selectedYears } = useOutletContext<{ selectedYears: number[] }>();
  const { canEditAllocations, canExport } = usePermissions();
  const lookups = useLookups();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [search, setSearch] = useState('');
  const [classificationId, setClassificationId] = useState('');
  const [category, setCategory] = useState('');
  const [status, setStatus] = useState('');
  const [approvalStatus, setApprovalStatus] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('');
  const q = useDebounced(search);

  const filters: AllocationFilters = useMemo(
    () => ({
      years: selectedYears,
      classificationId: toNumber(classificationId),
      category,
      status,
      approvalStatus,
      paymentMethod,
      q,
    }),
    [selectedYears, classificationId, category, status, approvalStatus, paymentMethod, q]
  );
  useEffect(() => setPage(0), [filters, size]);

  const { data, isLoading } = useQuery({
    queryKey: ['allocations', filters, page, size],
    queryFn: () => getAllocations({ ...filters, page, size, sort: 'zakatYear,desc' }),
    placeholderData: keepPreviousData,
  });
  const { data: summary } = useQuery({
    queryKey: ['allocations-summary', filters],
    queryFn: () => getAllocationSummary(filters),
  });

  const [editing, setEditing] = useState<Allocation | 'new' | null>(null);
  const [form, setForm] = useState<FormState>(toForm(null, new Date().getFullYear()));
  const [formError, setFormError] = useState<string | null>(null);
  const [pageError, setPageError] = useState<string | null>(null);
  const [toDelete, setToDelete] = useState<Allocation | null>(null);
  const [receiptFor, setReceiptFor] = useState<Allocation | null>(null);
  const [receiptFile, setReceiptFile] = useState<File | null>(null);

  const set = <K extends keyof FormState>(k: K, v: FormState[K]) => setForm((f) => ({ ...f, [k]: v }));

  const refresh = () => {
    ['allocations', 'allocations-summary', 'assessments', 'dashboard', 'planning'].forEach((k) =>
      queryClient.invalidateQueries({ queryKey: [k] })
    );
  };

  const save = useMutation({
    mutationFn: () => {
      const body = toRequest(form);
      return editing && editing !== 'new' ? updateAllocation(editing.allocationId, body) : createAllocation(body);
    },
    onSuccess: () => {
      refresh();
      setEditing(null);
    },
    onError: (e) => setFormError(errorMessage(e)),
  });

  const remove = useMutation({
    mutationFn: (a: Allocation) => deleteAllocation(a.allocationId),
    onSuccess: () => {
      refresh();
      setToDelete(null);
    },
    onError: (e) => {
      setPageError(errorMessage(e));
      setToDelete(null);
    },
  });

  const receipt = useMutation({
    mutationFn: () => uploadReceipt(receiptFor!.allocationId, receiptFile!),
    onSuccess: () => {
      refresh();
      setReceiptFor(null);
    },
    onError: (e) => setFormError(errorMessage(e)),
  });

  const defaultYear = selectedYears.length ? Math.max(...selectedYears) : lookups.years[0] ?? new Date().getFullYear();

  const open = (a: Allocation | null) => {
    setFormError(null);
    setForm(toForm(a, defaultYear));
    setEditing(a ?? 'new');
  };

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.zakatYear) return setFormError('Select a Zakat Year for this allocation.');
    if (!form.classificationId) return setFormError('Classification is required.');
    if (!form.beneficiaryId) return setFormError('Pick a verified beneficiary from the list.');
    if (toNumber(form.plannedAmount) === undefined) return setFormError('Planned amount is required.');
    if (form.currencyCode !== 'INR' && (toNumber(form.foreignAmount) === undefined || toNumber(form.fxRate) === undefined))
      return setFormError('Foreign amount and FX rate are required for non-INR payments.');
    save.mutate();
  };

  const fxPreview =
    form.currencyCode !== 'INR' && toNumber(form.foreignAmount) !== undefined && toNumber(form.fxRate) !== undefined
      ? (toNumber(form.foreignAmount) as number) * (toNumber(form.fxRate) as number)
      : null;

  const classificationOptions = lookups.classifications.map((c) => ({
    value: c.classificationId,
    label: `${c.name} (${c.fundType === 'SADAQAH' ? 'Sadaqah' : 'Zakat'})`,
  }));
  const categoryOptions = lookups.categories.map((c) => ({ value: c.name, label: c.name }));
  const isSchoolFees = lookups.classifications.find((c) => String(c.classificationId) === form.classificationId)?.name === 'School fees';

  const columns: Column<Allocation>[] = [
    { header: 'Year', cell: (a) => a.zakatYear },
    {
      header: 'Beneficiary',
      cell: (a) => (
        <div>
          <div className="font-medium">{a.beneficiary}</div>
          {a.school && <div className="text-[10px] text-[#5B6B67]">{a.school}{a.currentStd ? ` · ${a.currentStd}` : ''}</div>}
        </div>
      ),
    },
    {
      header: 'Classification',
      cell: (a) => (
        <div>
          <div>{a.classification}</div>
          {a.fundType === 'SADAQAH' && <span className="text-[10px] text-cyan-700 font-semibold">Sadaqah</span>}
        </div>
      ),
    },
    { header: 'Category', cell: (a) => a.category ?? '—' },
    { header: 'Planned', align: 'right', cell: (a) => formatINR(a.plannedAmount) },
    {
      header: 'Paid',
      align: 'right',
      cell: (a) => (
        <div>
          <div className="font-semibold">{formatINR(a.paidAmount)}</div>
          {a.currencyCode !== 'INR' && (
            <div className="text-[10px] text-[#5B6B67]">
              {a.currencyCode}
              {a.foreignAmount != null ? ` ${a.foreignAmount.toLocaleString('en-US')}` : ''}
              {a.fxRate != null ? ` @ ${a.fxRate}` : ''}
            </div>
          )}
        </div>
      ),
    },
    {
      header: 'Payment',
      cell: (a) => (
        <div className="text-[11px]">
          <div>{a.paymentMethod ?? <span className="text-gray-400">Not set</span>}</div>
          <div className="text-[#5B6B67]">{a.paidOn ?? ''}{a.paidSourceAccount ? ` · ${a.paidSourceAccount}` : ''}</div>
        </div>
      ),
    },
    {
      header: 'Status',
      align: 'center',
      cell: (a) => (
        <div className="flex flex-col items-center gap-1">
          <StatusBadge status={a.status} size="sm" />
          {a.approvalStatus === 'PENDING' && <span className="text-[10px] text-amber-700">Approval pending</span>}
        </div>
      ),
    },
  ];
  columns.push({
    header: '',
    align: 'right',
    cell: (a) => (
      <div className="flex justify-end gap-0.5">
        {a.receiptReceived === 'Y' && (
          <button type="button" className={iconBtn} title="Download receipt" onClick={() => downloadReceipt(a.allocationId).catch((e) => setPageError(errorMessage(e, 'Receipt marked as received, but no file was uploaded.')))}>
            <FileDown className="w-3.5 h-3.5" />
          </button>
        )}
        {canEditAllocations && (
          <>
            <button type="button" className={iconBtn} title="Upload receipt" onClick={() => { setFormError(null); setReceiptFile(null); setReceiptFor(a); }}>
              <Paperclip className="w-3.5 h-3.5" />
            </button>
            <button type="button" className={iconBtn} title="Edit" onClick={() => open(a)}>
              <Pencil className="w-3.5 h-3.5" />
            </button>
            <button type="button" className={`${iconBtn} text-rose-600`} title="Delete" onClick={() => setToDelete(a)}>
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          </>
        )}
      </div>
    ),
  });

  return (
    <div className="space-y-4">
      <PageHeader
        title="Allocations & Payments"
        subtitle={selectedYears.length ? `Showing Zakat year ${selectedYears.join(', ')}` : 'Showing all years (use the year filter at the top to narrow)'}
        actions={
          <div className="flex gap-2">
            {canExport && (
              <button type="button" className={btnSecondary} onClick={() => exportAllocations(filters).catch((e) => setPageError(errorMessage(e)))}>
                <Download className="w-3.5 h-3.5" /> Excel
              </button>
            )}
            {canEditAllocations && (
              <button type="button" className={btnPrimary} onClick={() => open(null)}>
                <Plus className="w-3.5 h-3.5" /> New Allocation
              </button>
            )}
          </div>
        }
      />

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <KpiCard title="Allocations" value={(summary?.totalAllocations ?? 0).toLocaleString('en-IN')} color="indigo" />
        <KpiCard title="Total Allocated" value={formatINR(summary?.totalAllocated)} color="sky" />
        <KpiCard title="Total Paid" value={formatINR(summary?.totalPaid)} color="emerald" />
        <KpiCard title="Outstanding" value={formatINR(summary?.remainingAmount)} color="amber" subtitle="Planned but not yet paid" />
      </div>

      <ErrorBanner message={pageError} />

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        rowKey={(a) => a.allocationId}
        isLoading={isLoading}
        totalElements={data?.totalElements}
        totalPages={data?.totalPages}
        page={page}
        pageSize={size}
        onPageChange={setPage}
        onPageSizeChange={setSize}
        searchQuery={search}
        onSearchChange={setSearch}
        searchPlaceholder="Search beneficiary, school, remarks..."
        actions={
          <div className="flex flex-wrap gap-2">
            <Select className="sm:w-40" value={classificationId} onChange={setClassificationId} options={classificationOptions} placeholder="All classifications" />
            <Select className="sm:w-32" value={category} onChange={setCategory} options={categoryOptions} placeholder="All categories" />
            <Select className="sm:w-28" value={status} onChange={setStatus} options={STATUS_OPTIONS} placeholder="Any status" />
            <Select className="sm:w-28" value={approvalStatus} onChange={setApprovalStatus} options={APPROVAL_OPTIONS} placeholder="Any approval" />
            <Select className="sm:w-28" value={paymentMethod} onChange={setPaymentMethod} options={METHOD_OPTIONS} placeholder="Any method" />
          </div>
        }
      />

      <Modal
        isOpen={editing !== null}
        title={editing === 'new' ? 'New Allocation' : `Edit Allocation #${editing ? editing.allocationId : ''}`}
        onClose={() => setEditing(null)}
        size="xl"
        footer={
          <>
            <button type="button" className={btnSecondary} onClick={() => setEditing(null)}>Cancel</button>
            <button type="submit" form="allocation-form" className={btnPrimary} disabled={save.isPending}>
              {save.isPending ? 'Saving...' : 'Save'}
            </button>
          </>
        }
      >
        <ErrorBanner message={formError} />
        <form id="allocation-form" onSubmit={submit} className="space-y-4">
          <section>
            <h3 className="text-[11px] font-bold uppercase tracking-wider text-[#0F766E] mb-2">Allocation</h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <Field label="Zakat Year" required>
                <Select value={form.zakatYear} onChange={(v) => set('zakatYear', v)} options={lookups.years.map((y) => ({ value: y, label: String(y) }))} placeholder="— Select —" />
              </Field>
              <Field label="Beneficiary" required className="sm:col-span-2" hint="Only Verified and Active beneficiaries are listed.">
                <BeneficiaryPicker
                  value={form.beneficiaryName}
                  onPick={(b) =>
                    setForm((f) => ({
                      ...f,
                      beneficiaryId: b.beneficiaryId,
                      beneficiaryName: b.name,
                      category: f.category || (b.beneficiaryCategory ?? ''),
                    }))
                  }
                />
              </Field>
              <Field label="Classification" required>
                <Select value={form.classificationId} onChange={(v) => set('classificationId', v)} options={classificationOptions} placeholder="— Select —" />
              </Field>
              <Field label="Category" hint="Defaults to the beneficiary's category. Used to match Planning.">
                <Select value={form.category} onChange={(v) => set('category', v)} options={categoryOptions} placeholder="— Beneficiary's category —" />
              </Field>
              <Field label="Planned Amount (₹)" required>
                <input className={inputClass} type="number" step="0.01" min="0" value={form.plannedAmount} onChange={(e) => set('plannedAmount', e.target.value)} />
              </Field>
              <Field label="Status" hint="Becomes Paid automatically once a paid amount is entered.">
                <Select value={form.status} onChange={(v) => set('status', v as AllocationStatus)} options={STATUS_OPTIONS} />
              </Field>
              <Field label="Approval">
                <Select value={form.approvalStatus} onChange={(v) => set('approvalStatus', v as ApprovalStatus)} options={APPROVAL_OPTIONS} />
              </Field>
            </div>
          </section>

          <section>
            <h3 className="text-[11px] font-bold uppercase tracking-wider text-[#0F766E] mb-2">Payment</h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <Field label="Currency">
                <Select value={form.currencyCode} onChange={(v) => set('currencyCode', v as Currency)} options={[{ value: 'INR', label: 'INR (₹)' }, { value: 'AED', label: 'AED' }]} />
              </Field>
              {form.currencyCode === 'INR' ? (
                <Field label="Paid Amount (₹)" className="sm:col-span-2">
                  <input className={inputClass} type="number" step="0.01" min="0" value={form.paidAmount} onChange={(e) => set('paidAmount', e.target.value)} />
                </Field>
              ) : (
                <>
                  <Field label={`Foreign Amount (${form.currencyCode})`} required>
                    <input className={inputClass} type="number" step="0.01" value={form.foreignAmount} onChange={(e) => set('foreignAmount', e.target.value)} />
                  </Field>
                  <Field label="FX Rate (₹ per unit)" required hint={fxPreview !== null ? `Paid amount will be ${formatINR(fxPreview)}` : undefined}>
                    <input className={inputClass} type="number" step="0.0001" value={form.fxRate} onChange={(e) => set('fxRate', e.target.value)} />
                  </Field>
                </>
              )}
              <Field label="Paid On">
                <input className={inputClass} type="date" value={form.paidOn} onChange={(e) => set('paidOn', e.target.value)} />
              </Field>
              <Field label="Payment Method">
                <Select value={form.paymentMethod} onChange={(v) => set('paymentMethod', v)} options={METHOD_OPTIONS} placeholder="Not set" />
              </Field>
              <Field label="Paid From (Source Account)">
                <Select value={form.paidSourceAccountId} onChange={(v) => set('paidSourceAccountId', v)} options={lookups.sourceAccounts.map((s) => ({ value: s.sourceAccountId, label: s.name }))} placeholder="Not set" />
              </Field>
              <Field label="Paid Via Agent">
                <Select value={form.paidViaAgent} onChange={(v) => set('paidViaAgent', v)} options={lookups.agents.map((a) => ({ value: a.name, label: a.name }))} placeholder="—" />
              </Field>
              <Field label="Paid Via Employee">
                <Select value={form.paidViaEmployee} onChange={(v) => set('paidViaEmployee', v)} options={lookups.employees.map((e) => ({ value: e.name, label: e.name }))} placeholder="—" />
              </Field>
              <Field label="Receipt Received">
                <Select value={form.receiptReceived} onChange={(v) => set('receiptReceived', v as YesNo)} options={YES_NO} />
              </Field>
              <Field label="Reference No.">
                <input className={inputClass} value={form.referenceNo} onChange={(e) => set('referenceNo', e.target.value)} />
              </Field>
            </div>
          </section>

          <section>
            <h3 className="text-[11px] font-bold uppercase tracking-wider text-[#0F766E] mb-2">School Fees {isSchoolFees ? '' : '(optional)'}</h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <Field label="Institute">
                <Select value={form.schoolId} onChange={(v) => set('schoolId', v)} options={lookups.schools.map((s) => ({ value: s.schoolId, label: s.name }))} placeholder="—" />
              </Field>
              <Field label="Current Std">
                <input className={inputClass} value={form.currentStd} onChange={(e) => set('currentStd', e.target.value)} />
              </Field>
              <Field label="Previous Std Result">
                <input className={inputClass} value={form.prevStdResult} onChange={(e) => set('prevStdResult', e.target.value)} />
              </Field>
            </div>
          </section>

          <Field label="Remarks">
            <textarea className={inputClass} rows={2} value={form.remarks} onChange={(e) => set('remarks', e.target.value)} />
          </Field>
        </form>
      </Modal>

      <Modal
        isOpen={receiptFor !== null}
        title={`Upload receipt — ${receiptFor?.beneficiary ?? ''}`}
        onClose={() => setReceiptFor(null)}
        size="md"
        footer={
          <>
            <button type="button" className={btnSecondary} onClick={() => setReceiptFor(null)}>Cancel</button>
            <button type="button" className={btnPrimary} disabled={!receiptFile || receipt.isPending} onClick={() => receipt.mutate()}>
              {receipt.isPending ? 'Uploading...' : 'Upload'}
            </button>
          </>
        }
      >
        <ErrorBanner message={formError} />
        <Field label="Receipt file (PDF or image, max 10 MB)">
          <input type="file" accept="application/pdf,image/*" className="text-xs" onChange={(e) => setReceiptFile(e.target.files?.[0] ?? null)} />
        </Field>
      </Modal>

      <ConfirmDialog
        isOpen={toDelete !== null}
        title="Delete allocation"
        message={`Delete the ${toDelete?.zakatYear ?? ''} allocation to ${toDelete?.beneficiary ?? ''} (${formatINR(toDelete?.plannedAmount)})? The year's totals are recalculated.`}
        confirmLabel="Delete"
        isDestructive
        isLoading={remove.isPending}
        onConfirm={() => toDelete && remove.mutate(toDelete)}
        onCancel={() => setToDelete(null)}
      />
    </div>
  );
};
