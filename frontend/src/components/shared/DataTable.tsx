import React from 'react';
import { ChevronLeft, ChevronRight, Search } from 'lucide-react';
import { LoadingSpinner } from './LoadingSpinner';
import { EmptyState } from './EmptyState';

export interface Column<T> {
  header: string;
  accessorKey?: keyof T;
  cell?: (row: T) => React.ReactNode;
  align?: 'left' | 'center' | 'right';
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  totalElements?: number;
  page?: number;
  pageSize?: number;
  totalPages?: number;
  isLoading?: boolean;
  searchQuery?: string;
  onSearchChange?: (query: string) => void;
  searchPlaceholder?: string;
  onPageChange?: (newPage: number) => void;
  onPageSizeChange?: (newSize: number) => void;
  footerRow?: React.ReactNode;
  actions?: React.ReactNode;
  emptyMessage?: string;
  /** Stable key for each row (e.g. row => row.allocationId). Falls back to the row index. */
  rowKey?: (row: T) => React.Key;
  /** Optional content rendered as a full-width row under a given row (drill-downs). */
  renderExpanded?: (row: T) => React.ReactNode | null;
}

export function DataTable<T>({
  columns,
  data,
  totalElements,
  page = 0,
  pageSize = 10,
  totalPages,
  isLoading = false,
  searchQuery,
  onSearchChange,
  searchPlaceholder = 'Search records...',
  onPageChange,
  onPageSizeChange,
  footerRow,
  actions,
  emptyMessage = 'No records found',
  rowKey,
  renderExpanded,
}: DataTableProps<T>) {
  const calculatedTotalPages = totalPages ?? (totalElements ? Math.ceil(totalElements / pageSize) : 1);
  const startIndex = page * pageSize + 1;
  const endIndex = totalElements ? Math.min((page + 1) * pageSize, totalElements) : data.length;

  return (
    <div className="overflow-hidden rounded-2xl border border-border bg-card text-card-foreground shadow-sm">
      {(onSearchChange || actions) && (
        <div className="flex flex-col items-center justify-between gap-3 border-b border-border bg-muted/50 p-4 sm:flex-row">
          {onSearchChange ? (
            <div className="relative w-full sm:w-72">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                value={searchQuery ?? ''}
                onChange={(e) => onSearchChange(e.target.value)}
                placeholder={searchPlaceholder}
                className="h-10 w-full rounded-lg border border-input bg-card pl-9 pr-3 text-sm text-foreground placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-primary/30"
              />
            </div>
          ) : (
            <div />
          )}

          {actions && <div className="flex items-center gap-2 w-full sm:w-auto">{actions}</div>}
        </div>
      )}

      <div className="overflow-x-auto min-h-[220px]">
        {isLoading ? (
          <div className="py-12">
            <LoadingSpinner text="Fetching data..." />
          </div>
        ) : data.length === 0 ? (
          <div className="py-8">
            <EmptyState title={emptyMessage} description="Try adjusting your search or filters." />
          </div>
        ) : (
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="sticky top-0 z-10 border-b border-border bg-muted text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
                {columns.map((col, idx) => (
                  <th
                    key={idx}
                    className={`py-3 px-4 ${
                      col.align === 'right'
                        ? 'text-right'
                        : col.align === 'center'
                        ? 'text-center'
                        : 'text-left'
                    } ${col.className || ''}`}
                  >
                    {col.header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {data.map((row, rowIdx) => {
                const expanded = renderExpanded ? renderExpanded(row) : null;
                return (
                <React.Fragment key={rowKey ? rowKey(row) : rowIdx}>
                <tr
                  className="text-foreground transition-colors hover:bg-muted/70"
                >
                  {columns.map((col, colIdx) => (
                    <td
                      key={colIdx}
                      className={`py-2.5 px-4 ${
                        col.align === 'right'
                          ? 'text-right'
                          : col.align === 'center'
                          ? 'text-center'
                          : 'text-left'
                      } ${col.className || ''}`}
                    >
                      {col.cell
                        ? col.cell(row)
                        : col.accessorKey
                        ? (row[col.accessorKey] as React.ReactNode)
                        : null}
                    </td>
                  ))}
                </tr>
                {expanded && (
                  <tr className="bg-muted/60">
                    <td colSpan={columns.length} className="px-4 py-3">{expanded}</td>
                  </tr>
                )}
                </React.Fragment>
                );
              })}
            </tbody>
            {footerRow && (
              <tfoot>
                {footerRow}
              </tfoot>
            )}
          </table>
        )}
      </div>

      {onPageChange && totalElements !== undefined && totalElements > 0 && (
        <div className="flex flex-col items-center justify-between gap-3 border-t border-border bg-muted/50 px-4 py-3 text-xs text-muted-foreground sm:flex-row">
          <div>
            Showing <span className="font-semibold text-foreground">{startIndex}</span> to{' '}
            <span className="font-semibold text-foreground">{endIndex}</span> of{' '}
            <span className="font-semibold text-foreground">{totalElements}</span> entries
          </div>

          <div className="flex items-center gap-4">
            {onPageSizeChange && (
              <div className="flex items-center gap-1.5">
                <span>Rows:</span>
                <select
                  value={pageSize}
                  onChange={(e) => onPageSizeChange(Number(e.target.value))}
                  className="h-9 rounded-lg border border-input bg-card px-2 text-sm text-foreground focus-visible:ring-2 focus-visible:ring-primary/30"
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                  <option value={100}>100</option>
                </select>
              </div>
            )}

            <div className="flex items-center gap-1">
              <button
                type="button"
                disabled={page <= 0}
                onClick={() => onPageChange(page - 1)}
                className="flex h-9 w-9 items-center justify-center rounded-lg border border-border bg-card hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40 transition-colors"
                title="Previous page"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <span className="px-2 font-medium">
                Page {page + 1} of {calculatedTotalPages || 1}
              </span>
              <button
                type="button"
                disabled={page >= calculatedTotalPages - 1}
                onClick={() => onPageChange(page + 1)}
                className="flex h-9 w-9 items-center justify-center rounded-lg border border-border bg-card hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40 transition-colors"
                title="Next page"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
