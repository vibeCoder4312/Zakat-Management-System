import { beforeEach, describe, expect, it } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useYearFilter } from '../hooks/useYearFilter';
import { YEAR_FILTER_KEY } from '../lib/constants';

describe('useYearFilter', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('initializes with empty array if nothing in localStorage', () => {
    const { result } = renderHook(() => useYearFilter());
    expect(result.current.selectedYears).toEqual([]);
  });

  it('saves selected years to localStorage and never saves "ALL"', () => {
    const { result } = renderHook(() => useYearFilter());

    act(() => {
      result.current.setSelectedYears([2024, 2025]);
    });

    expect(result.current.selectedYears).toEqual([2024, 2025]);
    const stored = JSON.parse(localStorage.getItem(YEAR_FILTER_KEY) || '[]');
    expect(stored).toEqual([2024, 2025]);
    expect(stored).not.toContain('ALL');
  });

  it('toggles a year on and off correctly', () => {
    const { result } = renderHook(() => useYearFilter());

    act(() => {
      result.current.toggleYear(2026);
    });
    expect(result.current.selectedYears).toEqual([2026]);

    act(() => {
      result.current.toggleYear(2025);
    });
    expect(result.current.selectedYears).toEqual([2025, 2026]);

    act(() => {
      result.current.toggleYear(2026);
    });
    expect(result.current.selectedYears).toEqual([2025]);
  });

  it('clears all selected years', () => {
    const { result } = renderHook(() => useYearFilter());

    act(() => {
      result.current.setSelectedYears([2021, 2022, 2023]);
    });
    expect(result.current.selectedYears.length).toBe(3);

    act(() => {
      result.current.clearAll();
    });
    expect(result.current.selectedYears).toEqual([]);
    expect(JSON.parse(localStorage.getItem(YEAR_FILTER_KEY) || '[]')).toEqual([]);
  });
});
