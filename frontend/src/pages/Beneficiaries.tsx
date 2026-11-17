import React, { useEffect, useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BadgeCheck, Ban, Download, Pencil, Plus, Trash2 } from 'lucide-react';
import { PageHeader } from '../components/layout/PageHeader';
import { Column, DataTable } from '../components/shared/DataTable';
import { Modal } from '../components/shared/Modal';
import { ConfirmDialog } from '../components/shared/ConfirmDialog';
import { StatusBadge } from '../components/shared/StatusBadge';
import { ErrorBanner, Field, Select, YES_NO, btnPrimary, btnSecondary, iconBtn, inputClass } from '../components/shared/Form';
import {
  createBeneficiary,
  deleteBeneficiary,
  getBeneficiaries,
  setVerification,
  updateBeneficiary,
} from '../api/beneficiaries';
import { exportBeneficiaries } from '../api/exports';
import { errorMessage } from '../api/client';
import { useLookups } from '../hooks/useLookups';
import { usePermissions } from '../hooks/usePermissions';
import type { Beneficiary, BeneficiaryParams, BeneficiaryRequest, VerificationStatus, YesNo } from '../types';

const VERIFICATION_OPTIONS = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'VERIFIED', label: 'Verified' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

interface FormState {
  name: string;
  beneficiaryCategory: string;
  contactNumber: string;
  address: string;
  referredBy: string;
  description: string;
  isActive: YesNo;
}

const emptyForm: FormState = {
  name: '',
  beneficiaryCategory: '',
  contactNumber: '',
  address: '',
  referredBy: '',
  description: '',
  isActive: 'Y',
};

/** Debounces free-text search so the API is not called on every keystroke. */
function useDebounced<T>(value: T, ms = 350): T {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

export const Beneficiaries: React.FC = () => {
  const { canEditBeneficiaries, canExport } = usePermissions();
  const { categories } = useLookups();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [verificationStatus, setVerificationStatus] = useState('');
  const [isActive, setIsActive] = useState('');
  const q = useDebounced(search);

  useEffect(() => setPage(0), [q, category, verificationStatus, isActive, size]);

  const params: BeneficiaryParams = { page, size, q, category, verificationStatus, isActive };
  const { data, isLoading } = useQuery({
    queryKey: ['beneficiaries', params],
    queryFn: () => getBeneficiaries(params),
    placeholderData: keepPreviousData,
  });

  const [editing, setEditing] = useState<Beneficiary | 'new' | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [pageError, setPageError] = useState<string | null>(null);
  const [toDelete, setToDelete] = useState<Beneficiary | null>(null);
  const [verifying, setVerifying] = useState<{ b: Beneficiary; status: VerificationStatus } | null>(null);
  const [verificationNote, setVerificationNote] = useState('');

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['beneficiaries'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    queryClient.invalidateQueries({ queryKey: ['master', 'beneficiary-categories'] });
  };

  const save = useMutation({
    mutationFn: () => {
      const body: BeneficiaryRequest = {
        name: form.name.trim(),
        beneficiaryCategory: form.beneficiaryCategory || undefined,
        contactNumber: form.contactNumber || undefined,
        address: form.address || undefined,
        referredBy: form.referredBy || undefined,
        description: form.description || undefined,
        isActive: form.isActive,
      };
      if (editing && editing !== 'new') {
        body.beneficiaryCode = editing.beneficiaryCode;
        body.zakatYear = editing.zakatYear ?? undefined;
        body.verificationStatus = editing.verificationStatus;
        body.verificationNote = editing.verificationNote ?? undefined;
        return updateBeneficiary(editing.beneficiaryId, body);
      }
      return createBeneficiary(body);
    },
    onSuccess: () => {
      refresh();
      setEditing(null);
    },
    onError: (e) => setFormError(errorMessage(e)),
  });

  const verify = useMutation({
    mutationFn: () => setVerification(verifying!.b.beneficiaryId, verifying!.status, verificationNote || undefined),
    onSuccess: () => {
      refresh();
      setVerifying(null);
    },
    onError: (e) => {
      setPageError(errorMessage(e));
      setVerifying(null);
    },
  });

  const remove = useMutation({
    mutationFn: (b: Beneficiary) => deleteBeneficiary(b.beneficiaryId),
    onSuccess: () => {
      refresh();
      setToDelete(null);
    },
    onError: (e) => {
      setPageError(errorMessage(e));
      setToDelete(null);
    },
  });

  const open = (b: Beneficiary | null) => {
    setFormError(null);
    setForm(
      b
        ? {
            name: b.name,
            beneficiaryCategory: b.beneficiaryCategory ?? '',
            contactNumber: b.contactNumber ?? '',
            address: b.address ?? '',
            referredBy: b.referredBy ?? '',
            description: b.description ?? '',
            isActive: b.isActive,
          }
        : emptyForm
    );
    setEditing(b ?? 'new');
  };

  const columns: Column<Beneficiary>[] = [
    { header: 'Code', cell: (b) => <span className="font-mono text-[11px]">{b.beneficiaryCode}</span> },
    {
      header: 'Name',
      cell: (b) => (
        <div>
          <div className="font-medium">{b.name}</div>
          {b.description && <div className="text-[10px] text-[#5B6B67] line-clamp-1 max-w-xs">{b.description}</div>}
        </div>
      ),
    },
    { header: 'Category', cell: (b) => b.beneficiaryCategory ?? '—' },
    { header: 'Contact', cell: (b) => b.contactNumber ?? '—' },
    { header: 'Referred By', cell: (b) => b.referredBy ?? '—' },
    { header: 'Verification', align: 'center', cell: (b) => <StatusBadge status={b.verificationStatus} size="sm" /> },
    { header: 'Active', align: 'center', cell: (b) => <StatusBadge status={b.isActive === 'Y' ? 'ACTIVE' : 'INACTIVE'} size="sm" /> },
  ];
  if (canEditBeneficiaries) {
    columns.push({
      header: '',
      align: 'right',
      cell: (b) => (
        <div className="flex justify-end gap-1">
          {b.verificationStatus !== 'VERIFIED' && (
            <button type="button" className={iconBtn} title="Mark verified" onClick={() => { setVerificationNote(''); setVerifying({ b, status: 'VERIFIED' }); }}>
              <BadgeCheck className="w-3.5 h-3.5" />
            </button>
          )}
          {b.verificationStatus !== 'CANCELLED' && (
            <button type="button" className={`${iconBtn} text-amber-600`} title="Cancel verification" onClick={() => { setVerificationNote(''); setVerifying({ b, status: 'CANCELLED' }); }}>
              <Ban className="w-3.5 h-3.5" />
            </button>
          )}
          <button type="button" className={iconBtn} title="Edit" onClick={() => open(b)}>
            <Pencil className="w-3.5 h-3.5" />
          </button>
          <button type="button" className={`${iconBtn} text-rose-600`} title="Delete" onClick={() => setToDelete(b)}>
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      ),
    });
  }

  const categoryOptions = categories.map((c) => ({ value: c.name, label: c.name }));

  return (
    <div className="space-y-4">
      <PageHeader
        title="Beneficiaries"
        subtitle="Only Verified and Active beneficiaries can receive allocations."
        badge={data ? `${data.totalElements} records` : undefined}
        actions={
          <div className="flex gap-2">
            {canExport && (
              <button type="button" className={btnSecondary} onClick={() => exportBeneficiaries({ q, category, verificationStatus, isActive }).catch((e) => setPageError(errorMessage(e)))}>
                <Download className="w-3.5 h-3.5" /> Excel
              </button>
            )}
            {canEditBeneficiaries && (
              <button type="button" className={btnPrimary} onClick={() => open(null)}>
                <Plus className="w-3.5 h-3.5" /> New Beneficiary
              </button>
            )}
          </div>
        }
      />
      <ErrorBanner message={pageError} />

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        rowKey={(b) => b.beneficiaryId}
        isLoading={isLoading}
        totalElements={data?.totalElements}
        totalPages={data?.totalPages}
        page={page}
        pageSize={size}
        onPageChange={setPage}
        onPageSizeChange={setSize}
        searchQuery={search}
        onSearchChange={setSearch}
        searchPlaceholder="Search name, code, phone, address, referred by..."
        actions={
          <>
            <Select className="sm:w-40" value={category} onChange={setCategory} options={categoryOptions} placeholder="All categories" />
            <Select className="sm:w-36" value={verificationStatus} onChange={setVerificationStatus} options={VERIFICATION_OPTIONS} placeholder="Any verification" />
            <Select className="sm:w-28" value={isActive} onChange={setIsActive} options={[{ value: 'Y', label: 'Active' }, { value: 'N', label: 'Inactive' }]} placeholder="Any status" />
          </>
        }
      />

      <Modal
        isOpen={editing !== null}
        title={editing === 'new' ? 'New Beneficiary' : `Edit ${editing ? editing.beneficiaryCode : ''}`}
        onClose={() => setEditing(null)}
        footer={
          <>
            <button type="button" className={btnSecondary} onClick={() => setEditing(null)}>Cancel</button>
            <button type="submit" form="beneficiary-form" className={btnPrimary} disabled={save.isPending}>
              {save.isPending ? 'Saving...' : 'Save'}
            </button>
          </>
        }
      >
        <ErrorBanner message={formError} />
        <form
          id="beneficiary-form"
          onSubmit={(e) => {
            e.preventDefault();
            if (!form.name.trim()) return setFormError('Name is required.');
            save.mutate();
          }}
          className="grid grid-cols-1 sm:grid-cols-2 gap-3"
        >
          <Field label="Name" required className="sm:col-span-2">
            <input className={inputClass} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </Field>
          <Field label="Category">
            <Select value={form.beneficiaryCategory} onChange={(v) => setForm({ ...form, beneficiaryCategory: v })} options={categoryOptions} placeholder="— Select —" />
          </Field>
          <Field label="Contact Number">
            <input className={inputClass} value={form.contactNumber} onChange={(e) => setForm({ ...form, contactNumber: e.target.value })} />
          </Field>
          <Field label="Referred By">
            <input className={inputClass} value={form.referredBy} onChange={(e) => setForm({ ...form, referredBy: e.target.value })} />
          </Field>
          <Field label="Active">
            <Select value={form.isActive} onChange={(v) => setForm({ ...form, isActive: v as YesNo })} options={YES_NO} />
          </Field>
          <Field label="Address" className="sm:col-span-2">
            <input className={inputClass} value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
          </Field>
          <Field label="Description / Situation" className="sm:col-span-2">
            <textarea className={inputClass} rows={4} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </Field>
          {editing === 'new' && (
            <p className="sm:col-span-2 text-[11px] text-[#5B6B67]">
              New beneficiaries start as <strong>Pending</strong>. Verify them from the list before making an allocation.
            </p>
          )}
        </form>
      </Modal>

      <Modal
        isOpen={verifying !== null}
        title={verifying?.status === 'VERIFIED' ? `Verify ${verifying?.b.name ?? ''}` : `Cancel verification: ${verifying?.b.name ?? ''}`}
        onClose={() => setVerifying(null)}
        size="md"
        footer={
          <>
            <button type="button" className={btnSecondary} onClick={() => setVerifying(null)}>Close</button>
            <button type="button" className={btnPrimary} disabled={verify.isPending} onClick={() => verify.mutate()}>
              {verify.isPending ? 'Saving...' : 'Confirm'}
            </button>
          </>
        }
      >
        <Field label="Verification note">
          <textarea className={inputClass} rows={3} value={verificationNote} onChange={(e) => setVerificationNote(e.target.value)} />
        </Field>
      </Modal>

      <ConfirmDialog
        isOpen={toDelete !== null}
        title="Delete beneficiary"
        message={`Delete ${toDelete?.name ?? ''} (${toDelete?.beneficiaryCode ?? ''})? This is blocked when the beneficiary has allocations; deactivate instead.`}
        confirmLabel="Delete"
        isDestructive
        isLoading={remove.isPending}
        onConfirm={() => toDelete && remove.mutate(toDelete)}
        onCancel={() => setToDelete(null)}
      />
    </div>
  );
};
