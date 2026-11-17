import { useCallback, useState } from 'react';
import { YEAR_FILTER_KEY } from '../lib/constants';

export function useYearFilter() {
  const [selectedYears, setSelectedYearsState] = useState<number[]>(() => {
    try {
      const stored = localStorage.getItem(YEAR_FILTER_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (Array.isArray(parsed) && parsed.every((item) => typeof item === 'number' && !isNaN(item))) {
          return parsed;
        }
      }
    } catch (e) {
      // fallback
    }
    return [];
  });

  const setSelectedYears = useCallback((years: number[]) => {
    const validYears = years.filter((y) => typeof y === 'number' && !isNaN(y) && y >= 2000 && y <= 2100);
    setSelectedYearsState(validYears);
    try {
      localStorage.setItem(YEAR_FILTER_KEY, JSON.stringify(validYears));
    } catch (e) {
      // ignore
    }
  }, []);

  const toggleYear = useCallback(
    (year: number) => {
      setSelectedYearsState((prev) => {
        const next = prev.includes(year) ? prev.filter((y) => y !== year) : [...prev, year].sort((a, b) => a - b);
        try {
          localStorage.setItem(YEAR_FILTER_KEY, JSON.stringify(next));
        } catch (e) {
          // ignore
        }
        return next;
      });
    },
    []
  );

  const selectAll = useCallback(
    (allYears: number[]) => {
      setSelectedYears(allYears);
    },
    [setSelectedYears]
  );

  const clearAll = useCallback(() => {
    setSelectedYears([]);
  }, [setSelectedYears]);

  return {
    selectedYears,
    setSelectedYears,
    toggleYear,
    selectAll,
    clearAll,
  };
}
