import { IconArrowLeft, IconShieldCheck } from '@tabler/icons-react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useEffect, useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { OtpInput } from '@/features/auth/components/OtpInput';
import { Colors, Fonts } from '@/theme/tokens';

const OTP_LENGTH = 6;
const RESEND_COOLDOWN_SECONDS = 30;
// Stubbed correct code until OTP delivery (MSG91, per docs/TECHNICAL_REQUIREMENTS.md) is wired up.
const STUB_VALID_OTP = '123456';

export function OtpScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { phoneNumber } = useLocalSearchParams<{ phoneNumber?: string }>();

  const [otp, setOtp] = useState('');
  const [hasError, setHasError] = useState(false);
  const [cooldown, setCooldown] = useState(RESEND_COOLDOWN_SECONDS);

  const maskedPhone = useMemo(
    () => (phoneNumber ? `+91 ${phoneNumber}` : 'your number'),
    [phoneNumber],
  );

  useEffect(() => {
    if (cooldown === 0) return;
    const timer = setInterval(() => setCooldown((s) => Math.max(0, s - 1)), 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  const handleChangeOtp = (next: string) => {
    setOtp(next);
    setHasError(false);

    if (next.length === OTP_LENGTH) {
      if (next === STUB_VALID_OTP) {
        router.push('/(auth)/complete-profile');
      } else {
        setHasError(true);
      }
    }
  };

  const handleResend = () => {
    if (cooldown > 0) return;
    setOtp('');
    setHasError(false);
    setCooldown(RESEND_COOLDOWN_SECONDS);
  };

  return (
    <View style={styles.screen}>
      <View style={[styles.content, { paddingTop: insets.top + 16, paddingBottom: insets.bottom + 24 }]}>
        <Pressable
          style={styles.backButton}
          onPress={() => router.back()}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="Back">
          <IconArrowLeft size={24} color={Colors.textPrimary} strokeWidth={1.75} />
        </Pressable>

        <View style={styles.iconBadge}>
          <IconShieldCheck size={36} color={Colors.accent} strokeWidth={1.75} />
        </View>

        <Text style={styles.title}>Enter the code</Text>
        <Text style={styles.subtitle}>We sent a 6-digit code to {maskedPhone}</Text>

        <View style={styles.otpBlock}>
          <OtpInput length={OTP_LENGTH} value={otp} onChange={handleChangeOtp} hasError={hasError} />
          {hasError && <Text style={styles.errorText}>Incorrect code. Please try again.</Text>}
        </View>

        <View style={styles.resendRow}>
          <Text style={styles.resendLabel}>Didn&apos;t get a code?</Text>
          <Pressable onPress={handleResend} disabled={cooldown > 0} hitSlop={8}>
            <Text style={[styles.resendAction, cooldown > 0 && styles.resendActionDisabled]}>
              {cooldown > 0 ? `Resend in ${cooldown}s` : 'Resend OTP'}
            </Text>
          </Pressable>
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
    paddingHorizontal: 24,
  },
  backButton: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  iconBadge: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: Colors.white,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 24,
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 24,
    lineHeight: 32,
    color: Colors.textPrimary,
    marginBottom: 8,
  },
  subtitle: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
    marginBottom: 32,
  },
  otpBlock: {
    gap: 12,
  },
  errorText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 13,
    color: Colors.badge,
  },
  resendRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 6,
    marginTop: 32,
  },
  resendLabel: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    color: Colors.textSecondary,
  },
  resendAction: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 14,
    color: Colors.accent,
  },
  resendActionDisabled: {
    color: Colors.textSecondary,
  },
});
