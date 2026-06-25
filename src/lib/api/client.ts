import type { ApiErrorResponse } from '@/types/auth';

const BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL;

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<TResponse>(
  path: string,
  options: { method: 'GET' | 'POST' | 'PATCH' | 'DELETE'; body?: unknown; accessToken?: string }
): Promise<TResponse | undefined> {
  if (!BASE_URL) {
    throw new Error('EXPO_PUBLIC_API_BASE_URL is not set — see .env');
  }

  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (options.accessToken) {
    headers.Authorization = `Bearer ${options.accessToken}`;
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method: options.method,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  if (!response.ok) {
    const errorBody = (await response.json().catch(() => null)) as ApiErrorResponse | null;
    throw new ApiError(response.status, errorBody?.message ?? 'Something went wrong. Please try again.');
  }

  if (response.status === 204 || response.status === 202) {
    return undefined;
  }

  return (await response.json()) as TResponse;
}

export const apiClient = {
  post: <TResponse>(path: string, body?: unknown, accessToken?: string) =>
    request<TResponse>(path, { method: 'POST', body, accessToken }),
  get: <TResponse>(path: string, accessToken?: string) => request<TResponse>(path, { method: 'GET', accessToken }),
  patch: <TResponse>(path: string, body?: unknown, accessToken?: string) =>
    request<TResponse>(path, { method: 'PATCH', body, accessToken }),
  delete: <TResponse>(path: string, accessToken?: string) =>
    request<TResponse>(path, { method: 'DELETE', accessToken }),
};
