/**
 * Format a number using Indian Numbering System (e.g. ₹41,05,212)
 *
 * Rules:
 * - Currency symbol is ₹
 * - Standard grouping: 3 digits for hundreds, then groups of 2 (lakhs, crores)
 * - Negative values format with a minus: -₹5,000 or -₹41,05,212
 */
export function formatINR(amount: number | null | undefined, compact = false): string {
  if (amount == null || isNaN(amount)) return '₹0';

  const isNegative = amount < 0;
  const absAmount = Math.abs(amount);

  const formatter = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: compact ? 0 : 2,
    minimumFractionDigits: 0,
  });

  const formatted = formatter.format(absAmount);
  return isNegative ? `-${formatted}` : formatted;
}

/**
 * Format number with Indian number grouping without the ₹ currency symbol
 */
export function formatNumber(n: number | null | undefined): string {
  if (n == null || isNaN(n)) return '0';
  return new Intl.NumberFormat('en-IN').format(n);
}

/**
 * Format currency with FX details if foreign currency (e.g. AED 1,000 (₹22,500))
 */
export function formatCurrencyWithFx(
  amount: number | null | undefined,
  currency: 'INR' | 'AED' = 'INR',
  fxRate?: number | null,
  inrEquivalent?: number | null
): string {
  if (amount == null) return '₹0';
  if (currency === 'AED') {
    const formattedAed = `AED ${new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount)}`;
    if (inrEquivalent != null) {
      return `${formattedAed} (${formatINR(inrEquivalent)})`;
    }
    if (fxRate != null) {
      return `${formattedAed} (${formatINR(amount * fxRate)})`;
    }
    return formattedAed;
  }
  return formatINR(amount);
}
