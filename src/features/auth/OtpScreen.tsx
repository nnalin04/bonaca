import { LinearGradient } from 'expo-linear-gradient';
import { useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BackButton } from '@/features/auth/components/BackButton';
import { OtpInput } from '@/features/auth/components/OtpInput';
import { Colors, Fonts, Radii } from '@/theme/tokens';

const RESEND_COUNTDOWN_SECONDS = 30;
const MOBILE_NUMBER = '9742657712';
const OTP_LENGTH = 4;

function formatCountdown(seconds: number) {
  const mm = Math.floor(seconds / 60)
    .toString()
    .padStart(2, '0');
  const ss = (seconds % 60).toString().padStart(2, '0');
  return `${mm}:${ss}`;
}

export function OtpScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [digits, setDigits] = useState<string[]>(['', '', '', '']);
  const [secondsLeft, setSecondsLeft] = useState(RESEND_COUNTDOWN_SECONDS);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const timer = setInterval(() => {
      setSecondsLeft((value) => Math.max(0, value - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, [secondsLeft]);

  const handleChange = (next: string[]) => {
    setHasError(false);
    setDigits(next);

    const code = next.join('');
    if (code.length === OTP_LENGTH) {
      // Stubbed verification: any code other than 1234 is treated as incorrect.
      if (code === '1234') {
        router.push('/(auth)/complete-profile');
      } else {
        setHasError(true);
      }
    }
  };

  const handleResend = () => {
    setSecondsLeft(RESEND_COUNTDOWN_SECONDS);
    setHasError(false);
    setDigits(['', '', '', '']);
  };

  return (
    <View style={styles.screen}>
      <LinearGradient
        colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
        locations={[0.03, 0.81]}
        start={{ x: 0.95, y: 0.29 }}
        end={{ x: 0.05, y: 0.71 }}
        style={styles.hero}>
        <View style={[styles.backRow, { top: insets.top + 8 }]}>
          <BackButton onPress={() => router.back()} />
        </View>
      </LinearGradient>

      <View style={styles.card}>
        <View style={styles.titleBlock}>
          <Text style={styles.title}>Verify OTP</Text>
          <Text style={styles.subtitle}>
            OTP sent to <Text style={styles.subtitleStrong}>{MOBILE_NUMBER}</Text>
          </Text>
        </View>

        <OtpInput value={digits} onChange={handleChange} hasError={hasError} />

        {hasError && <Text style={styles.errorText}>Enter a valid OTP</Text>}

        {secondsLeft > 0 ? (
          <Text style={styles.statusText}>Resend OTP in {formatCountdown(secondsLeft)}</Text>
        ) : (
          <Pressable onPress={handleResend} accessibilityRole="button" accessibilityLabel="Resend OTP">
            <Text style={styles.resendLink}>Resend OTP</Text>
          </Pressable>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  hero: {
    height: 254,
  },
  backRow: {
    position: 'absolute',
    left: 16,
  },
  card: {
    flex: 1,
    backgroundColor: Colors.white,
    borderTopLeftRadius: Radii.cardTop,
    borderTopRightRadius: Radii.cardTop,
    marginTop: -48,
    paddingHorizontal: 16,
    paddingTop: 24,
    gap: 24,
  },
  titleBlock: {
    alignItems: 'center',
    gap: 4,
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 20,
    lineHeight: 28,
    color: Colors.textPrimary,
  },
  subtitle: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
  },
  subtitleStrong: {
    fontWeight: '500',
    color: Colors.textPrimary,
  },
  statusText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
  errorText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.error,
    textAlign: 'center',
    marginTop: -12,
  },
  resendLink: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.accent,
    textAlign: 'center',
  },
});
