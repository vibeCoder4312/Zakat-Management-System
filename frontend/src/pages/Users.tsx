import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Eye, EyeOff, Pencil, Plus, Trash2, Shield } from 'lucide-react';
import { PageHeader } from '../components/layout/PageHeader';
import { Column, DataTable } from '../components/shared/DataTable';
import { Modal } from '../components/shared/Modal';
import { ConfirmDialog } from '../components/shared/ConfirmDialog';
import { StatusBadge } from '../components/shared/StatusBadge';
import { createUser, deleteUser, getUsers, updateUser } from '../api/users';
import { errorMessage } from '../api/client';
import { useAuth } from '../hooks/useAuth';
import { usePermissions } from '../hooks/usePermissions';
import type { AppUser, CreateUserRequest, UpdateUserRequest, UserRole, YesNo } from '../types';

interface FormState {
  username: string;
  fullName: string;
  email: string;
  phone: string;
  role: UserRole;
  isActive: YesNo;
  password: string;
}

const emptyForm: FormState = {
  username: '',
  fullName: '',
  email: '',
  phone: '',
  role: 'VIEWER',
  isActive: 'Y',
  password: '',
};

export const Users: React.FC = () => {
  const { user: currentUser } = useAuth();
  const { canManageUsers } = usePermissions();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [searchQuery, setSearchQuery] = useState('');

  const [editing, setEditing] = useState<AppUser | 'new' | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const [pageError, setPageError] = useState<string | null>(null);
  const [toDelete, setToDelete] = useState<AppUser | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['users', page, pageSize, searchQuery],
    queryFn: () => getUsers({ page, size: pageSize, q: searchQuery }),
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['users'] });
    queryClient.invalidateQueries({ queryKey: ['auth', 'me'] });
  };

  const openCreateModal = () => {
    setForm(emptyForm);
    setShowPassword(false);
    setFormError(null);
    setEditing('new');
  };

  const openEditModal = (u: AppUser) => {
    setShowPassword(false);
    setForm({
      username: u.username,
      fullName: u.fullName,
      email: u.email || '',
      phone: u.phone || '',
      role: u.role,
      isActive: u.isActive,
      password: '',
    });
    setFormError(null);
    setEditing(u);
  };

  const saveMutation = useMutation({
    mutationFn: () => {
      if (editing === 'new') {
        const body: CreateUserRequest = {
          username: form.username.trim(),
          fullName: form.fullName.trim(),
          email: form.email.trim() || null,
          phone: form.phone.trim() || null,
          role: form.role,
          isActive: form.isActive,
          password: form.password,
        };
        return createUser(body);
      } else if (editing) {
        const body: UpdateUserRequest = {
          fullName: form.fullName.trim(),
          email: form.email.trim() || null,
          phone: form.phone.trim() || null,
          role: form.role,
          isActive: form.isActive,
          newPassword: form.password ? form.password : undefined,
        };
        return updateUser(editing.userId, body);
      }
      return Promise.reject(new Error('Invalid state'));
    },
    onSuccess: () => {
      refresh();
      setEditing(null);
    },
    onError: (err) => {
      setFormError(errorMessage(err, 'Failed to save user.'));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (u: AppUser) => deleteUser(u.userId),
    onSuccess: () => {
      refresh();
      setToDelete(null);
    },
    onError: (err) => {
      setPageError(errorMessage(err, 'Failed to delete user.'));
    },
  });

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (editing === 'new') {
      if (!form.username.trim()) {
        setFormError('Username is required.');
        return;
      }
      if (!form.password || form.password.length < 8) {
        setFormError('Password must be at least 8 characters long.');
        return;
      }
    } else {
      if (form.password && form.password.length < 8) {
        setFormError('New password must be at least 8 characters long.');
        return;
      }
    }

    if (!form.fullName.trim()) {
      setFormError('Full name is required.');
      return;
    }

    saveMutation.mutate();
  };

  const columns: Column<AppUser>[] = [
    {
      header: 'Username',
      accessorKey: 'username',
      cell: (row) => (
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-full bg-[#E1E8E5] text-[#0B4A3F] flex items-center justify-center font-bold text-xs">
            {row.username.charAt(0).toUpperCase()}
          </div>
          <div>
            <span className="font-bold text-[#0B4A3F] block leading-tight">{row.username}</span>
            {currentUser?.username.toLowerCase() === row.username.toLowerCase() && (
              <span className="text-[10px] text-emerald-700 font-semibold">(Current User)</span>
            )}
          </div>
        </div>
      ),
    },
    {
      header: 'Full Name',
      accessorKey: 'fullName',
      cell: (row) => <span className="font-medium text-gray-800">{row.fullName}</span>,
    },
    {
      header: 'Role',
      accessorKey: 'role',
      cell: (row) => {
        const isAdm = row.role === 'ADMIN';
        const isAcct = row.role === 'ACCOUNTANT';
        const color = isAdm
          ? 'bg-purple-100 text-purple-800 border-purple-200'
          : isAcct
          ? 'bg-blue-100 text-blue-800 border-blue-200'
          : 'bg-gray-100 text-gray-700 border-gray-200';
        return (
          <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold border ${color}`}>
            <Shield className="w-3 h-3" />
            {row.role}
          </span>
        );
      },
    },
    {
      header: 'Status',
      accessorKey: 'isActive',
      cell: (row) => <StatusBadge status={row.isActive === 'Y' ? 'ACTIVE' : 'INACTIVE'} size="sm" />,
    },
    {
      header: 'Updated By / On',
      cell: (row) => (
        <div className="text-xs text-gray-500">
          <div>{row.updatedBy || row.createdBy || 'SYSTEM'}</div>
          <div className="text-[10px] text-gray-400">
            {row.updatedOn ? new Date(row.updatedOn).toLocaleString() : '-'}
          </div>
        </div>
      ),
    },
    {
      header: 'Actions',
      align: 'right',
      cell: (row) => {
        const isSelf = currentUser?.username.toLowerCase() === row.username.toLowerCase();
        return (
          <div className="flex items-center justify-end gap-1.5">
            {canManageUsers && (
              <button
                type="button"
                onClick={() => openEditModal(row)}
                className="p-1 text-gray-500 hover:text-[#0B4A3F] hover:bg-gray-100 rounded transition-colors"
                title="Edit User"
              >
                <Pencil className="w-4 h-4" />
              </button>
            )}
            {canManageUsers && !isSelf && (
              <button
                type="button"
                onClick={() => setToDelete(row)}
                className="p-1 text-gray-400 hover:text-rose-600 hover:bg-rose-50 rounded transition-colors"
                title="Delete User"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <div className="space-y-4">
      <PageHeader
        title="Users & Roles"
        subtitle="User Accounts & Role Permissions"
        actions={
          canManageUsers ? (
            <button
              type="button"
              onClick={openCreateModal}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-[#0B4A3F] hover:bg-[#0F766E] text-white text-xs font-bold rounded shadow transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              Add User
            </button>
          ) : undefined
        }
      />

      {pageError && (
        <div className="p-3 rounded bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium flex items-center justify-between">
          <span>{pageError}</span>
          <button type="button" onClick={() => setPageError(null)} className="text-rose-500 hover:text-rose-800 text-xs font-bold">
            Dismiss
          </button>
        </div>
      )}

      <DataTable<AppUser>
        columns={columns}
        data={data?.content || []}
        totalElements={data?.totalElements}
        page={page}
        pageSize={pageSize}
        totalPages={data?.totalPages}
        isLoading={isLoading}
        searchQuery={searchQuery}
        onSearchChange={(q) => {
          setSearchQuery(q);
          setPage(0);
        }}
        searchPlaceholder="Search by username, full name, or role..."
        onPageChange={setPage}
        onPageSizeChange={(newSize) => {
          setPageSize(newSize);
          setPage(0);
        }}
        emptyMessage="No application users found."
        rowKey={(row) => row.userId}
      />

      {/* Add / Edit User Modal */}
      <Modal
        isOpen={editing !== null}
        title={editing === 'new' ? 'Create New User' : `Edit User: ${editing?.username}`}
        onClose={() => !saveMutation.isPending && setEditing(null)}
        size="md"
        footer={
          <>
            <button
              type="button"
              disabled={saveMutation.isPending}
              onClick={() => setEditing(null)}
              className="px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-100 rounded border border-gray-300 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              form="user-form"
              disabled={saveMutation.isPending}
              className="px-4 py-1.5 text-xs font-bold text-white bg-[#0B4A3F] hover:bg-[#0F766E] rounded shadow-sm transition-colors flex items-center gap-1.5 disabled:opacity-50"
            >
              {saveMutation.isPending && <span className="inline-block w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin" />}
              {editing === 'new' ? 'Create User' : 'Save Changes'}
            </button>
          </>
        }
      >
        <form id="user-form" onSubmit={handleFormSubmit} className="space-y-4">
          {formError && (
            <div className="p-2.5 rounded bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium">
              {formError}
            </div>
          )}

          <div className="space-y-1">
            <label className="text-xs font-semibold text-gray-700">Username</label>
            <input
              type="text"
              required
              disabled={editing !== 'new'}
              value={form.username}
              onChange={(e) => setForm((prev) => ({ ...prev, username: e.target.value }))}
              className="w-full px-3 py-1.5 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-[#0B4A3F] disabled:bg-gray-100 disabled:text-gray-500"
              placeholder="e.g. john_doe"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-gray-700">Full Name</label>
            <input
              type="text"
              required
              value={form.fullName}
              onChange={(e) => setForm((prev) => ({ ...prev, fullName: e.target.value }))}
              className="w-full px-3 py-1.5 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-[#0B4A3F]"
              placeholder="e.g. John Doe"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-gray-700">Email</label>
              <input type="email" value={form.email} onChange={(e) => setForm((prev) => ({ ...prev, email: e.target.value }))} className="w-full px-3 py-1.5 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-[#0B4A3F]" placeholder="name@example.com" />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold text-gray-700">Phone</label>
              <input type="tel" value={form.phone} onChange={(e) => setForm((prev) => ({ ...prev, phone: e.target.value }))} className="w-full px-3 py-1.5 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-[#0B4A3F]" placeholder="Phone number" />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-gray-700">Role</label>
              <select
                value={form.role}
                onChange={(e) => setForm((prev) => ({ ...prev, role: e.target.value as UserRole }))}
                className="w-full px-3 py-1.5 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-[#0B4A3F]"
              >
                <option value="ADMIN">ADMIN</option>
                <option value="ACCOUNTANT">ACCOUNTANT</option>
                <option value="VIEWER">VIEWER</option>
              </select>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-gray-700">Active Status</label>
              <select
                value={form.isActive}
                onChange={(e) => setForm((prev) => ({ ...prev, isActive: e.target.value as YesNo }))}
                className="w-full px-3 py-1.5 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-[#0B4A3F]"
              >
                <option value="Y">Active (Y)</option>
                <option value="N">Inactive (N)</option>
              </select>
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-gray-700">
              {editing === 'new' ? 'Initial Password (min 8 chars)' : 'Reset Password (leave blank to keep current)'}
            </label>
            <div className="relative">
            <input
              type={showPassword ? 'text' : 'password'}
              minLength={8}
              required={editing === 'new'}
              value={form.password}
              onChange={(e) => setForm((prev) => ({ ...prev, password: e.target.value }))}
              className="w-full px-3 py-1.5 pr-9 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-[#0B4A3F]"
              placeholder={editing === 'new' ? 'Enter minimum 8 characters' : 'Enter new password to reset'}
            />
            <button type="button" onClick={() => setShowPassword((visible) => !visible)} className="absolute inset-y-0 right-0 px-2.5 text-gray-500 hover:text-[#0B4A3F]" aria-label={showPassword ? 'Hide password' : 'Show password'} title={showPassword ? 'Hide password' : 'Show password'}>
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
            </div>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={toDelete !== null}
        title="Delete User Account"
        message={`Are you sure you want to delete user "${toDelete?.username}" (${toDelete?.fullName})? This action cannot be undone.`}
        confirmLabel="Delete User"
        isDestructive={true}
        isLoading={deleteMutation.isPending}
        onConfirm={() => toDelete && deleteMutation.mutate(toDelete)}
        onCancel={() => setToDelete(null)}
      />
    </div>
  );
};
