import { apiClient } from '@/lib/api/client';
import type { PaymentLinkResponse } from '@/types/payment';

export function getPaymentLink(
  accessToken: string,
  accountId: string,
): Promise<PaymentLinkResponse> {
  return apiClient.post<PaymentLinkResponse>(
    `/api/v1/accounts/${accountId}/subscription/payment-link`,
    undefined,
    accessToken,
  ) as Promise<PaymentLinkResponse>;
}
