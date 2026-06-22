// Subscriptions API response DTOs — shaped to match the backend exactly
// (backend/...subscriptions/SubscriptionsController, MockPaymentController).
// Kept separate from the hand-written domain types in src/types/index.ts, same separation
// the project already uses between database types and domain types (see types/auth.ts).

import type { SubscriptionStatus } from '@/types';

export interface SubscriptionResponse {
  id: string;
  accountId: string;
  status: SubscriptionStatus;
  trialEndsAt: string | null;
  renewedAt: string | null;
}
