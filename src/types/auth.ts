// Auth API request/response DTOs — shaped to match the backend exactly (backend/...AuthController).
// Kept separate from the hand-written domain types in src/types/index.ts, same separation the
// project already uses between database types and domain types.

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  profileCompleted: boolean;
}

export interface MeResponse {
  userId: string;
  phoneNumber: string;
  profileCompleted: boolean;
}

export interface ApiErrorResponse {
  message: string;
}
