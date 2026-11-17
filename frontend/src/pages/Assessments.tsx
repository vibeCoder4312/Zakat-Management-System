import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, Pencil, Plus, Trash2 } from 'lucide-react';
import { PageHeader } from '../components/layout/PageHeader';
import { Column, DataTable } from '../components/shared/DataTable';
import { Modal } from '../components/shared/Modal';
import { ConfirmDialog } from '../components/shared/ConfirmDialog';
import { StatusBadge } from '../components/shared/StatusBadge';
import { CurrencyDisplay } from '../components/shared/CurrencyDisplay';
import { ErrorBanner, Field, Select, btnPrimary, btnSecondary, iconBtn, inputClass, toNumber } from '../components/shared/Form';
import { createAssessment, deleteAssessment, getAssessments, updateAssessment } from '../api/assessments';
import { exportAssessments } from '../api/exports';
import { errorMessage } from '../api/client';
import { usePermissions } from '../hooks/usePermissions';
import { formatINR } from '../lib/formatCurrency';
import type { Assessment, AssessmentStatus } from '../types';

interface FormState {
  zakatYear: string;
  zakatAmount: string;
  carryForward: string;
  status: AssessmentStatus;
  remarks: string;
}

const emptyForm: FormState = {
  zakatYear: String(new Date().getFullYear()),
  zakatAmount: '',
  carryForward: '0',
  status: 'OPEN',
  remarks: '',
};

const sum = (rows: Assessment[], k: keyof Assessment) => rows.reduce((t, r) => t + Number(r[k] ?? 0), 0);

export const Assessments: React.FC = () => {
  const { canEditAssessments, canExport } = usePermissions();
  const queryClient = useQueryClient();
  const { data = [], isLoading } = useQuery({ queryKey: ['assessments'], queryFn: getAssessments });

  const [editing, setEditing] = useState<Assessment | 'new' | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [pageError, setPageError] = useState<string | null>(null);
  const [toDelete, setToDelete] = useState<Assessment | null>(null);

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['assessments'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    queryClient.invalidateQueries({ queryKey: ['lookups', 'years'] });
  };

  const save = useMutation({
    mutationFn: () => {
      const body = {
        zakatYear: Number(form.zakatYear),
        zakatAmount: toNumber(form.zakatAmount) ?? 0,
        carryForward: toNumber(form.carryForward) ?? 0,
        status: form.status,
        remarks: form.remarks || undefined,
      };
      return editing === 'new' || editing === null
        ? createAssessment(body)
        : updateAssessment(editing.assessmentId, body);
    },
    onSuccess: () => {
      refresh();
      setEditing(null);
    },
    onError: (e) => setFormError(errorMessage(e)),
  });

  const remove = useMutation({
    mutationFn: (a: Assessment) => deleteAssessment(a.assessmentId),
    onSuccess: () => {
      refresh();
      setToDelete(null);
    },
    onError: (e) => {
      setPageError(errorMessage(e));
      setToDelete(null);
    },
  });

  const open = (a: Assessment | null) => {
    setFormError(null);
    setForm(
      a
        ? {
            zakatYear: String(a.zakatYear),
            zakatAmount: String(a.zakatAmount),
            carryForward: String(a.carryForward),
            status: a.status,
            remarks: a.remarks ?? '',
          }
        : emptyForm
    );
    setEditing(a ?? 'new');
  };

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!/^\d{4}$/.test(form.zakatYear)) return setFormError('Enter a 4-digit Zakat year.');
    if (toNumber(form.zakatAmount) === undefined) return setFormError('Zakat amount is required.');
    save.mutate();
  };

  const netPreview = (toNumber(form.zakatAmount) ?? 0) - (toNumber(form.carryForward) ?? 0);

  const columns: Column<Assessment>[] = [
    { header: 'Year', cell: (a) => <span className="font-semibold">{a.zakatYear}</span> },
    { header: 'Zakat Amount', align: 'right', cell: (a) => formatINR(a.zakatAmount) },
    { header: 'Carry Forward', align: 'right', cell: (a) => formatINR(a.carryForward) },
    { header: 'Net Payable', align: 'right', cell: (a) => <span className="font-semibold">{formatINR(a.netPayable)}</span> },
    { header: 'Total Allocated', align: 'right', cell: (a) => formatINR(a.totalAllocatedAmount) },
    {
      header: 'Remaining',
      align: 'right',
      cell: (a) => <CurrencyDisplay amount={a.remainingBalance} highlightNegative className="font-semibold" />,
    },
    { header: 'Status', align: 'center', cell: (a) => <StatusBadge status={a.status} size="sm" /> },
    { header: 'Remarks', cell: (a) => <span className="text-[11px] text-[#5B6B67] line-clamp-2 max-w-xs">{a.remarks ?? ''}</span> },
  ];
  if (canEditAssessments) {
    columns.push({
      header: '',
      align: 'right',
      cell: (a) => (
        <div className="flex justify-end gap-1">
          <button type="button" className={iconBtn} title="Edit" onClick={() => open(a)}>
            <Pencil className="w-3.5 h-3.5" />
          </button>
          <button type="button" className={`${iconBtn} text-rose-600`} title="Delete" onClick={() => setToDelete(a)}>
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      ),
    });
  }

  const footer = data.length > 0 && (
    <tr className="bg-[#F6FAF8] font-bold text-[#10231F] border-t-2 border-[#C9A84C]">
      <td className="py-2.5 px-4">Total</td>
      <td className="py-2.5 px-4 text-right">{formatINR(sum(data, 'zakatAmount'))}</td>
      <td className="py-2.5 px-4 text-right">{formatINR(sum(data, 'carryForward'))}</td>
      <td className="py-2.5 px-4 text-right">{formatINR(sum(data, 'netPayable'))}</td>
      <td className="py-2.5 px-4 text-right">{formatINR(sum(data, 'totalAllocatedAmount'))}</td>
      <td className="py-2.5 px-4 text-right">
        <CurrencyDisplay amount={sum(data, 'remainingBalance')} highlightNegative />
      </td>
      <td colSpan={canEditAssessments ? 3 : 2} />
    </tr>
  );

  return (
    <div className="space-y-4">
      <PageHeader
        title="Zakat Assessments"
        subtitle="Zakat due for each year. Total Allocated counts Zakat-fund allocations only (Sadaqah is excluded)."
        actions={
          <div className="flex gap-2">
            {canExport && (
              <button type="button" className={btnSecondary} onClick={() => exportAssessments().catch((e) => setPageError(errorMessage(e)))}>
                <Download className="w-3.5 h-3.5" /> Excel
              </button>
            )}
            {canEditAssessments && (
              <button type="button" className={btnPrimary} onClick={() => open(null)}>
                <Plus className="w-3.5 h-3.5" /> New Year
              </button>
            )}
          </div>
        }
      />
      <ErrorBanner message={pageError} />
      <DataTable columns={columns} data={data} rowKey={(a) => a.assessmentId} isLoading={isLoading} footerRow={footer} />

      <Modal
        isOpen={editing !== null}
        title={editing === 'new' ? 'New Zakat Year' : `Edit Zakat ${editing ? editing.zakatYear : ''}`}
        onClose={() => setEditing(null)}
        size="md"
        footer={
          <>
            <button type="button" className={btnSecondary} onClick={() => setEditing(null)}>Cancel</button>
            <button type="submit" form="assessment-form" className={btnPrimary} disabled={save.isPending}>
              {save.isPending ? 'Saving...' : 'Save'}
            </button>
          </>
        }
      >
        <ErrorBanner message={formError} />
        <form id="assessment-form" onSubmit={submit} className="grid grid-cols-2 gap-3">
          <Field label="Zakat Year" required>
            <input className={inputClass} inputMode="numeric" value={form.zakatYear} onChange={(e) => setForm({ ...form, zakatYear: e.target.value })} />
          </Field>
          <Field label="Status">
            <Select
              value={form.status}
              onChange={(v) => setForm({ ...form, status: v as AssessmentStatus })}
              options={[{ value: 'OPEN', label: 'Open' }, { value: 'CLOSED', label: 'Closed' }]}
            />
          </Field>
          <Field label="Zakat Amount (₹)" required>
            <input className={inputClass} type="number" step="0.01" value={form.zakatAmount} onChange={(e) => setForm({ ...form, zakatAmount: e.target.value })} />
          </Field>
          <Field label="Carry Forward (₹)" hint="Amount already paid in advance; reduces net payable.">
            <input className={inputClass} type="number" step="0.01" value={form.carryForward} onChange={(e) => setForm({ ...form, carryForward: e.target.value })} />
          </Field>
          <div className="col-span-2 text-xs text-[#5B6B67]">
            Net payable: <span className="font-semibold text-[#10231F]">{formatINR(netPreview)}</span>
          </div>
          <Field label="Remarks" className="col-span-2">
            <textarea className={inputClass} rows={3} value={form.remarks} onChange={(e) => setForm({ ...form, remarks: e.target.value })} />
          </Field>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={toDelete !== null}
        title="Delete Zakat year"
        message={`Delete the ${toDelete?.zakatYear ?? ''} assessment? This is blocked while allocations exist for the year.`}
        confirmLabel="Delete"
        isDestructive
        isLoading={remove.isPending}
        onConfirm={() => toDelete && remove.mutate(toDelete)}
        onCancel={() => setToDelete(null)}
      />
    </div>
  );
};
