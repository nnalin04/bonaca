import { IconLock } from '@tabler/icons-react-native';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PaymentMethodOption } from '@/features/subscription/components/PaymentMethodOption';
import { PlanSummaryCard } from '@/features/subscription/components/PlanSummaryCard';
import { SubscriptionFlowHeader } from '@/features/subscription/components/SubscriptionFlowHeader';
import { Colors, Fonts, Radii } from '@/theme/tokens';
import type { PaymentMethod } from '@/types';

// Payment methods per docs/PRD.md §4 "Connecting a Wearable" / §3.6 subscription lifecycle:
// UPI, PayPal, American Express, Mastercard, Apple Pay — international cards + Apple Pay are
// live from day one (NRI-diaspora segment), UPI covers the India-resident segment.
const PAYMENT_METHODS: PaymentMethod[] = [
  { id: 'pm-upi', type: 'upi', label: 'UPI', detail: 'Pay directly via any UPI app' },
  { id: 'pm-paypal', type: 'paypal', label: 'PayPal', detail: 'Pay using your PayPal balance or card' },
  { id: 'pm-amex', type: 'amex', label: 'American Express', detail: 'Ending in 4242' },
  { id: 'pm-mastercard', type: 'mastercard', label: 'Mastercard', detail: 'Ending in 4242' },
  { id: 'pm-apple-pay', type: 'apple-pay', label: 'Apple Pay', detail: 'Use Face ID to pay' },
];

export function PaymentGatewayScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [selectedMethodId, setSelectedMethodId] = useState<string>(PAYMENT_METHODS[0].id);

  const handlePay = () => {
    // Payments are not implemented yet (RevenueCat + StoreKit/Play Billing/Razorpay are
    // decided-but-not-built per docs/TECHNICAL_REQUIREMENTS.md). This is a UI-only stub —
    // no charge is made, no SDK is called. Navigate back to Home as a stand-in for a
    // successful-subscription confirmation.
    router.replace('/(tabs)/home');
  };

  return (
    <View style={styles.screen}>
      <SubscriptionFlowHeader title="Payment Gateway" onPressBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        <PlanSummaryCard
          planName="Bonaca Family Plan"
          priceLabel="₹499/mo"
          billingCycleLabel="Billed monthly · cancel anytime"
        />

        <Text style={styles.sectionTitle}>Choose payment method</Text>

        <View style={styles.list}>
          {PAYMENT_METHODS.map((method) => (
            <PaymentMethodOption
              key={method.id}
              method={method}
              selected={selectedMethodId === method.id}
              onPress={() => setSelectedMethodId(method.id)}
            />
          ))}
        </View>

        <View style={styles.secureNotice}>
          <IconLock size={14} color={Colors.textSecondary} strokeWidth={1.75} />
          <Text style={styles.secureNoticeText}>Payments are encrypted and securely processed</Text>
        </View>
      </ScrollView>

      <View style={[styles.footer, { paddingBottom: insets.bottom + 16 }]}>
        <Pressable
          style={({ pressed }) => [styles.payButton, pressed && styles.payButtonPressed]}
          onPress={handlePay}
          accessibilityRole="button"
          accessibilityLabel="Pay and subscribe">
          <Text style={styles.payButtonText}>Pay & Subscribe</Text>
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
  content: {
    paddingHorizontal: 16,
    paddingTop: 8,
    gap: 16,
  },
  sectionTitle: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textPrimary,
  },
  list: {
    gap: 10,
  },
  secureNotice: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    marginTop: 4,
  },
  secureNoticeText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
  footer: {
    paddingHorizontal: 16,
    paddingTop: 12,
    backgroundColor: Colors.background,
    borderTopWidth: 1,
    borderTopColor: Colors.cardBorder,
  },
  payButton: {
    height: 52,
    borderRadius: Radii.pill,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  payButtonPressed: {
    opacity: 0.85,
  },
  payButtonText: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.white,
  },
});
