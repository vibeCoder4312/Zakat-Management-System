import { describe, expect, it } from 'vitest';
import { AxiosError, AxiosHeaders } from 'axios';
import { apiClient, cleanParams, errorMessage } from '../api/client';

describe('apiClient', () => {
  it('uses the versioned API path by default', () => {
    expect(apiClient.defaults.baseURL).toBe('/api/v1');
  });

  it('sends year lists as repeated keys, the way Spring binds List<Integer>', () => {
    const url = apiClient.getUri({ url: '/allocations', params: cleanParams({ years: [2025, 2026], q: '' }) });
    expect(url).toContain('years=2025&years=2026');
    expect(url).not.toContain('years[]');
    expect(url).not.toContain('q=');
  });
});

describe('cleanParams', () => {
  it('drops empty values but keeps zero', () => {
    expect(cleanParams({ a: undefined, b: '', c: [], d: 0, e: 'x' })).toEqual({ d: 0, e: 'x' });
  });
});

describe('errorMessage', () => {
  const axiosError = (status: number, data: unknown) =>
    new AxiosError('fail', 'ERR', undefined, undefined, {
      status, statusText: '', data, headers: {}, config: { headers: new AxiosHeaders() },
    });

  it('shows the business-rule message from the API', () => {
    const e = axiosError(422, { message: 'This year is CLOSED.' });
    expect(errorMessage(e)).toBe('This year is CLOSED.');
  });

  it('joins field validation errors', () => {
    const e = axiosError(400, { message: 'Validation failed', fieldErrors: { name: 'Name is required' } });
    expect(errorMessage(e)).toBe('Name is required');
  });

  it('explains a 403 without a body', () => {
    expect(errorMessage(axiosError(403, ''))).toMatch(/permission/);
  });
});
