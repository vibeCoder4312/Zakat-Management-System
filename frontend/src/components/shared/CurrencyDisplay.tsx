import React from 'react';
import { formatCurrencyWithFx, formatINR } from '../../lib/formatCurrency';
import { Currency } from '../../types';

interface CurrencyDisplayProps {
  amount: number | null | undefined;
  currency?: Currency;
  fxRate?: number | null;
  inrEquivalent?: number | null;
  highlightNegative?: boolean;
  className?: string;
}

export const CurrencyDisplay: React.FC<CurrencyDisplayProps> = ({
  amount,
  currency = 'INR',
  fxRate,
  inrEquivalent,
  highlightNegative = false,
  className = '',
}) => {
  if (amount == null) return <span className={`text-gray-400 ${className}`}>—</span>;

  const isNegative = amount < 0;
  const colorClass = highlightNegative && isNegative ? 'text-rose-600 font-semibold' : '';

  if (currency === 'AED') {
    return (
      <span className={`${colorClass} ${className}`}>
        {formatCurrencyWithFx(amount, 'AED', fxRate, inrEquivalent)}
      </span>
    );
  }

  return (
    <span className={`${colorClass} ${className}`}>
      {formatINR(amount)}
    </span>
  );
};
