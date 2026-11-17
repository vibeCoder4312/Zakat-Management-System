import React, { useEffect, useRef, useState } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useYearFilter } from '../../hooks/useYearFilter';
import { TopBar } from './TopBar';
import { Sidebar } from './Sidebar';

export const AppShell: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(() => window.matchMedia('(min-width: 1024px)').matches);
  const sidebarCloseTimer = useRef<number | null>(null);
  const { selectedYears, setSelectedYears } = useYearFilter();

  const cancelSidebarClose = () => {
    if (sidebarCloseTimer.current !== null) window.clearTimeout(sidebarCloseTimer.current);
    sidebarCloseTimer.current = null;
  };
  const openSidebar = () => {
    cancelSidebarClose();
    setSidebarOpen(true);
  };
  const scheduleSidebarClose = () => {
    cancelSidebarClose();
    sidebarCloseTimer.current = window.setTimeout(() => setSidebarOpen(false), 300);
  };
  const toggleSidebar = () => {
    cancelSidebarClose();
    setSidebarOpen((open) => !open);
  };

  useEffect(() => () => cancelSidebarClose(), []);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      <a href="#main-content" className="sr-only z-[100] rounded-lg bg-card px-4 py-3 text-foreground focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus-visible:ring-2 focus-visible:ring-ring">
        Skip to main content
      </a>
      <TopBar
        onToggleSidebar={toggleSidebar}
        onSidebarHover={openSidebar}
        onSidebarLeave={scheduleSidebarClose}
        sidebarOpen={sidebarOpen}
        selectedYears={selectedYears}
        onYearChange={setSelectedYears}
      />

      <div className="flex flex-1">
        <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} onHoverOpen={openSidebar} onHoverLeave={scheduleSidebarClose} />

        <main id="main-content" className={`flex-1 w-full min-w-0 transition-[padding] duration-200 ${sidebarOpen ? 'lg:pl-64' : ''}`}>
          <div className="p-4 sm:p-5 lg:p-6 max-w-[1600px] mx-auto">
            <Outlet context={{ selectedYears }} />
          </div>
        </main>
      </div>
    </div>
  );
};
