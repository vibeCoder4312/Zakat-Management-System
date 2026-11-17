// Relative by default: Vite (dev) and Nginx (Docker) both proxy /api to the Spring Boot backend.
// Set VITE_API_URL only when the frontend is served from a different host than the API.
export const API_BASE_URL: string =
  (import.meta as unknown as { env: { VITE_API_URL?: string } }).env.VITE_API_URL || '/api/v1';

export const JWT_KEY = 'zakat_token';
export const YEAR_FILTER_KEY = 'zakat_year_filter';

export const COLORS = {
  primary: '#0B4A3F',
  primaryDark: '#0A3D34',
  accent: '#C9A84C',
  bg: '#F7F5F0',
  white: '#FFFFFF',
  textOnPrimary: '#FFFFFF',
};

export const STATUS_COLORS: Record<string, string> = {
  PAID: 'bg-green-100 text-green-800 border-green-200',
  ACTIVE: 'bg-green-100 text-green-800 border-green-200',
  VERIFIED: 'bg-green-100 text-green-800 border-green-200',
  PLANNED: 'bg-blue-100 text-blue-800 border-blue-200',
  PARTIAL: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  PENDING: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  CANCELLED: 'bg-gray-100 text-gray-700 border-gray-200',
  INACTIVE: 'bg-gray-100 text-gray-700 border-gray-200',
  APPROVED: 'bg-green-100 text-green-800 border-green-200',
  OPEN: 'bg-sky-100 text-sky-800 border-sky-200',
  CLOSED: 'bg-gray-200 text-gray-800 border-gray-300',
  Y: 'bg-green-100 text-green-800 border-green-200',
  N: 'bg-gray-100 text-gray-700 border-gray-200',
  ZAKAT: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  SADAQAH: 'bg-cyan-100 text-cyan-800 border-cyan-200',
};
