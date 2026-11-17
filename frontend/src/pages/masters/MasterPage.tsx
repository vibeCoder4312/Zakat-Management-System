import React, { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, Trash2 } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Column, DataTable } from '../../components/shared/DataTable';
import { Modal } from '../../components/shared/Modal';
import { ConfirmDialog } from '../../components/shared/ConfirmDialog';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { ErrorBanner, Field, Option, Select, btnPrimary, btnSecondary, iconBtn, inputClass } from '../../components/shared/Form';
import { createMaster, deleteMaster, listMaster, MasterResource, updateMaster } from '../../api/masters';
import { errorMessage } from '../../api/client';
import { usePermissions } from '../../hooks/usePermissions';

export interface MasterField {
  key: string;
  label: string;
  type?: 'text' | 'number' | 'select';
  options?: Option[];
  required?: boolean;
  defaultValue?: string | number;
  hint?: string;
  /** Shown as a table column (default true). */
  inTable?: boolean;
  render?: (value: unknown) => React.ReactNode;
}

export interface MasterConfig {
  resource: MasterResource;
  title: string;
  subtitle: string;
  singular: string;
  idKey: string;
  fields: MasterField[];
  /** Extra read-only columns, e.g. computed counts. */
  extraColumns?: { key: string; header: string; align?: 'left' | 'right' | 'center' }[];
}

type Row = Record<string, unknown>;

const activeField: MasterField = {
  key: 'isActive',
  label: 'Active',
  type: 'select',
  options: [
    { value: 'Y', label: 'Yes' },
    { value: 'N', label: 'No' },
  ],
  defaultValue: 'Y',
  render: (v) => <StatusBadge status={v === 'Y' ? 'ACTIVE' : 'INACTIVE'} size="sm" />,
};

/** One screen implementation shared by all six master lists (APEX "Masters" menu). */
export const MasterPage: React.FC<{ config: MasterConfig }> = ({ config }) => {
  const { resource, idKey } = config;
  const fields = useMemo(() => [...config.fields, activeField], [config.fields]);
  const { canEditMasters } = usePermissions();
  const queryClient = useQueryClient();

  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<Row | null>(null);
  const [isNew, setIsNew] = useState(false);
  const [form, setForm] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [toDelete, setToDelete] = useState<Row | null>(null);
  const [pageError, setPageError] = useState<string | null>(null);

  const { data = [], isLoading } = useQuery({
    queryKey: ['master', resource],
    queryFn: () => listMaster<Row>(resource),
  });

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return data;
    return data.filter((r) => fields.some((f) => String(r[f.key] ?? '').toLowerCase().includes(q)));
  }, [data, search, fields]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['master', resource] });
    queryClient.invalidateQueries({ queryKey: ['lookups'] });
  };

  const save = useMutation({
    mutationFn: (body: Record<string, unknown>) =>
      isNew ? createMaster(resource, body) : updateMaster(resource, Number(editing?.[idKey]), body),
    onSuccess: () => {
      invalidate();
      setEditing(null);
    },
    onError: (e) => setFormError(errorMessage(e)),
  });

  const remove = useMutation({
    mutationFn: (row: Row) => deleteMaster(resource, Number(row[idKey])),
    onSuccess: () => {
      invalidate();
      setToDelete(null);
    },
    onError: (e) => {
      setPageError(errorMessage(e));
      setToDelete(null);
    },
  });

  const openForm = (row: Row | null) => {
    const initial: Record<string, string> = {};
    fields.forEach((f) => {
      const v = row ? row[f.key] : f.defaultValue;
      initial[f.key] = v === null || v === undefined ? '' : String(v);
    });
    setForm(initial);
    setFormError(null);
    setIsNew(!row);
    setEditing(row ?? {});
  };

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const missing = fields.filter((f) => f.required && !form[f.key]?.trim());
    if (missing.length) {
      setFormError(`${missing.map((f) => f.label).join(', ')} is required.`);
      return;
    }
    const body: Record<string, unknown> = {};
    fields.forEach((f) => {
      const v = form[f.key]?.trim() ?? '';
      body[f.key] = v === '' ? null : f.type === 'number' ? Number(v) : v;
    });
    save.mutate(body);
  };

  const columns: Column<Row>[] = [
    ...fields
      .filter((f) => f.inTable !== false)
      .map((f) => ({
        header: f.label,
        cell: (r: Row) => (f.render ? f.render(r[f.key]) : (r[f.key] as React.ReactNode) ?? '—'),
        className: f.key === 'name' ? 'font-medium' : '',
      })),
    ...(config.extraColumns ?? []).map((c) => ({
      header: c.header,
      align: c.align,
      cell: (r: Row) => String(r[c.key] ?? 0),
    })),
  ];
  if (canEditMasters) {
    columns.push({
      header: '',
      align: 'right',
      cell: (r: Row) => (
        <div className="flex justify-end gap-1">
          <button type="button" className={iconBtn} title="Edit" onClick={() => openForm(r)}>
            <Pencil className="w-3.5 h-3.5" />
          </button>
          <button type="button" className={`${iconBtn} text-rose-600`} title="Delete" onClick={() => setToDelete(r)}>
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      ),
    });
  }

  return (
    <div className="space-y-4">
      <PageHeader
        title={config.title}
        subtitle={config.subtitle}
        badge={`${data.length} records`}
        actions={
          canEditMasters && (
            <button type="button" className={btnPrimary} onClick={() => openForm(null)}>
              <Plus className="w-3.5 h-3.5" /> New {config.singular}
            </button>
          )
        }
      />
      <ErrorBanner message={pageError} />
      <DataTable
        columns={columns}
        data={filtered}
        rowKey={(r) => String(r[idKey])}
        isLoading={isLoading}
        searchQuery={search}
        onSearchChange={setSearch}
        searchPlaceholder={`Search ${config.title.toLowerCase()}...`}
      />

      <Modal
        isOpen={editing !== null}
        title={isNew ? `New ${config.singular}` : `Edit ${config.singular}`}
        onClose={() => setEditing(null)}
        size="md"
        footer={
          <>
            <button type="button" className={btnSecondary} onClick={() => setEditing(null)}>
              Cancel
            </button>
            <button type="submit" form="master-form" className={btnPrimary} disabled={save.isPending}>
              {save.isPending ? 'Saving...' : 'Save'}
            </button>
          </>
        }
      >
        <ErrorBanner message={formError} />
        <form id="master-form" onSubmit={submit} className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {fields.map((f) => (
            <Field key={f.key} label={f.label} required={f.required} hint={f.hint} className={f.key === 'name' || f.key === 'remarks' ? 'sm:col-span-2' : ''}>
              {f.type === 'select' ? (
                <Select value={form[f.key]} onChange={(v) => setForm({ ...form, [f.key]: v })} options={f.options ?? []} />
              ) : (
                <input
                  className={inputClass}
                  type={f.type === 'number' ? 'number' : 'text'}
                  value={form[f.key] ?? ''}
                  onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                />
              )}
            </Field>
          ))}
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={toDelete !== null}
        title={`Delete ${config.singular}`}
        message={`Delete "${String(toDelete?.name ?? '')}"? Records still used elsewhere cannot be deleted; deactivate them instead.`}
        confirmLabel="Delete"
        isDestructive
        isLoading={remove.isPending}
        onConfirm={() => toDelete && remove.mutate(toDelete)}
        onCancel={() => setToDelete(null)}
      />
    </div>
  );
};
