import { IconChevronLeft, IconCreditCardPay } from '@tabler/icons-react-native';
import { useRouter } from 'expo-router';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Colors, Fonts, Radii } from '@/theme/tokens';

export type PaymentGatewayVariant = 'trial-signup' | 'renewal';

interface PaymentGatewayScreenProps {
  variant?: PaymentGatewayVariant;
}

const copyByVariant: Record<PaymentGatewayVariant, { title: string; body: string; cta: string }> = {
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
};

export function PaymentGatewayScreen({ variant = 'trial-signup' }: PaymentGatewayScreenProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
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

      <View style={[styles.content, { paddingBottom: insets.bottom + 24 }]}>
        <Text style={styles.title}>{copy.title}</Text>
        <IconCreditCardPay size={64} color={Colors.iconMuted} strokeWidth={1.5} />
        <Text style={styles.body}>{copy.body}</Text>
        <Text style={styles.note}>
          Payment processing (UPI, PayPal, American Express, Mastercard, Apple Pay) isn&rsquo;t
          wired up yet — this is a placeholder until the payment-method picker is built.
        </Text>

        <Pressable
          style={styles.cta}
          onPress={() => {
            // Stub: payment processing is not implemented yet (see CLAUDE.md Tech Stack).
            // Wiring to RevenueCat / StoreKit / Razorpay is a separate, explicitly-scoped task.
          }}
          accessibilityRole="button"
          accessibilityLabel={copy.cta}>
          <Text style={styles.ctaLabel}>{copy.cta}</Text>
        </Pressable>
      </View>
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
  cta: {
    marginTop: 16,
    height: 56,
    width: '100%',
    borderRadius: Radii.row,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  ctaLabel: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 18,
    color: Colors.white,
  },
});
