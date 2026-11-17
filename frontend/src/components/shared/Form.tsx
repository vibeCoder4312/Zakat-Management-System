import React from 'react';

/** Shared form primitives so every screen looks and behaves the same. */

export const inputClass =
  'w-full h-11 px-3 text-sm bg-card text-foreground border border-input rounded-lg placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-primary/30 focus-visible:border-primary disabled:bg-muted disabled:text-muted-foreground';

export const btnPrimary =
  'inline-flex min-h-10 items-center gap-1.5 px-4 py-2 text-sm font-semibold rounded-lg bg-primary text-primary-foreground hover:bg-green-600 dark:hover:bg-green-400 disabled:opacity-60 disabled:cursor-not-allowed transition-colors active:scale-[0.98]';
export const btnSecondary =
  'inline-flex min-h-10 items-center gap-1.5 px-4 py-2 text-sm font-semibold rounded-lg bg-secondary text-secondary-foreground border border-border hover:bg-accent disabled:opacity-60 disabled:cursor-not-allowed transition-colors';
export const btnDanger =
  'inline-flex min-h-10 items-center gap-1.5 px-4 py-2 text-sm font-semibold rounded-lg bg-red-500/10 text-red-700 hover:bg-red-500/20 dark:text-red-400 disabled:opacity-60 transition-colors';
export const iconBtn = 'flex h-10 w-10 items-center justify-center rounded-lg hover:bg-muted text-primary disabled:opacity-40';

interface FieldProps {
  label: string;
  required?: boolean;
  hint?: string;
  className?: string;
  children: React.ReactNode;
}

export const Field: React.FC<FieldProps> = ({ label, required, hint, className = '', children }) => (
  <label className={`block ${className}`}>
    <span className="mb-1 block text-xs font-semibold text-muted-foreground">
      {label}
      {required && <span className="text-rose-600"> *</span>}
    </span>
    {children}
    {hint && <span className="mt-1 block text-xs text-muted-foreground">{hint}</span>}
  </label>
);

export interface Option {
  value: string | number;
  label: string;
}

interface SelectProps extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'onChange' | 'value'> {
  value: string | number | null | undefined;
  onChange: (value: string) => void;
  options: Option[];
  placeholder?: string;
}

export const Select: React.FC<SelectProps> = ({ value, onChange, options, placeholder, className = '', ...rest }) => (
  <select value={value ?? ''} onChange={(e) => onChange(e.target.value)} className={`${inputClass} ${className}`} {...rest}>
    {placeholder !== undefined && <option value="">{placeholder}</option>}
    {options.map((o) => (
      <option key={o.value} value={o.value}>
        {o.label}
      </option>
    ))}
  </select>
);

export const ErrorBanner: React.FC<{ message?: string | null }> = ({ message }) =>
  message ? (
    <div role="alert" className="mb-4 flex items-start gap-2 rounded-lg border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-500/30 dark:bg-[#2d1214] dark:text-red-400">
      {message}
    </div>
  ) : null;

/** Converts an input string into a number, or undefined when blank. */
export const toNumber = (v: string): number | undefined => (v.trim() === '' || isNaN(Number(v)) ? undefined : Number(v));

export const YES_NO: Option[] = [
  { value: 'Y', label: 'Yes' },
  { value: 'N', label: 'No' },
];
