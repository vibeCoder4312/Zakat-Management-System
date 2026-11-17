import { describe, expect, it } from 'vitest';
import { formatCurrencyWithFx, formatINR, formatNumber } from '../lib/formatCurrency';

describe('formatINR', () => {
  it('formats positive amount in Indian grouping with ₹ symbol', () => {
    const formatted = formatINR(4105212);
    // Should contain 41,05,212 and ₹
    expect(formatted).toContain('41,05,212');
    expect(formatted).toContain('₹');
  });

  it('formats zero correctly', () => {
    const formatted = formatINR(0);
    expect(formatted).toContain('0');
    expect(formatted).toContain('₹');
  });

  it('formats null/undefined as ₹0', () => {
    expect(formatINR(null)).toBe('₹0');
    expect(formatINR(undefined)).toBe('₹0');
  });

  it('formats negative amounts with minus sign and does not clamp to zero', () => {
    const formatted = formatINR(-50000);
    expect(formatted).toContain('-');
    expect(formatted).toContain('50,000');
    expect(formatted).toContain('₹');
  });
});

describe('formatNumber', () => {
  it('formats number with Indian grouping without ₹ symbol', () => {
    const formatted = formatNumber(12639792.93);
    expect(formatted).toContain('1,26,39,792');
  });
});

describe('formatCurrencyWithFx', () => {
  it('formats AED with INR equivalent when fxRate is provided', () => {
    const formatted = formatCurrencyWithFx(1000, 'AED', 22.5);
    expect(formatted).toContain('AED 1,000.00');
    expect(formatted).toContain('22,500');
  });

  it('formats standard INR when currency is INR', () => {
    const formatted = formatCurrencyWithFx(15000, 'INR');
    expect(formatted).toContain('15,000');
    expect(formatted).toContain('₹');
  });
});
