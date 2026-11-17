import React, { useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ChevronDown, ChevronRight, Pencil, Plus, Trash2 } from 'lucide-react';
import { PageHeader } from '../components/layout/PageHeader';
import { Column, DataTable } from '../components/shared/DataTable';
import { Modal } from '../components/shared/Modal';
import { ConfirmDialog } from '../components/shared/ConfirmDialog';
import { StatusBadge } from '../components/shared/StatusBadge';
import { CurrencyDisplay } from '../components/shared/CurrencyDisplay';
import { KpiCard } from '../components/shared/KpiCard';
import { LoadingSpinner } from '../components/shared/LoadingSpinner';
import { ErrorBanner, Field, Select, btnPrimary, btnSecondary, iconBtn, inputClass, toNumber } from '../components/shared/Form';
import { createPlanning, deletePlanning, getPlanning, getPlanningAllocations, updatePlanning } from '../api/planning';
import { errorMessage } from '../api/client';
import { useLookups } from '../hooks/useLookups';
import { usePermissions } from '../hooks/usePermissions';
import { formatINR } from '../lib/formatCurrency';
import type { Planning } from '../types';

interface FormState {
  zakatYear: string;
  classificationId: string;
  category: string;
  plannedAmount: string;
  remarks: string;
}

/** Allocation lines behind one planning row (same year + classification + category). */
const Drilldown: React.FC<{ planningId: number }> = ({ planningId }) => {
  const { data, isLoading } = useQuery({
    queryKey: ['planning', 'drilldown', planningId],
    queryFn: () => getPlanningAllocations(planningId),
  });
  if (isLoading) return <LoadingSpinner size="sm" text="Loading allocations..." />;
  const rows = (data ?? []).filter((a) => a.paidAmount > 0 || a.plannedAmount > 0);
  if (rows.length === 0) return <p className="text-xs text-[#5B6B67]">No allocations match this plan yet.</p>;
  return (
    <div className="max-h-72 overflow-y-auto border border-[#E1E8E5] rounded bg-white">
      <table className="w-full text-[11px]">
        <thead className="bg-[#F6FAF8] text-[#5B6B67] sticky top-0">
          <tr>
            <th className="text-left px-3 py-1.5">Beneficiary</th>
            <th className="text-left px-3 py-1.5">School</th>
            <th className="text-right px-3 py-1.5">Planned</th>
            <th className="text-right px-3 py-1.5">Paid</th>
            <th className="text-left px-3 py-1.5">Method</th>
            <th className="text-center px-3 py-1.5">Status</th>
            <th className="text-left px-3 py-1.5">Remarks</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-[#E1E8E5]">
          {rows.map((a) => (
            <tr key={a.allocationId}>
              <td className="px-3 py-1.5 font-medium">{a.beneficiary}</td>
              <td className="px-3 py-1.5">{a.school ?? ''}</td>
              <td className="px-3 py-1.5 text-right">{formatINR(a.plannedAmount)}</td>
              <td className="px-3 py-1.5 text-right font-semibold">{formatINR(a.paidAmount)}</td>
              <td className="px-3 py-1.5">{a.paymentMethod ?? ''}</td>
              <td className="px-3 py-1.5 text-center"><StatusBadge status={a.status} size="sm" /></td>
              <td className="px-3 py-1.5 text-[#5B6B67]">{a.remarks ?? ''}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export const PlanningPage: React.FC = () => {
  const { selectedYears } = useOutletContext<{ selectedYears: number[] }>();
  const { canEditPlanning } = usePermissions();
  const lookups = useLookups();
  const queryClient = useQueryClient();

  const { data = [], isLoading } = useQuery({
    queryKey: ['planning', selectedYears],
    queryFn: () => getPlanning(selectedYears),
  });

  const [expanded, setExpanded] = useState<Set<number>>(new Set());
  const [editing, setEditing] = useState<Planning | 'new' | null>(null);
  const [form, setForm] = useState<FormState>({ zakatYear: '', classificationId: '', category: '', plannedAmount: '', remarks: '' });
  const [formError, setFormError] = useState<string | null>(null);
  const [pageError, setPageError] = useState<string | null>(null);
  const [toDelete, setToDelete] = useState<Planning | null>(null);

  const totals = useMemo(
    () =>
      data.reduce(
        (t, p) => ({
          planned: t.planned + p.plannedAmount,
          paid: t.paid + p.paidAmount,
          remaining: t.remaining + p.remainingAmount,
          count: t.count + p.allocationCount,
          cy: [1, 2, 3, 4, 5].map((i, idx) => t.cy[idx] + Number(p[`cyMinus${i}` as keyof Planning] ?? 0)),
        }),
        { planned: 0, paid: 0, remaining: 0, count: 0, cy: [0, 0, 0, 0, 0] }
      ),
    [data]
  );

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['planning'] });

  const save = useMutation({
    mutationFn: () => {
      const body = {
        zakatYear: Number(form.zakatYear),
        classificationId: toNumber(form.classificationId) ?? null,
        category: form.category || undefined,
        plannedAmount: toNumber(form.plannedAmount) ?? 0,
        remarks: form.remarks || undefined,
      };
      return editing && editing !== 'new' ? updatePlanning(editing.planningId, body) : createPlanning(body);
    },
    onSuccess: () => {
      refresh();
      setEditing(null);
    },
    onError: (e) => setFormError(errorMessage(e)),
  });

  const remove = useMutation({
    mutationFn: (p: Planning) => deletePlanning(p.planningId),
    onSuccess: () => {
      refresh();
      setToDelete(null);
    },
    onError: (e) => {
      setPageError(errorMessage(e));
      setToDelete(null);
    },
  });

  const open = (p: Planning | null) => {
    setFormError(null);
    const year = selectedYears.length ? Math.max(...selectedYears) : lookups.years[0];
    setForm(
      p
        ? {
            zakatYear: String(p.zakatYear),
            classificationId: p.classificationId ? String(p.classificationId) : '',
            category: p.category ?? '',
            plannedAmount: String(p.plannedAmount),
            remarks: p.remarks ?? '',
          }
        : { zakatYear: year ? String(year) : '', classificationId: '', category: '', plannedAmount: '', remarks: '' }
    );
    setEditing(p ?? 'new');
  };

  const toggle = (id: number) =>
    setExpanded((s) => {
      const n = new Set(s);
      if (n.has(id)) n.delete(id);
      else n.add(id);
      return n;
    });

  const baseYear = data[0]?.zakatYear;
  const cyHeader = (i: number) => (baseYear && new Set(data.map((d) => d.zakatYear)).size === 1 ? String(baseYear - i) : `CY-${i}`);

  const columns: Column<Planning>[] = [
    {
      header: '',
      cell: (p) => (
        <button type="button" className={iconBtn} onClick={() => toggle(p.planningId)} title="Show allocations" aria-expanded={expanded.has(p.planningId)}>
          {expanded.has(p.planningId) ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
        </button>
      ),
    },
    { header: 'Year', cell: (p) => p.zakatYear },
    {
      header: 'Classification',
      cell: (p) => (
        <div>
          <div className="font-medium">{p.classification ?? '—'}</div>
          <div className="text-[10px] text-[#5B6B67]">{p.planType}</div>
        </div>
      ),
    },
    { header: 'Category', cell: (p) => p.category ?? '—' },
    { header: 'Planned', align: 'right', cell: (p) => formatINR(p.plannedAmount) },
    { header: 'Paid', align: 'right', cell: (p) => <span className="font-semibold">{formatINR(p.paidAmount)}</span> },
    { header: 'Remaining', align: 'right', cell: (p) => <CurrencyDisplay amount={p.remainingAmount} highlightNegative /> },
    { header: 'Allocs / Benef.', align: 'center', cell: (p) => `${p.allocationCount} / ${p.beneficiaryCount}` },
    { header: 'Status', align: 'center', cell: (p) => <StatusBadge status={p.paymentStatus} size="sm" /> },
    ...[1, 2, 3, 4, 5].map((i) => ({
      header: cyHeader(i),
      align: 'right' as const,
      className: 'text-[#5B6B67]',
      cell: (p: Planning) => formatINR(Number(p[`cyMinus${i}` as keyof Planning] ?? 0)),
    })),
  ];
  if (canEditPlanning) {
    columns.push({
      header: '',
      align: 'right',
      cell: (p) => (
        <div className="flex justify-end gap-1">
          <button type="button" className={iconBtn} title="Edit" onClick={() => open(p)}>
            <Pencil className="w-3.5 h-3.5" />
          </button>
          <button type="button" className={`${iconBtn} text-rose-600`} title="Delete" onClick={() => setToDelete(p)}>
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      ),
    });
  }

  const footer = data.length > 0 && (
    <tr className="bg-[#F6FAF8] font-bold border-t-2 border-[#C9A84C]">
      <td className="py-2.5 px-4" colSpan={4}>Total</td>
      <td className="py-2.5 px-4 text-right">{formatINR(totals.planned)}</td>
      <td className="py-2.5 px-4 text-right">{formatINR(totals.paid)}</td>
      <td className="py-2.5 px-4 text-right"><CurrencyDisplay amount={totals.remaining} highlightNegative /></td>
      <td className="py-2.5 px-4 text-center">{totals.count}</td>
      <td />
      {totals.cy.map((v, i) => (
        <td key={i} className="py-2.5 px-4 text-right">{formatINR(v)}</td>
      ))}
      {canEditPlanning && <td />}
    </tr>
  );

  return (
    <div className="space-y-4">
      <PageHeader
        title="Zakat Planning"
        subtitle="Planned amounts per classification and category. Paid figures are calculated live from allocations; CY-1…CY-5 show the same plan's paid totals in earlier years."
        actions={
          canEditPlanning && (
            <button type="button" className={btnPrimary} onClick={() => open(null)}>
              <Plus className="w-3.5 h-3.5" /> New Plan Line
            </button>
          )
        }
      />
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <KpiCard title="Planned" value={formatINR(totals.planned)} color="indigo" />
        <KpiCard title="Paid" value={formatINR(totals.paid)} color="emerald" />
        <KpiCard title="Remaining" value={formatINR(totals.remaining)} color={totals.remaining < 0 ? 'rose' : 'amber'} />
        <KpiCard title="Allocations" value={totals.count.toLocaleString('en-IN')} color="sky" />
      </div>
      <ErrorBanner message={pageError} />
      <DataTable
        columns={columns}
        data={data}
        rowKey={(p) => p.planningId}
        isLoading={isLoading}
        footerRow={footer}
        emptyMessage="No planning lines for the selected year(s)"
        renderExpanded={(p) => (expanded.has(p.planningId) ? <Drilldown planningId={p.planningId} /> : null)}
      />

      <Modal
        isOpen={editing !== null}
        title={editing === 'new' ? 'New Plan Line' : 'Edit Plan Line'}
        onClose={() => setEditing(null)}
        size="md"
        footer={
          <>
            <button type="button" className={btnSecondary} onClick={() => setEditing(null)}>Cancel</button>
            <button type="submit" form="planning-form" className={btnPrimary} disabled={save.isPending}>
              {save.isPending ? 'Saving...' : 'Save'}
            </button>
          </>
        }
      >
        <ErrorBanner message={formError} />
        <form
          id="planning-form"
          className="grid grid-cols-2 gap-3"
          onSubmit={(e) => {
            e.preventDefault();
            if (!form.zakatYear) return setFormError('Zakat year is required.');
            if (!form.classificationId || !form.category) return setFormError('Classification and category are both needed to match allocations.');
            if (toNumber(form.plannedAmount) === undefined) return setFormError('Planned amount is required.');
            save.mutate();
          }}
        >
          <Field label="Zakat Year" required>
            <Select value={form.zakatYear} onChange={(v) => setForm({ ...form, zakatYear: v })} options={lookups.years.map((y) => ({ value: y, label: String(y) }))} placeholder="— Select —" />
          </Field>
          <Field label="Planned Amount (₹)" required>
            <input className={inputClass} type="number" step="0.01" min="0" value={form.plannedAmount} onChange={(e) => setForm({ ...form, plannedAmount: e.target.value })} />
          </Field>
          <Field label="Classification" required hint="Plan type (Zakat / Sadaqah) follows the classification.">
            <Select value={form.classificationId} onChange={(v) => setForm({ ...form, classificationId: v })} options={lookups.classifications.map((c) => ({ value: c.classificationId, label: c.name }))} placeholder="— Select —" />
          </Field>
          <Field label="Category" required>
            <Select value={form.category} onChange={(v) => setForm({ ...form, category: v })} options={lookups.categories.map((c) => ({ value: c.name, label: c.name }))} placeholder="— Select —" />
          </Field>
          <Field label="Remarks" className="col-span-2">
            <textarea className={inputClass} rows={2} value={form.remarks} onChange={(e) => setForm({ ...form, remarks: e.target.value })} />
          </Field>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={toDelete !== null}
        title="Delete plan line"
        message={`Delete the ${toDelete?.zakatYear ?? ''} plan for ${toDelete?.classification ?? ''} / ${toDelete?.category ?? ''}? Allocations are not affected.`}
        confirmLabel="Delete"
        isDestructive
        isLoading={remove.isPending}
        onConfirm={() => toDelete && remove.mutate(toDelete)}
        onCancel={() => setToDelete(null)}
      />
    </div>
  );
};
