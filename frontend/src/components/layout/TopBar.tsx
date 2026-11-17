import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, Camera, KeyRound, LogOut, Mail, Menu, Phone, Shield, Trash2, User } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { YearFilter } from '../shared/YearFilter';
import { Modal } from '../shared/Modal';
import { changePassword, getCurrentUser, updateProfilePicture } from '../../api/auth';
import { errorMessage } from '../../api/client';
import { ThemeSelector } from './ThemeSelector';

interface TopBarProps {
  onToggleSidebar: () => void;
  onSidebarHover: () => void;
  onSidebarLeave: () => void;
  sidebarOpen: boolean;
  selectedYears: number[];
  onYearChange: (years: number[]) => void;
}

export const TopBar: React.FC<TopBarProps> = ({
  onToggleSidebar,
  onSidebarHover,
  onSidebarLeave,
  sidebarOpen,
  selectedYears,
  onYearChange,
}) => {
  const { user, logout } = useAuth();
  const queryClient = useQueryClient();
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [profilePhotoDraft, setProfilePhotoDraft] = useState<string | null | undefined>(undefined);
  const [profilePhotoError, setProfilePhotoError] = useState<string | null>(null);
  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: getCurrentUser,
    enabled: showProfile,
  });
  const profilePhotoMutation = useMutation({
    mutationFn: updateProfilePicture,
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(['auth', 'me'], updatedProfile);
      setProfilePhotoDraft(undefined);
      setProfilePhotoError(null);
    },
    onError: (err) => setProfilePhotoError(errorMessage(err, 'Could not save your profile photo.')),
  });
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleOpenPasswordModal = () => {
    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setError(null);
    setSuccess(null);
    setShowPasswordModal(true);
  };

  const handleProfilePhotoSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
      setProfilePhotoError('Choose a PNG, JPEG, or WebP image.');
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setProfilePhotoError('The image must be 2 MB or smaller.');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setProfilePhotoDraft(reader.result);
        setProfilePhotoError(null);
      }
    };
    reader.onerror = () => setProfilePhotoError('Could not read this image. Please choose another file.');
    reader.readAsDataURL(file);
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (newPassword.length < 8) {
      setError('New password must be at least 8 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match.');
      return;
    }

    setLoading(true);
    try {
      const resp = await changePassword({ currentPassword, newPassword });
      setSuccess(resp.message || 'Password updated successfully.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setTimeout(() => {
        setShowPasswordModal(false);
      }, 1200);
    } catch (err) {
      setError(errorMessage(err, 'Failed to update password.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <header className="sticky top-0 z-30 flex h-[68px] items-center justify-between gap-1 border-b border-border bg-card px-2 text-foreground shadow-sm sm:px-4 md:px-6">
        <div className="flex min-w-0 items-center gap-2 sm:gap-4">
          <button
            type="button"
            onClick={onToggleSidebar}
            onMouseEnter={onSidebarHover}
            onMouseLeave={onSidebarLeave}
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            aria-label="Toggle navigation"
            aria-expanded={sidebarOpen}
          >
            <Menu className="w-5 h-5" />
          </button>

          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#22C55E] to-[#16A34A] text-white shadow-sm shadow-green-500/30 sm:h-9 sm:w-9" aria-hidden="true">
              <Building2 className="w-4 h-4" strokeWidth={2.5} />
            </div>
            <div className="max-[400px]:hidden">
              <h1 className="max-w-[90px] truncate text-xs font-black tracking-tight text-primary leading-none sm:max-w-none sm:text-base">
                Zakat Management System
              </h1>
              <p className="text-[10px] text-muted-foreground hidden sm:block mt-0.5 tracking-wider uppercase font-semibold">
                Financial Administration
              </p>
            </div>
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-1 sm:gap-2">
          {/* Global Year Filter */}
          <YearFilter selectedYears={selectedYears} onChange={onYearChange} />

          <ThemeSelector />

          {/* User Info & Actions */}
          {user && (
            <div className="flex items-center gap-1 sm:gap-2 pl-1.5 sm:pl-3 border-l border-border">
              <button type="button" onClick={() => { setProfilePhotoDraft(undefined); setProfilePhotoError(null); setShowProfile(true); }} className="flex min-h-10 min-w-10 items-center justify-center rounded-lg px-1 hover:opacity-80 focus:outline-none focus:ring-1 focus:ring-ring md:flex-col md:items-end" aria-label="Open user profile">
                <span className="hidden md:flex text-xs font-semibold text-foreground items-center gap-1">
                  <User className="w-3 h-3 text-primary" />
                  {user.username}
                </span>
                <User className="w-4 h-4 text-primary md:hidden" />
                <span className="hidden md:inline text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 bg-primary/10 text-primary rounded-full border border-primary/30">
                  {user.role}
                </span>
              </button>

              <button
                type="button"
                onClick={handleOpenPasswordModal}
                className="hidden h-10 w-10 items-center justify-center rounded-xl text-muted-foreground transition-colors hover:bg-muted hover:text-foreground sm:flex"
                title="Change Password"
                aria-label="Change Password"
              >
                <KeyRound className="w-4 h-4" />
              </button>

              <button
                type="button"
                onClick={logout}
                className="flex h-10 w-10 items-center justify-center rounded-xl text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                title="Sign Out"
                aria-label="Sign Out"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      </header>

      {/* Change Password Modal */}
      <Modal
        isOpen={showProfile}
        title="User Profile"
        onClose={() => setShowProfile(false)}
        size="md"
      >
        {profileLoading ? (
          <p className="text-sm text-gray-500">Loading profile…</p>
        ) : profile ? (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-3 border-b border-border pb-4">
              <div className="flex h-14 w-14 shrink-0 items-center justify-center overflow-hidden rounded-full bg-emerald-100 text-lg font-bold text-emerald-800 dark:bg-[#0d2818] dark:text-emerald-400">
                {(profilePhotoDraft === undefined ? profile.profilePicture : profilePhotoDraft) ? (
                  <img src={profilePhotoDraft === undefined ? profile.profilePicture || undefined : profilePhotoDraft || undefined} alt={`${profile.fullName} profile`} className="h-full w-full object-cover" />
                ) : (profile.fullName?.charAt(0)?.toUpperCase() || profile.username.charAt(0).toUpperCase())}
              </div>
              <div className="min-w-0 flex-1"><p className="font-semibold text-gray-900">{profile.fullName}</p><p className="text-xs text-gray-500">@{profile.username}</p></div>
              <div className="flex items-center gap-2">
                <label className="inline-flex min-h-10 cursor-pointer items-center gap-2 rounded-lg border border-border bg-secondary px-3 text-sm font-semibold text-secondary-foreground hover:bg-muted">
                  <Camera className="h-4 w-4" /> {profile.profilePicture || profilePhotoDraft ? 'Change photo' : 'Add photo'}
                  <input type="file" accept="image/png,image/jpeg,image/webp" onChange={handleProfilePhotoSelect} className="sr-only" aria-label="Choose profile photo" />
                </label>
                {(profile.profilePicture || (profilePhotoDraft !== undefined && profilePhotoDraft !== null)) && (
                  <button type="button" onClick={() => { setProfilePhotoDraft(null); setProfilePhotoError(null); }} disabled={profilePhotoMutation.isPending} className="flex h-10 w-10 items-center justify-center rounded-lg border border-border text-red-600 hover:bg-red-500/10 disabled:opacity-50" aria-label="Remove profile photo" title="Remove photo">
                    <Trash2 className="h-4 w-4" />
                  </button>
                )}
              </div>
              <p className="w-full pl-[68px] text-xs text-muted-foreground">PNG, JPEG, or WebP · up to 2 MB</p>
              {profilePhotoError && <p role="alert" className="w-full pl-[68px] text-xs text-red-700 dark:text-red-400">{profilePhotoError}</p>}
              {profilePhotoDraft !== undefined && (
                <button type="button" onClick={() => profilePhotoMutation.mutate(profilePhotoDraft)} disabled={profilePhotoMutation.isPending} className="ml-[68px] inline-flex min-h-10 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-semibold text-primary-foreground hover:bg-green-600 disabled:cursor-not-allowed disabled:opacity-60 dark:hover:bg-green-400">
                  {profilePhotoMutation.isPending ? 'Saving…' : profilePhotoDraft ? 'Save photo' : 'Remove photo'}
                </button>
              )}
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div className="rounded border border-gray-200 p-3"><p className="text-xs text-gray-500">Role</p><p className="mt-1 flex items-center gap-1 font-medium text-gray-900"><Shield className="h-3.5 w-3.5" />{profile.role}</p></div>
              <div className="rounded border border-gray-200 p-3"><p className="text-xs text-gray-500">Status</p><p className="mt-1 font-medium text-gray-900">{profile.isActive === 'Y' ? 'Active' : 'Inactive'}</p></div>
              <div className="rounded border border-gray-200 p-3"><p className="text-xs text-gray-500">Email</p><p className="mt-1 flex items-center gap-1 font-medium text-gray-900 break-all"><Mail className="h-3.5 w-3.5 shrink-0" />{profile.email || 'Not added'}</p></div>
              <div className="rounded border border-gray-200 p-3"><p className="text-xs text-gray-500">Phone</p><p className="mt-1 flex items-center gap-1 font-medium text-gray-900"><Phone className="h-3.5 w-3.5" />{profile.phone || 'Not added'}</p></div>
              <div className="rounded border border-gray-200 p-3 col-span-2"><p className="text-xs text-gray-500">Account created</p><p className="mt-1 font-medium text-gray-900">{profile.createdOn ? new Date(profile.createdOn).toLocaleString() : '—'}</p></div>
            </div>
            <p className="text-xs text-gray-500">To update your contact details, ask an administrator to edit your user account.</p>
            <button type="button" onClick={() => { setShowProfile(false); handleOpenPasswordModal(); }} className="inline-flex min-h-10 items-center gap-2 rounded-lg border border-border bg-secondary px-4 text-sm font-semibold text-secondary-foreground hover:bg-muted">
              <KeyRound className="h-4 w-4" /> Change password
            </button>
          </div>
        ) : (
          <p className="text-sm text-rose-600">Could not load your profile. Please try again.</p>
        )}
      </Modal>

      <Modal
        isOpen={showPasswordModal}
        title="Change Password"
        onClose={() => !loading && setShowPasswordModal(false)}
        size="md"
        footer={
          <>
            <button
              type="button"
              disabled={loading}
              onClick={() => setShowPasswordModal(false)}
              className="px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-100 rounded border border-gray-300 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              form="change-password-form"
              disabled={loading}
              className="px-4 py-1.5 text-xs font-bold text-white bg-[#0B4A3F] hover:bg-[#0F766E] rounded shadow-sm transition-colors flex items-center gap-1.5 disabled:opacity-50"
            >
              {loading && <span className="inline-block w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin" />}
              Update Password
            </button>
          </>
        }
      >
        <form id="change-password-form" onSubmit={handlePasswordSubmit} className="space-y-4">
          {error && (
            <div className="p-2.5 rounded bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium">
              {error}
            </div>
          )}
          {success && (
            <div className="p-2.5 rounded bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs font-medium">
              {success}
            </div>
          )}

          <div className="space-y-1">
            <label className="text-xs font-semibold text-gray-700">Current Password</label>
            <input
              type="password"
              required
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              className="w-full px-3 py-1.5 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-[#0B4A3F]"
              placeholder="Enter current password"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-gray-700">New Password (minimum 8 characters)</label>
            <input
              type="password"
              required
              minLength={8}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full px-3 py-1.5 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-[#0B4A3F]"
              placeholder="Enter new password"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-gray-700">Confirm New Password</label>
            <input
              type="password"
              required
              minLength={8}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="w-full px-3 py-1.5 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-[#0B4A3F]"
              placeholder="Confirm new password"
            />
          </div>
        </form>
      </Modal>
    </>
  );
};
