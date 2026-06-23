import { useRouter } from 'expo-router';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { SubscriptionHeader } from '@/features/subscription/components/SubscriptionHeader';
import { useCurrentSubscription } from '@/features/subscription/hooks/useCurrentSubscription';
import { Colors, Fonts, Radii } from '@/theme/tokens';

interface PaymentGatewayScreenProps {
  accountId?: string;
}

export function PaymentGatewayScreen({ accountId }: PaymentGatewayScreenProps) {
  const router = useRouter();
  const {
    subscription,
    isLoading,
    isPaying,
    errorMessage,
    activateMockPayment,
  } = useCurrentSubscription(accountId);
  const isActive = subscription?.status === 'active';

  const handlePay = async () => {
    if (isActive) {
      router.replace('/subscription');
      return;
    }

    const activated = await activateMockPayment();
    if (activated) router.replace('/subscription');
  };

  return (
    <View style={styles.screen}>
      <SubscriptionHeader
        title="Payment Gateway"
        onPressBack={() => router.back()}
      />

      <View style={styles.content}>
        <View style={styles.card}>
          <Text style={styles.eyebrow}>Bonaca subscription</Text>
          <Text style={styles.price}>₹249</Text>
          <Text style={styles.period}>per month</Text>
          <Text style={styles.description}>
            Keeps wearable sync, family sharing, and health insights active for
            this account.
          </Text>

          {isLoading ? (
            <ActivityIndicator style={styles.loader} />
          ) : (
            <Text style={styles.currentStatus}>
              Current status: {subscription?.status ?? 'Unavailable'}
            </Text>
          )}

          {errorMessage ? (
            <Text style={styles.error}>{errorMessage}</Text>
          ) : null}

          <Pressable
            style={[
              styles.payButton,
              (isPaying || isActive) && styles.payButtonDisabled,
            ]}
            onPress={handlePay}
            disabled={isPaying || isLoading}
            accessibilityRole="button"
            accessibilityLabel={
              isActive ? 'Subscription active' : 'Activate subscription at ₹249'
            }
          >
            {isPaying ? (
              <ActivityIndicator color={Colors.white} />
            ) : (
              <Text style={styles.payText}>
                {isActive ? 'Subscription active' : 'Activate at ₹249'}
              </Text>
            )}
          </Pressable>

          <Text style={styles.helperText}>
            Development build: this uses the backend mock payment endpoint until
            the real processor is added.
          </Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  content: {
    flex: 1,
    paddingHorizontal: 16,
    paddingTop: 24,
  },
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
  loader: {
    marginTop: 20,
  },
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
  payButtonDisabled: {
    opacity: 0.7,
  },
  payText: {
    fontFamily: Fonts.family,
    fontWeight: '700',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.white,
  },
  helperText: {
    marginTop: 16,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 18,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
});
