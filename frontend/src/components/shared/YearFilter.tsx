import React, { useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Calendar, Check, ChevronDown, X } from 'lucide-react';
import { getYears } from '../../api/lookups';

interface YearFilterProps {
  selectedYears: number[];
  onChange: (years: number[]) => void;
  availableYears?: number[];
  className?: string;
}

export const YearFilter: React.FC<YearFilterProps> = ({
  selectedYears,
  onChange,
  availableYears: propYears,
  className = '',
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const { data: fetchedYears } = useQuery({
    queryKey: ['lookups', 'years'],
    queryFn: getYears,
    enabled: !propYears?.length,
    staleTime: 10 * 60 * 1000,
  });
  const yearsList = [...(propYears?.length ? propYears : fetchedYears?.length ? fetchedYears : [2020, 2021, 2022, 2023, 2024, 2025, 2026])]
    .sort((a, b) => b - a);
  const previousYearsRef = useRef(yearsList);

  useEffect(() => {
    const previousYears = previousYearsRef.current;
    const hasNewYear = yearsList.some((year) => !previousYears.includes(year));
    const hadAllYearsSelected = previousYears.length > 0 &&
      previousYears.every((year) => selectedYears.includes(year)) &&
      selectedYears.length === previousYears.length;

    previousYearsRef.current = yearsList;
    if (hasNewYear && hadAllYearsSelected) {
      onChange(yearsList);
    }
  }, [yearsList, selectedYears, onChange]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const toggleYear = (year: number) => {
    if (selectedYears.includes(year)) {
      onChange(selectedYears.filter((y) => y !== year));
    } else {
      onChange([...selectedYears, year].sort((a, b) => b - a));
    }
  };

  const handleSelectAll = () => {
    if (selectedYears.length === yearsList.length) {
      onChange([]);
    } else {
      onChange([...yearsList].sort((a, b) => b - a));
    }
  };

  const isAllSelected = selectedYears.length === 0 || selectedYears.length === yearsList.length;

  const buttonLabel = () => {
    if (selectedYears.length === 0 || selectedYears.length === yearsList.length) {
      return 'All Years';
    }
    if (selectedYears.length === 1) {
      return `${selectedYears[0]}`;
    }
    if (selectedYears.length <= 2) {
      return selectedYears.join(', ');
    }
    return `${selectedYears.length} Years Selected`;
  };

  return (
    <div className={`relative inline-block text-left ${className}`} ref={dropdownRef}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="inline-flex h-10 items-center gap-1.5 whitespace-nowrap rounded-xl border border-border bg-card px-2 text-xs font-semibold text-foreground shadow-sm transition-colors hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring sm:gap-2 sm:px-3 sm:text-sm"
      >
        <Calendar className="h-3.5 w-3.5 text-primary sm:h-4 sm:w-4" />
        <span>{buttonLabel()}</span>
        <ChevronDown className="h-3.5 w-3.5 text-muted-foreground sm:h-4 sm:w-4" />
      </button>

      {isOpen && (
        <div className="absolute right-0 z-50 mt-2 w-56 rounded-xl border border-border bg-popover py-2 text-sm text-popover-foreground shadow-xl animate-in fade-in slide-in-from-top-1 duration-150">
          <div className="flex items-center justify-between border-b border-border px-3 py-2">
            <span className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Assessment Years</span>
            <button
              type="button"
              onClick={handleSelectAll}
              className="text-xs font-semibold text-primary hover:underline"
            >
              {isAllSelected ? 'Deselect All' : 'Select All'}
            </button>
          </div>

          <div className="max-h-60 overflow-y-auto py-1">
            {yearsList.map((year) => {
              const checked = selectedYears.length === 0 || selectedYears.includes(year);
              return (
                <button
                  key={year}
                  type="button"
                  onClick={() => toggleYear(year)}
                  className="flex min-h-10 w-full items-center justify-between px-3 py-1.5 text-left text-sm text-foreground transition-colors hover:bg-muted"
                >
                  <span className="font-medium">{year}</span>
                  <div
                    className={`w-4 h-4 rounded border flex items-center justify-center transition-colors ${
                      checked
                        ? 'bg-primary border-primary text-primary-foreground'
                        : 'border-input bg-card'
                    }`}
                  >
                    {checked && <Check className="w-3 h-3 stroke-[3]" />}
                  </div>
                </button>
              );
            })}
          </div>

          {selectedYears.length > 0 && selectedYears.length < yearsList.length && (
            <div className="flex items-center justify-between border-t border-border px-3 pb-1 pt-2">
              <span className="text-xs text-muted-foreground">{selectedYears.length} selected</span>
              <button
                type="button"
                onClick={() => onChange([])}
                className="text-xs text-rose-600 hover:underline flex items-center gap-1"
              >
                <X className="w-3 h-3" /> Reset
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
