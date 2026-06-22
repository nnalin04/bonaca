import { apiClient } from '@/lib/api/client';
import type { SubscriptionResponse } from '@/types/subscriptions';

export function getSubscription(accessToken: string, accountId: string): Promise<SubscriptionResponse> {
  return apiClient.get<SubscriptionResponse>(
    `/api/v1/accounts/${accountId}/subscription`,
    accessToken
  ) as Promise<SubscriptionResponse>;
}

/**
 * Dev-only stand-in for real payment processing — see
 * docs/TECHNICAL/BACKEND_STATUS_AND_NEXT_STEPS.md §4. Immediately activates the subscription, no
 * card details, no processor. The backend endpoint this calls is itself gated to never run in a
 * "prod" Spring profile.
 */
export function mockPay(accessToken: string, accountId: string): Promise<SubscriptionResponse> {
  return apiClient.post<SubscriptionResponse>(
    `/api/v1/accounts/${accountId}/subscription/mock-pay`,
    undefined,
    accessToken
  ) as Promise<SubscriptionResponse>;
}
