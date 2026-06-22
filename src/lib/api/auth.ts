import { apiClient } from '@/lib/api/client';
import type { AuthTokens, MeResponse } from '@/types/auth';

export function requestOtp(phoneNumberE164: string): Promise<void> {
  return apiClient.post('/api/v1/auth/otp/request', { phoneNumber: phoneNumberE164 }) as Promise<void>;
}

export function verifyOtp(phoneNumberE164: string, code: string): Promise<AuthTokens> {
  return apiClient.post<AuthTokens>('/api/v1/auth/otp/verify', { phoneNumber: phoneNumberE164, code }) as Promise<AuthTokens>;
}

export function refreshAccessToken(refreshToken: string): Promise<AuthTokens> {
  return apiClient.post<AuthTokens>('/api/v1/auth/refresh', { refreshToken }) as Promise<AuthTokens>;
}

export function logout(refreshToken: string): Promise<void> {
  return apiClient.post('/api/v1/auth/logout', { refreshToken }) as Promise<void>;
}

export function getMe(accessToken: string): Promise<MeResponse> {
  return apiClient.get<MeResponse>('/api/v1/auth/me', accessToken) as Promise<MeResponse>;
}
