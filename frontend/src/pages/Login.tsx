import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle2, Landmark, Lock, LogIn, ShieldCheck, User } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { errorMessage } from '../api/client';

export const Login: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      await login({ username, password });
      navigate('/dashboard', { replace: true });
    } catch (err: any) {
      setError(
        errorMessage(err, 'Authentication failed. Please check your username and password.')
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen grid bg-background text-foreground lg:grid-cols-[minmax(400px,40%)_1fr]">
      <section className="flex min-h-screen items-center justify-center bg-[#f8fffb] px-5 py-10 dark:bg-[#0d1117] sm:px-8 lg:px-12">
        <div className="w-full max-w-md rounded-[28px] border border-border bg-card p-7 shadow-[0_20px_60px_rgba(0,0,0,0.08)] dark:shadow-[0_8px_24px_rgba(0,0,0,0.4)] sm:p-9">
          <div className="mb-8 flex flex-col items-center text-center">
            <div className="mb-5 flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-[#22C55E] to-[#16A34A] text-white shadow-lg shadow-green-500/20" aria-hidden="true">
              <Landmark className="h-5 w-5" />
            </div>
            <h1 className="text-3xl font-black tracking-tight text-foreground">Zakat Management System</h1>
            <p className="mt-2 text-sm text-muted-foreground">Secure financial administration for transparent giving.</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
          {error && (
            <div role="alert" className="rounded-lg border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-500/30 dark:bg-[#2d1214] dark:text-red-400">
              {error}
            </div>
          )}

          <div className="space-y-2">
            <label className="flex items-center gap-2 text-sm font-semibold text-foreground">
              <User className="h-4 w-4 text-primary" />
              Username
            </label>
            <input
              type="text"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              className="h-11 w-full rounded-lg border border-input bg-card px-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
              placeholder="Enter username"
            />
          </div>

          <div className="space-y-2">
            <label className="flex items-center gap-2 text-sm font-semibold text-foreground">
              <Lock className="h-4 w-4 text-primary" />
              Password
            </label>
            <input
              type="password"
              required
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="h-11 w-full rounded-lg border border-input bg-card px-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
              placeholder="Enter password"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-primary px-4 text-sm font-semibold text-primary-foreground shadow-sm transition-colors hover:bg-green-600 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 dark:hover:bg-green-400"
            aria-busy={loading}
          >
            {loading ? (
              <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <>
                <LogIn className="w-4 h-4" />
                Sign In
              </>
            )}
          </button>
          </form>
          <p className="mt-7 text-center text-xs text-muted-foreground">Authorized users only</p>
        </div>
      </section>

      <aside className="relative hidden overflow-hidden bg-[#edf7f3] p-12 dark:bg-[#0d1117] lg:flex lg:flex-col lg:justify-between xl:p-16">
        <div className="absolute -right-24 -top-24 h-96 w-96 rounded-full bg-green-400/10 blur-3xl dark:bg-green-400/5" />
        <div className="absolute -bottom-28 -left-20 h-96 w-96 rounded-full bg-emerald-300/20 blur-3xl dark:bg-green-400/5" />
        <div className="relative z-10 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-[#22C55E] to-[#16A34A] text-white"><Landmark className="h-5 w-5" /></div>
          <span className="font-bold tracking-tight text-foreground">Zakat Management System</span>
        </div>
        <div className="relative z-10 max-w-2xl py-10">
          <span className="mb-5 inline-flex items-center gap-2 rounded-full border border-green-500/20 bg-green-500/10 px-3 py-1.5 text-xs font-semibold text-green-700 dark:text-green-400"><ShieldCheck className="h-4 w-4" />Clear records. Trusted stewardship.</span>
          <h2 className="max-w-xl text-4xl font-black leading-tight tracking-tight text-foreground xl:text-5xl">Manage every contribution with clarity and care.</h2>
          <p className="mt-5 max-w-lg text-base leading-7 text-muted-foreground">A dependable workspace for zakat assessments, allocations, beneficiaries, and annual reporting.</p>
          <div className="mt-9 grid max-w-lg grid-cols-2 gap-3">
            {['Annual assessments', 'Transparent allocations', 'Beneficiary records', 'Year-by-year reports'].map((item) => <div key={item} className="flex items-center gap-2 rounded-xl border border-border bg-card/70 px-3 py-3 text-sm font-medium text-foreground shadow-sm"><CheckCircle2 className="h-4 w-4 shrink-0 text-primary" />{item}</div>)}
          </div>
        </div>
        <p className="relative z-10 text-xs text-muted-foreground">Financial administration · Private and secure</p>
      </aside>
    </div>
  );
};
