import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

export type ThemeChoice = 'light' | 'dark';
type ResolvedTheme = ThemeChoice;

interface ThemeContextValue {
  theme: ThemeChoice;
  resolvedTheme: ResolvedTheme;
  isDark: boolean;
  setTheme: (theme: ThemeChoice) => void;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);
const STORAGE_KEY = 'theme';
const LEGACY_STORAGE_KEY = 'zakat-theme';

function readTheme(): ThemeChoice {
  try {
    const saved = localStorage.getItem(STORAGE_KEY) || localStorage.getItem(LEGACY_STORAGE_KEY);
    return saved === 'light' || saved === 'dark' ? saved : 'dark';
  } catch {
    return 'dark';
  }
}

function resolveTheme(theme: ThemeChoice): ResolvedTheme { return theme; }

function applyTheme(theme: ThemeChoice, resolved: ResolvedTheme) {
  const root = document.documentElement;
  root.classList.toggle('dark', resolved === 'dark');
  root.dataset.theme = resolved;
  root.dataset.themeChoice = theme;
}

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [theme, setThemeState] = useState<ThemeChoice>(readTheme);
  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme>(() => resolveTheme(readTheme()));

  const setTheme = useCallback((next: ThemeChoice) => {
    try {
      localStorage.setItem(STORAGE_KEY, next);
      // Continue writing the old key so existing app integrations keep seeing the selected light/dark value.
      localStorage.setItem(LEGACY_STORAGE_KEY, next);
    } catch {
      // The selected theme still applies when browser storage is unavailable.
    }
    setThemeState(next);
  }, []);

  useEffect(() => {
    const nextResolved = resolveTheme(theme);
    const root = document.documentElement;
    root.classList.add('no-transition');
    applyTheme(theme, nextResolved);
    setResolvedTheme(nextResolved);
    requestAnimationFrame(() => requestAnimationFrame(() => root.classList.remove('no-transition')));
  }, [theme]);

  const toggleTheme = useCallback(() => setTheme(resolvedTheme === 'dark' ? 'light' : 'dark'), [resolvedTheme, setTheme]);
  const value = useMemo(() => ({ theme, resolvedTheme, isDark: resolvedTheme === 'dark', setTheme, toggleTheme }), [theme, resolvedTheme, setTheme, toggleTheme]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export function useTheme() {
  const value = useContext(ThemeContext);
  if (!value) throw new Error('useTheme must be used within ThemeProvider');
  return value;
}
