import React from 'react';
import { formatINR } from '../../lib/formatCurrency';

interface KpiCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon?: React.ReactNode;
  trend?: string;
  color?: 'emerald' | 'amber' | 'indigo' | 'violet' | 'sky' | 'rose' | 'default';
  isCurrency?: boolean;
  compact?: boolean;
}

const colorMap = {
  emerald: {
    border: 'border-l-4 border-l-emerald-500',
    bg: 'bg-emerald-50 text-emerald-700',
  },
  amber: {
    border: 'border-l-4 border-l-amber-500',
    bg: 'bg-amber-50 text-amber-700',
  },
  indigo: {
    border: 'border-l-4 border-l-indigo-500',
    bg: 'bg-indigo-50 text-indigo-700',
  },
  violet: {
    border: 'border-l-4 border-l-purple-500',
    bg: 'bg-purple-50 text-purple-700',
  },
  sky: {
    border: 'border-l-4 border-l-sky-500',
    bg: 'bg-sky-50 text-sky-700',
  },
  rose: {
    border: 'border-l-4 border-l-rose-500',
    bg: 'bg-rose-50 text-rose-700',
  },
  default: {
    border: 'border-l-4 border-l-[#0B4A3F]',
    bg: 'bg-[#0B4A3F]/10 text-[#0B4A3F]',
  },
};

export const KpiCard: React.FC<KpiCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  trend,
  color = 'default',
  isCurrency = false,
  compact = false,
}) => {
  const colorStyles = colorMap[color] || colorMap.default;

  const displayValue =
    typeof value === 'number'
      ? isCurrency
        ? formatINR(value)
        : new Intl.NumberFormat('en-IN').format(value)
      : value;

  return (
    <div
      className={`rounded-2xl border border-border bg-card text-card-foreground shadow-sm ${compact ? 'p-4' : 'p-5'} transition-all duration-200 hover:shadow-md dark:hover:shadow-[0_8px_24px_rgba(0,0,0,0.35)] ${colorStyles.border}`}
    >
      <div className="flex items-start justify-between">
        <div className="space-y-1">
          <p className={`${compact ? 'text-[10px]' : 'text-xs'} font-semibold tracking-wider text-muted-foreground uppercase`}>{title}</p>
          <p className={`${compact ? 'text-xl' : 'text-3xl'} font-extrabold tabular-nums text-foreground tracking-tight whitespace-nowrap`}>{displayValue}</p>
          {subtitle && <p className={`${compact ? 'text-[10px]' : 'text-xs'} text-muted-foreground`}>{subtitle}</p>}
          {trend && <p className="text-xs font-medium text-emerald-600 mt-1">{trend}</p>}
        </div>
        {icon && (
          <div className={`${compact ? 'p-2' : 'p-2.5'} rounded-xl flex items-center justify-center ${colorStyles.bg}`}>
            {icon}
          </div>
        )}
      </div>
    </div>
  );
};
