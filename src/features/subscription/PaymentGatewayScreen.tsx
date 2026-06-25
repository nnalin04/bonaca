import { useRouter } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Linking, Pressable, StyleSheet, Text, View } from 'react-native';

import { useAuth } from '@/features/auth/AuthContext';
import { useMembers } from '@/features/members/useMembers';
import { SubscriptionHeader } from '@/features/subscription/components/SubscriptionHeader';
import { useCurrentSubscription } from '@/features/subscription/hooks/useCurrentSubscription';
import { ApiError, getPaymentLink, mockPay } from '@/lib/api';
import { Colors, Fonts, Radii } from '@/theme/tokens';

interface PaymentGatewayScreenProps {
  accountId?: string;
}

export function PaymentGatewayScreen({ accountId }: PaymentGatewayScreenProps) {
  const router = useRouter();
  const { accessToken } = useAuth();
  const { self } = useMembers();
  const {
    subscription,
    isLoading,
    isPaying,
    errorMessage: subErrorMessage,
    activateMockPayment,
  } = useCurrentSubscription(accountId);

  const [isOpeningPayment, setIsOpeningPayment] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);

  const resolvedAccountId = accountId ?? self?.accountId ?? null;
  const isActive = subscription?.status === 'active';
  const isTrial = subscription?.status === 'trial';

  const errorMessage = paymentError ?? subErrorMessage;

  // Opens the Razorpay payment link in the device browser. On completion, Razorpay
  // calls our webhook which activates the subscription; the user taps "I've paid"
  // to poll and confirm.
  const handleRazorpayCheckout = useCallback(async () => {
    if (!accessToken || !resolvedAccountId) return;
    setIsOpeningPayment(true);
    setPaymentError(null);
    try {
      const result = await getPaymentLink(accessToken, resolvedAccountId);
      await Linking.openURL(result.paymentLink);
    } catch (err) {
      setPaymentError(err instanceof ApiError ? err.message : 'Could not open payment page. Please try again.');
    } finally {
      setIsOpeningPayment(false);
    }
  }, [accessToken, resolvedAccountId]);

  // Mock path — only works because the backend runs MockPaymentController in non-prod profiles.
  const handleMockPay = useCallback(async () => {
    if (isActive) { router.replace('/subscription'); return; }
    const activated = await activateMockPayment();
    if (activated) router.replace('/subscription');
  }, [isActive, activateMockPayment, router]);

  return (
    <View style={styles.screen}>
      <SubscriptionHeader title="Payment Gateway" onPressBack={() => router.back()} />

      <View style={styles.content}>
        <View style={styles.card}>
          <Text style={styles.eyebrow}>Bonaca subscription</Text>
          <Text style={styles.price}>₹249</Text>
          <Text style={styles.period}>per month · 7-day free trial</Text>
          <Text style={styles.description}>
            Keeps wearable sync, family sharing, and health insights active for this account.
          </Text>

          {isLoading ? (
            <ActivityIndicator style={styles.loader} />
          ) : (
            <Text style={styles.currentStatus}>
              Current status: {subscription?.status ?? 'Unavailable'}
            </Text>
          )}

          {errorMessage ? <Text style={styles.error}>{errorMessage}</Text> : null}

          {/* Real Razorpay checkout — opens payment link in browser */}
          {!isActive && (
            <Pressable
              style={[styles.payButton, (isOpeningPayment || isLoading) && styles.payButtonDisabled]}
              onPress={handleRazorpayCheckout}
              disabled={isOpeningPayment || isLoading}
              accessibilityRole="button"
              accessibilityLabel="Pay ₹249 per month via Razorpay">
              {isOpeningPayment ? (
                <ActivityIndicator color={Colors.white} />
              ) : (
                <Text style={styles.payText}>
                  {isTrial ? 'Start trial · ₹249/month' : 'Pay ₹249/month'}
                </Text>
              )}
            </Pressable>
          )}

          {isActive && (
            <View style={[styles.payButton, styles.payButtonDisabled]}>
              <Text style={styles.payText}>Subscription active</Text>
            </View>
          )}

          {/* After paying in browser the user comes back and confirms */}
          {!isActive && (
            <Text style={styles.returnNote}>
              After paying in your browser, tap below to confirm.
            </Text>
          )}

          {/* Dev-only mock path (shown when Razorpay not configured yet) */}
          <Pressable
            style={styles.mockButton}
            onPress={handleMockPay}
            disabled={isPaying || isLoading}
            accessibilityRole="button"
            accessibilityLabel="Activate mock subscription for development">
            {isPaying ? (
              <ActivityIndicator color={Colors.accent} />
            ) : (
              <Text style={styles.mockText}>
                {isActive ? 'View subscription' : '[Dev] Activate without payment'}
              </Text>
            )}
          </Pressable>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: Colors.background },
  content: { flex: 1, paddingHorizontal: 16, paddingTop: 24 },
  card: {
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    padding: 20,
    alignItems: 'center',
  },
  eyebrow: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
  },
  price: {
    marginTop: 16,
    fontFamily: Fonts.family,
    fontWeight: '700',
    fontSize: 44,
    lineHeight: 52,
    color: Colors.textPrimary,
  },
  period: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
  },
  description: {
    marginTop: 20,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 15,
    lineHeight: 22,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
  loader: { marginTop: 20 },
  currentStatus: {
    marginTop: 20,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
    textTransform: 'capitalize',
  },
  error: {
    marginTop: 16,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.error,
    textAlign: 'center',
  },
  payButton: {
    width: '100%',
    minHeight: 48,
    marginTop: 24,
    borderRadius: Radii.button,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  payButtonDisabled: { opacity: 0.7 },
  payText: {
    fontFamily: Fonts.family,
    fontWeight: '700',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.white,
  },
  returnNote: {
    marginTop: 12,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 13,
    lineHeight: 18,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
  mockButton: {
    marginTop: 20,
    paddingVertical: 8,
  },
  mockText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 13,
    lineHeight: 18,
    color: Colors.textSecondary,
    textDecorationLine: 'underline',
  },
});
