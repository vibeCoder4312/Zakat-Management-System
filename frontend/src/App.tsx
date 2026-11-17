import React from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './contexts/AuthContext';
import { AppShell } from './components/layout/AppShell';

// Pages
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { Assessments } from './pages/Assessments';
import { Allocations } from './pages/Allocations';
import { PlanningPage } from './pages/Planning';
import { Beneficiaries } from './pages/Beneficiaries';
import { Users } from './pages/Users';

// Master Pages
import { Agents, Categories, Classifications, Employees, Schools, SourceAccounts } from './pages/masters';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />

            <Route path="/" element={<AppShell />}>
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="assessments" element={<Assessments />} />
              <Route path="allocations" element={<Allocations />} />
              <Route path="planning" element={<PlanningPage />} />
              <Route path="beneficiaries" element={<Beneficiaries />} />

              {/* Masters */}
              <Route path="masters">
                <Route path="classifications" element={<Classifications />} />
                <Route path="categories" element={<Categories />} />
                <Route path="source-accounts" element={<SourceAccounts />} />
                <Route path="schools" element={<Schools />} />
                <Route path="agents" element={<Agents />} />
                <Route path="employees" element={<Employees />} />
                <Route path="users" element={<Users />} />
              </Route>

              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;
