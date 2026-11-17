import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import {
  Banknote,
  Briefcase,
  ChevronDown,
  ChevronRight,
  GraduationCap,
  Handshake,
  Layers,
  LayoutDashboard,
  ListChecks,
  Tag,
  Target,
  UserCheck,
  Users,
  Wallet,
} from 'lucide-react';
import { usePermissions } from '../../hooks/usePermissions';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  onHoverOpen: () => void;
  onHoverLeave: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose, onHoverOpen, onHoverLeave }) => {
  const [isMastersOpen, setIsMastersOpen] = useState(true);
  const { canManageUsers } = usePermissions();

  const navItemClass = ({ isActive }: { isActive: boolean }) =>
    `group flex items-center gap-3 px-3 py-2.5 text-sm font-semibold rounded-lg transition-colors ${
      isActive
        ? 'bg-gradient-to-r from-[#22C55E] to-[#16A34A] text-white shadow-[0_4px_12px_rgba(34,197,94,0.24)] [&>span]:bg-white/20 [&>span]:text-white'
        : 'text-muted-foreground hover:bg-muted hover:text-foreground'
    }`;

  const subNavItemClass = ({ isActive }: { isActive: boolean }) =>
    `group flex items-center gap-2.5 px-3 py-2 text-sm rounded-lg transition-colors pl-8 ${
      isActive
        ? 'bg-primary/10 text-primary font-semibold'
        : 'text-muted-foreground hover:bg-muted hover:text-foreground'
    }`;

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          onClick={onClose}
          className="fixed inset-0 z-40 bg-black/50 lg:hidden backdrop-blur-xs"
        />
      )}

      {/* Sidebar container */}
      <aside
        onMouseEnter={onHoverOpen}
        onMouseLeave={onHoverLeave}
          className={`fixed top-[68px] bottom-0 left-0 z-40 w-64 bg-card text-card-foreground border-r border-border transition-transform duration-200 ease-in-out ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        } overflow-y-auto flex flex-col justify-between`}
      >
        <div className="p-3 space-y-1">
          <div className="px-3 py-2 text-[10px] font-bold uppercase tracking-[.14em] text-muted-foreground">
            Core Modules
          </div>

          <NavLink to="/dashboard" onClick={onClose} className={navItemClass}>
            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-muted group-hover:text-primary"><LayoutDashboard className="w-4 h-4" /></span>
            <span>Dashboard</span>
          </NavLink>

          <NavLink to="/assessments" onClick={onClose} className={navItemClass}>
            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-muted group-hover:text-primary"><Banknote className="w-4 h-4" /></span>
            <span>Zakat Amount</span>
          </NavLink>

          <NavLink to="/allocations" onClick={onClose} className={navItemClass}>
            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-muted group-hover:text-primary"><ListChecks className="w-4 h-4" /></span>
            <span>Allocations</span>
          </NavLink>

          <NavLink to="/planning" onClick={onClose} className={navItemClass}>
            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-muted group-hover:text-primary"><Target className="w-4 h-4" /></span>
            <span>Zakat Planning</span>
          </NavLink>

          <NavLink to="/beneficiaries" onClick={onClose} className={navItemClass}>
            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-muted group-hover:text-primary"><Users className="w-4 h-4" /></span>
            <span>Beneficiaries</span>
          </NavLink>

          {/* Masters Section */}
          <div className="pt-4">
            <button
              type="button"
              onClick={() => setIsMastersOpen(!isMastersOpen)}
              className="w-full flex items-center justify-between px-3 py-2 text-[10px] font-bold uppercase tracking-[.14em] text-muted-foreground hover:text-foreground transition-colors"
            >
              <span>Master Setup</span>
              {isMastersOpen ? (
                <ChevronDown className="w-3.5 h-3.5" />
              ) : (
                <ChevronRight className="w-3.5 h-3.5" />
              )}
            </button>

            {isMastersOpen && (
              <div className="space-y-0.5 mt-1">
                <NavLink to="/masters/classifications" onClick={onClose} className={subNavItemClass}>
                  <Tag className="w-3.5 h-3.5 group-hover:text-primary" />
                  <span>Classifications</span>
                </NavLink>

                <NavLink to="/masters/categories" onClick={onClose} className={subNavItemClass}>
                  <Layers className="w-3.5 h-3.5 group-hover:text-primary" />
                  <span>Categories</span>
                </NavLink>

                <NavLink to="/masters/source-accounts" onClick={onClose} className={subNavItemClass}>
                  <Wallet className="w-3.5 h-3.5 group-hover:text-primary" />
                  <span>Source Accounts</span>
                </NavLink>

                <NavLink to="/masters/schools" onClick={onClose} className={subNavItemClass}>
                  <GraduationCap className="w-3.5 h-3.5 group-hover:text-primary" />
                  <span>Schools & Institutes</span>
                </NavLink>

                <NavLink to="/masters/agents" onClick={onClose} className={subNavItemClass}>
                  <Handshake className="w-3.5 h-3.5 group-hover:text-primary" />
                  <span>Agents</span>
                </NavLink>

                <NavLink to="/masters/employees" onClick={onClose} className={subNavItemClass}>
                  <Briefcase className="w-3.5 h-3.5 group-hover:text-primary" />
                  <span>Employees</span>
                </NavLink>

                {canManageUsers && (
                  <NavLink to="/masters/users" onClick={onClose} className={subNavItemClass}>
                    <UserCheck className="w-3.5 h-3.5 group-hover:text-primary" />
                    <span>Users & Roles</span>
                  </NavLink>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Footer info in sidebar */}
        <div className="border-t border-border bg-muted/50 p-4 text-[11px] text-muted-foreground">
          <div className="flex items-center justify-between gap-2">
            <span className="flex items-center gap-2 truncate font-medium">
              <span className="relative flex h-2 w-2 shrink-0">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-green-400 opacity-60" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-green-500" />
              </span>
              Session active
            </span>
            <span className="font-mono text-muted-foreground">v1.0</span>
          </div>
        </div>
      </aside>
    </>
  );
};
