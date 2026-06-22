import { IconChevronLeft, IconCircleCheck, IconCreditCardPay } from '@tabler/icons-react-native';
import { useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/AuthContext';
import { useMembers } from '@/features/members';
import { ApiError, getSubscription, mockPay } from '@/lib/api';
import { Colors, Fonts, Radii } from '@/theme/tokens';
import type { SubscriptionResponse } from '@/types/subscriptions';

type PaymentGatewayVariant = 'trial-signup' | 'renewal' | 'active';

const copyByVariant: Record<PaymentGatewayVariant, { title: string; body: string; cta?: string }> = {
  'trial-signup': {
    title: 'Payment Gateway',
    body: 'Add a payment method to keep monitoring active after your free trial ends.',
    cta: 'Add Payment Method',
  },
  renewal: {
    title: 'Payment Gateway',
    body: 'Renew your subscription to keep monitoring all your connected family members.',
    cta: 'Renew Subscription',
  },
  active: {
    title: 'Payment Gateway',
    body: 'Your subscription is active — all your connected family members are being monitored.',
  },
};

function variantForStatus(status: SubscriptionResponse['status']): PaymentGatewayVariant {
  if (status === 'trial') return 'trial-signup';
  if (status === 'active') return 'active';
  return 'renewal';
}

export function PaymentGatewayScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { accessToken } = useAuth();
  const { self } = useMembers();
  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (!accessToken || !self) return;
    getSubscription(accessToken, self.accountId)
      .then((result) => {
        if (!cancelled) setSubscription(result);
      })
      .catch((error: unknown) => {
        if (!cancelled) setErrorMessage(error instanceof ApiError ? error.message : 'Could not load your subscription.');
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, self]);

  const handlePay = async () => {
    if (!accessToken || !self) return;
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      setSubscription(await mockPay(accessToken, self.accountId));
      router.back();
    } catch (error) {
      setErrorMessage(error instanceof ApiError ? error.message : 'Payment could not be completed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const variant = subscription ? variantForStatus(subscription.status) : 'trial-signup';
  const copy = copyByVariant[variant];

  return (
    <View style={styles.screen}>
      <View style={[styles.topBar, { paddingTop: insets.top + 12 }]}>
        <Pressable
          style={styles.backButton}
          onPress={() => router.back()}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="Go back">
          <IconChevronLeft size={24} color={Colors.textPrimary} strokeWidth={2} />
        </Pressable>
      </View>

      {isLoading ? (
        <ActivityIndicator style={styles.loading} />
      ) : (
        <View style={[styles.content, { paddingBottom: insets.bottom + 24 }]}>
          <Text style={styles.title}>{copy.title}</Text>
          {variant === 'active' ? (
            <IconCircleCheck size={64} color={Colors.accent} strokeWidth={1.5} />
          ) : (
            <IconCreditCardPay size={64} color={Colors.iconMuted} strokeWidth={1.5} />
          )}
          <Text style={styles.body}>{copy.body}</Text>

          {errorMessage ? (
            <Text style={styles.errorNote}>{errorMessage}</Text>
          ) : (
            copy.cta && (
              <Text style={styles.note}>
                Test mode — tapping {copy.cta.toLowerCase()} simulates a successful payment. Real
                payment processing isn&rsquo;t wired up yet.
              </Text>
            )
          )}

          {copy.cta && (
            <Pressable
              style={[styles.cta, isSubmitting && styles.ctaDisabled]}
              onPress={() => void handlePay()}
              disabled={isSubmitting}
              accessibilityRole="button"
              accessibilityLabel={copy.cta}>
              {isSubmitting ? (
                <ActivityIndicator color={Colors.white} />
              ) : (
                <Text style={styles.ctaLabel}>{copy.cta}</Text>
              )}
            </Pressable>
          )}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  topBar: {
    paddingHorizontal: 16,
    paddingBottom: 8,
  },
  backButton: {
    width: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  loading: {
    marginTop: 48,
  },
  content: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 32,
    gap: 16,
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 18,
    color: Colors.textSecondary,
  },
  body: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.textPrimary,
    textAlign: 'center',
  },
  note: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 13,
    lineHeight: 18,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
  errorNote: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 13,
    lineHeight: 18,
    color: Colors.error,
    textAlign: 'center',
  },
  cta: {
    marginTop: 16,
    height: 56,
    width: '100%',
    borderRadius: Radii.row,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  ctaDisabled: {
    opacity: 0.6,
  },
  ctaLabel: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 18,
    color: Colors.white,
  },
});
