# Zakat Management System UI

## Theme behavior

- The app supports **Light** and **Dark** choices from the header theme menu.
- The existing `zakat-theme` preference is still read and written for compatibility. The canonical preference is `theme`.
- The current dark experience is preserved as the initial default and uses the GitHub Dark surface palette.
- `index.html` applies the saved/resolved theme before the app starts to avoid a flash. The `ThemeProvider` keeps the `dark` class and resolved `data-theme` in sync.

## Semantic tokens

| Token | Light | Dark |
|---|---|---|
| `--background` | `#F6F8FB` | `#0d1117` |
| `--foreground` | `#171717` | `#e6edf3` |
| `--card` | `#ffffff` | `#161b22` |
| `--popover` | `#ffffff` | `#1c2128` |
| `--primary` | `#16a34a` | `#22C55E` |
| `--secondary`, `--muted`, `--accent` | `#f1f5f9` | `#1c2128` / `#161b22` |
| `--muted-foreground` | `#64748b` | `#7d8590` |
| `--destructive` | `#ef4444` | `#f85149` |
| `--border`, `--input` | `#e2e8f0` | `#30363d` / `#1c2128` |
| `--ring` | `#16a34a` | `#22C55E` |

Charts use CSS variables for grid, axis, tooltip, and series colors so they follow both themes. Print styles force a light surface.

## Component rules

- Use semantic Tailwind colors such as `bg-background`, `bg-card`, `bg-popover`, `text-foreground`, `text-muted-foreground`, `border-border`, and `ring-ring` for new UI.
- Cards use a subtle border, rounded corners, and a restrained shadow in light mode. Dark cards rely on borders and minimal shadow.
- Primary actions use emerald. Amber, orange, red, and blue are reserved for caution, high severity, errors, and information.
- Inputs and selects use the theme surface, a visible emerald focus ring, and a minimum 44px control height.
- Tables use muted headers, subtle borders, and a muted row hover. KPI values use tabular numerals.
- Icon-only actions need an accessible name and a 40px hit area. Keyboard focus remains visible.
- Respect reduced-motion preferences. Keep the data, routes, and business behavior independent of theme.

## Current coverage

The global shell, login, theme menu, shared table, modal, form primitives, page headers, KPI cards, empty state, and dashboard chart colors use the semantic system. Legacy page classes are kept compatible with both palettes and can be migrated incrementally without changing their data behavior.
