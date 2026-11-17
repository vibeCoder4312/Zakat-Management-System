import React from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, BadgeCheck, Calculator, CheckCircle2, Clock3, Hourglass, Percent, ShieldCheck, Users } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { PageHeader } from '../components/layout/PageHeader';
import { KpiCard } from '../components/shared/KpiCard';
import { LoadingSpinner } from '../components/shared/LoadingSpinner';
import { EmptyState } from '../components/shared/EmptyState';
import { ErrorBanner } from '../components/shared/Form';
import { getCategoryPivot, getClassificationPivot, getDashboard } from '../api/dashboard';
import { getPlanning } from '../api/planning';
import { errorMessage } from '../api/client';
import { formatINR } from '../lib/formatCurrency';
import type { ChartItem, DonutItem, PivotTable, Planning } from '../types';

// Categorical slots from the validated reference palette (fixed order, never cycled).
const SERIES = { s1: 'var(--chart-green)', s2: 'var(--chart-blue)', s3: 'var(--chart-purple)' };
const GRID = 'var(--chart-grid)';
const AXIS = { fontSize: 10, fill: 'var(--chart-axis)' };

/** Compact Indian notation for axis ticks: 12.6L, 1.2Cr. */
const compactINR = (v: number) => {
  const a = Math.abs(v);
  if (a >= 1e7) return `₹${(v / 1e7).toFixed(1)}Cr`;
  if (a >= 1e5) return `₹${(v / 1e5).toFixed(1)}L`;
  if (a >= 1e3) return `₹${(v / 1e3).toFixed(0)}K`;
  return `₹${v}`;
};

const Card: React.FC<{ title: string; subtitle?: string; children: React.ReactNode; className?: string }> = ({ title, subtitle, children, className = '' }) => (
  <section className={`rounded-2xl border border-border bg-card p-4 text-card-foreground shadow-sm ${className}`}>
    <h2 className="text-base font-semibold text-foreground">{title}</h2>
    {subtitle && <p className="mb-2 text-xs text-muted-foreground">{subtitle}</p>}
    <div className="mt-2">{children}</div>
  </section>
);

const tooltipStyle = { fontSize: 11, borderRadius: 10, border: `1px solid ${GRID}`, backgroundColor: 'var(--chart-tooltip)', color: 'var(--foreground)' };

/** Paid vs pending per classification / category, horizontal stacked bars. */
const PaidPendingChart: React.FC<{ data: ChartItem[] }> = ({ data }) => {
  if (data.length === 0) return <EmptyState title="No allocations" />;
  const rows = [...data].sort((a, b) => b.total - a.total);
  const hasPending = rows.some((r) => r.pending > 0);
  return (
    <ResponsiveContainer width="100%" height={Math.max(180, rows.length * 30 + 40)}>
      <BarChart data={rows} layout="vertical" margin={{ left: 8, right: 16 }} barCategoryGap={6}>
        <CartesianGrid horizontal={false} stroke={GRID} strokeDasharray="3 3" />
        <XAxis type="number" tickFormatter={compactINR} tick={AXIS} axisLine={false} tickLine={false} />
        <YAxis type="category" dataKey="name" width={130} tick={AXIS} axisLine={false} tickLine={false} />
        <Tooltip formatter={(v: number) => formatINR(v)} contentStyle={tooltipStyle} cursor={{ fill: 'var(--muted)' }} />
        {hasPending && <Legend wrapperStyle={{ fontSize: 11 }} />}
        <Bar isAnimationActive={false} dataKey="paid" name="Paid" stackId="a" fill={SERIES.s1} radius={hasPending ? 0 : [0, 4, 4, 0]} stroke="var(--card)" strokeWidth={1} />
        {hasPending && <Bar isAnimationActive={false} dataKey="pending" name="Pending" stackId="a" fill={SERIES.s2} radius={[0, 4, 4, 0]} stroke="var(--card)" strokeWidth={1} />}
      </BarChart>
    </ResponsiveContainer>
  );
};

/** Share list: label, value and a thin bar. Readable without a legend and keeps every value visible. */
const ShareList: React.FC<{ items: DonutItem[] }> = ({ items }) => {
  if (items.length === 0) return <EmptyState title="No data" />;
  const max = Math.max(...items.map((i) => i.amount), 1);
  return (
    <ul className="space-y-2">
      {items.map((i) => (
        <li key={i.label} className="text-xs">
          <div className="flex justify-between gap-2">
            <span className="text-[#10231F]">{i.label}</span>
            <span className="text-[#5B6B67] whitespace-nowrap">
              {formatINR(i.amount)} · {Number(i.percentage).toFixed(1)}%
            </span>
          </div>
          <div className="mt-1 h-1.5 bg-[#F2F6F4] rounded">
            <div className="h-1.5 rounded" style={{ width: `${(i.amount / max) * 100}%`, background: SERIES.s1 }} />
          </div>
        </li>
      ))}
    </ul>
  );
};

const Pivot: React.FC<{ pivot?: PivotTable; isLoading: boolean }> = ({ pivot, isLoading }) => {
  if (isLoading) return <LoadingSpinner size="sm" />;
  if (!pivot || pivot.rows.length === 0) return <EmptyState title="No paid allocations" />;
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-[11px]">
        <thead>
          <tr className="text-[#5B6B67] border-b border-[#E1E8E5]">
            <th className="text-left py-1.5 pr-3">&nbsp;</th>
            {pivot.years.map((y) => (
              <th key={y} className="text-right py-1.5 px-2">{y}</th>
            ))}
            <th className="text-right py-1.5 pl-2">Total</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-[#E1E8E5]">
          {pivot.rows.map((r) => (
            <tr key={r.rowLabel}>
              <td className="py-1.5 pr-3 font-medium whitespace-nowrap">{r.rowLabel}</td>
              {pivot.years.map((y) => (
                <td key={y} className="text-right py-1.5 px-2 whitespace-nowrap">{r.values[String(y)] ? formatINR(r.values[String(y)]) : '—'}</td>
              ))}
              <td className="text-right py-1.5 pl-2 font-semibold whitespace-nowrap">{formatINR(r.total)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr className="border-t-2 border-[#C9A84C] font-bold">
            <td className="py-1.5 pr-3">Total</td>
            {pivot.years.map((y) => (
              <td key={y} className="text-right py-1.5 px-2 whitespace-nowrap">{formatINR(pivot.yearTotals[String(y)] ?? 0)}</td>
            ))}
            <td className="text-right py-1.5 pl-2 whitespace-nowrap">{formatINR(pivot.grandTotal)}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
};

const PlanningReport: React.FC<{ rows: Planning[]; isLoading: boolean; error?: unknown }> = ({ rows, isLoading, error }) => {
  if (isLoading) return <LoadingSpinner size="sm" text="Loading planning report..." />;
  if (error) return <ErrorBanner message={errorMessage(error, 'Could not load the planning report.')} />;
  if (rows.length === 0) return <EmptyState title="No planning records" description="There are no plans for the selected year or years." />;

  const totals = rows.reduce((sum, row) => ({
    planned: sum.planned + Number(row.plannedAmount || 0),
    paid: sum.paid + Number(row.paidAmount || 0),
    remaining: sum.remaining + Number(row.remainingAmount || 0),
  }), { planned: 0, paid: 0, remaining: 0 });

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[760px] text-[11px]">
        <thead className="bg-[#F6FAF8] text-[#5B6B67] uppercase tracking-wide">
          <tr className="border-b border-[#E1E8E5]">
            <th className="text-left px-3 py-2">Plan type</th>
            <th className="text-left px-3 py-2">Classification</th>
            <th className="text-left px-3 py-2">Category</th>
            <th className="text-right px-3 py-2">Planned amount</th>
            <th className="text-right px-3 py-2">Paid amount</th>
            <th className="text-right px-3 py-2">Remaining amount</th>
            <th className="text-left px-3 py-2">Payment status</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-[#E1E8E5]">
          {rows.map((row) => (
            <tr key={row.planningId} className="hover:bg-[#F2F6F4]/50">
              <td className="px-3 py-2 font-semibold">{row.planType}</td>
              <td className="px-3 py-2">{row.classification || '—'}</td>
              <td className="px-3 py-2">{row.category || '—'}</td>
              <td className="px-3 py-2 text-right whitespace-nowrap">{formatINR(row.plannedAmount)}</td>
              <td className="px-3 py-2 text-right whitespace-nowrap">{formatINR(row.paidAmount)}</td>
              <td className="px-3 py-2 text-right whitespace-nowrap">{formatINR(row.remainingAmount)}</td>
              <td className="px-3 py-2">{row.paymentStatus}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr className="border-t-2 border-[#C9A84C] font-bold">
            <td className="px-3 py-2" colSpan={3}>Total</td>
            <td className="px-3 py-2 text-right whitespace-nowrap">{formatINR(totals.planned)}</td>
            <td className="px-3 py-2 text-right whitespace-nowrap">{formatINR(totals.paid)}</td>
            <td className="px-3 py-2 text-right whitespace-nowrap">{formatINR(totals.remaining)}</td>
            <td />
          </tr>
        </tfoot>
      </table>
    </div>
  );
};

export const Dashboard: React.FC = () => {
  const { selectedYears } = useOutletContext<{ selectedYears: number[] }>();
  const { data, isLoading, error } = useQuery({
    queryKey: ['dashboard', selectedYears],
    queryFn: () => getDashboard(selectedYears),
  });
  const clsPivot = useQuery({ queryKey: ['dashboard', 'pivot', 'classification'], queryFn: getClassificationPivot });
  const catPivot = useQuery({ queryKey: ['dashboard', 'pivot', 'category'], queryFn: getCategoryPivot });
  const planningReport = useQuery({
    queryKey: ['dashboard', 'planning-report', selectedYears],
    queryFn: () => getPlanning(selectedYears),
  });

  if (isLoading) return <LoadingSpinner text="Loading dashboard..." />;
  if (error || !data) return <ErrorBanner message={errorMessage(error, 'Could not load the dashboard.')} />;

  const { hero, kpis } = data;
  const pct = Math.min(Number(hero.obligationPercentage) || 0, 100);
  const overAllocated = hero.unallocated < 0;

  return (
    <div className="space-y-5">
      <PageHeader title="Dashboard" />

      {/* Hero: obligation progress */}
      <section className="rounded-2xl bg-green-700 text-white p-5 border-b-4 border-[#C9A84C] shadow-sm dark:bg-card">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="text-lg font-bold">{hero.yearTitle}</h2>
          <span className="rounded-full border border-white/40 bg-white/15 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-white">{hero.statusPill}</span>
        </div>
        <div className="mt-4 grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <p className="text-[11px] uppercase tracking-wider text-emerald-100/80">Zakat paid</p>
            <p className="text-3xl font-bold">{formatINR(hero.zakatPaid)}</p>
            <p className="text-xs text-emerald-100/80">of {formatINR(hero.netPayable)} net payable</p>
          </div>
          <div className="md:col-span-2 flex flex-col justify-end">
            <div className="flex justify-between text-[11px] text-emerald-100/80 mb-1">
              <span>{Number(hero.obligationPercentage).toFixed(1)}% of obligation met</span>
              <span className={overAllocated ? 'text-white font-semibold' : ''}>
                {overAllocated ? `Over-allocated by ${formatINR(Math.abs(hero.unallocated))}` : `Unallocated ${formatINR(hero.unallocated)}`}
              </span>
            </div>
            <div className="h-2.5 rounded bg-white/15" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
              <div className="h-2.5 rounded bg-green-300" style={{ width: `${pct}%` }} />
            </div>
            <div className="flex flex-wrap gap-x-6 gap-y-1 mt-3 text-xs text-emerald-50">
              <span>Zakat amount {formatINR(hero.zakatAmount)}</span>
              <span>Carry forward {formatINR(hero.carryForward)}</span>
              <span>Planned, not paid {formatINR(hero.plannedOutstanding)}</span>
              <span>Sadaqah paid (separate) {formatINR(hero.sadaqahPaid)}</span>
            </div>
          </div>
        </div>
      </section>

      <div className="grid grid-cols-2 sm:grid-cols-4 2xl:grid-cols-8 gap-3">
        <KpiCard compact title="Net payable" value={formatINR(kpis.netPayable)} subtitle="After carry forward" icon={<Calculator className="w-4 h-4" />} color="default" />
        <KpiCard compact title="Paid out" value={formatINR(hero.zakatPaid)} subtitle={`${kpis.paidCount} payments`} icon={<CheckCircle2 className="w-4 h-4" />} color="emerald" />
        <KpiCard compact title="Planned, not paid" value={formatINR(kpis.plannedNotPaidAmount)} subtitle={`${kpis.plannedNotPaidCount} allocations waiting`} icon={<Hourglass className="w-4 h-4" />} color="amber" />
        <KpiCard compact title="Over-allocated" value={formatINR(kpis.overallocated ? kpis.remainingOrOverallocated : 0)} subtitle={kpis.overallocated ? 'Allocations exceed net payable' : 'Within net payable'} icon={<AlertTriangle className="w-4 h-4" />} color="rose" />
        <KpiCard compact title="Distributed" value={`${Math.min(Number(kpis.distributedPercentage) || 0, 100).toFixed(1)}%`} subtitle="Of net payable" icon={<Percent className="w-4 h-4" />} color="violet" />
        <KpiCard compact title="Beneficiaries helped" value={kpis.beneficiariesHelped} subtitle={selectedYears.length === 1 ? 'This year' : selectedYears.length > 1 ? 'Selected years' : 'All years'} icon={<Users className="w-4 h-4" />} color="indigo" />
        <KpiCard compact title="Verified & active" value={kpis.verifiedActiveCount} subtitle="Eligible to receive" icon={<ShieldCheck className="w-4 h-4" />} color="emerald" />
        <KpiCard compact title="Awaiting verification" value={kpis.awaitingVerificationCount} subtitle={`${kpis.missingReceiptsCount} paid without receipt`} icon={<Clock3 className="w-4 h-4" />} color="sky" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card title="Classification × Year" subtitle="All-fund paid totals (Zakat + Sadaqah), every year">
          <Pivot pivot={clsPivot.data} isLoading={clsPivot.isLoading} />
        </Card>
        <Card title="Category × Year" subtitle="All-fund paid totals (Zakat + Sadaqah), every year">
          <Pivot pivot={catPivot.data} isLoading={catPivot.isLoading} />
        </Card>
      </div>

      <Card title="Planning Report" subtitle="Planned, paid, and remaining amounts for the selected year or years">
        <PlanningReport rows={planningReport.data ?? []} isLoading={planningReport.isLoading} error={planningReport.error} />
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card title="Distribution by classification" subtitle="Paid and still-pending amounts, all funds">
          <PaidPendingChart data={data.distributionByClassification} />
        </Card>
        <Card title="Distribution by category" subtitle="Paid and still-pending amounts, all funds">
          <PaidPendingChart data={data.distributionByCategory} />
        </Card>
      </div>

      <Card title="Year by year" subtitle="Net payable compared with Zakat paid; Sadaqah shown separately">
        {data.yearlyDistribution.length === 0 ? (
          <EmptyState title="No years" />
        ) : (
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={data.yearlyDistribution} barGap={2} margin={{ right: 8 }}>
              <CartesianGrid vertical={false} stroke={GRID} />
              <XAxis dataKey="year" tick={AXIS} axisLine={false} tickLine={false} />
              <YAxis tickFormatter={compactINR} tick={AXIS} axisLine={false} tickLine={false} width={56} />
              <Tooltip formatter={(v: number) => formatINR(v)} contentStyle={tooltipStyle} cursor={{ fill: 'var(--muted)' }} />
              <Legend wrapperStyle={{ fontSize: 11 }} />
              <Bar isAnimationActive={false} dataKey="netPayable" name="Net payable" fill={SERIES.s1} radius={[4, 4, 0, 0]} />
              <Bar isAnimationActive={false} dataKey="zakatPaid" name="Zakat paid" fill={SERIES.s2} radius={[4, 4, 0, 0]} />
              <Bar isAnimationActive={false} dataKey="sadaqahPaid" name="Sadaqah paid" fill={SERIES.s3} radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card title="By beneficiary category">
          <ShareList items={data.beneficiaryCategoryDonut} />
        </Card>
        <Card title="By payment method" subtitle="Most historical rows have no method recorded (Not set)">
          <ShareList items={data.cashVsOnlineDonut} />
        </Card>
        <Card title="By source account" subtitle="Most historical rows have no source account recorded">
          <ShareList items={data.sourceAccountDistribution} />
        </Card>
      </div>

      {data.monthlyDistribution.length > 0 && (
        <Card title="Monthly payments" subtitle="Only payments with a Paid On date">
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={data.monthlyDistribution}>
              <CartesianGrid vertical={false} stroke={GRID} />
              <XAxis dataKey="monthYear" tick={AXIS} axisLine={false} tickLine={false} />
              <YAxis tickFormatter={compactINR} tick={AXIS} axisLine={false} tickLine={false} width={56} />
              <Tooltip formatter={(v: number) => formatINR(v)} contentStyle={tooltipStyle} cursor={{ fill: 'var(--muted)' }} />
              <Bar isAnimationActive={false} dataKey="amount" name="Paid" fill={SERIES.s1} radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card title="Needs attention">
          {data.needsAttention.length === 0 ? (
            <EmptyState title="Nothing outstanding" icon={<BadgeCheck className="w-6 h-6 text-emerald-600" />} />
          ) : (
            <ul className="divide-y divide-[#E1E8E5]">
              {data.needsAttention.map((a, idx) => (
                <li key={`${a.badgeType}-${a.entityId ?? idx}`} className="py-2 flex items-center justify-between gap-3 text-xs">
                  <div>
                    <span className="inline-flex items-center gap-1 text-[10px] font-semibold px-1.5 py-0.5 rounded border border-amber-200 bg-amber-50 text-amber-800 mr-2">
                      <AlertTriangle className="w-3 h-3" /> {a.badgeLabel}
                    </span>
                    <span className="font-medium">{a.beneficiary}</span>
                    {a.detail && <span className="text-[#5B6B67]"> · {a.detail}</span>}
                  </div>
                  {a.amount != null && <span className="font-semibold whitespace-nowrap">{formatINR(a.amount)}</span>}
                </li>
              ))}
            </ul>
          )}
          <Link to="/allocations" className="block mt-3 text-[11px] font-semibold text-[#0F766E] hover:underline">Open allocations →</Link>
        </Card>
        <Card title="Recent payments" subtitle="Dated payments first, then the latest paid allocations">
          {data.recentPayments.length === 0 ? (
            <EmptyState title="No dated payments" description="Historical rows were imported without payment dates." />
          ) : (
            <table className="w-full text-[11px]">
              <tbody className="divide-y divide-[#E1E8E5]">
                {data.recentPayments.map((p, i) => (
                  <tr key={i}>
                    <td className="py-1.5 pr-2 whitespace-nowrap text-[#5B6B67]">{p.paidOn ?? ''}</td>
                    <td className="py-1.5 pr-2 font-medium">{p.beneficiary}</td>
                    <td className="py-1.5 pr-2">{p.classification}</td>
                    <td className="py-1.5 pr-2 text-[#5B6B67]">{p.paymentMethod ?? ''}</td>
                    <td className="py-1.5 text-right font-semibold whitespace-nowrap">{formatINR(p.paidAmount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>

    </div>
  );
};
