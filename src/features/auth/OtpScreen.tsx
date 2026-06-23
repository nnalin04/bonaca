import { LinearGradient } from 'expo-linear-gradient';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/AuthContext';
import { BackButton } from '@/features/auth/components/BackButton';
import { OtpInput } from '@/features/auth/components/OtpInput';
import { ApiError, requestOtp } from '@/lib/api';
import { Colors, Fonts, Radii } from '@/theme/tokens';

const RESEND_COUNTDOWN_SECONDS = 30;
const OTP_LENGTH = 4;

function formatCountdown(seconds: number) {
  const mm = Math.floor(seconds / 60)
    .toString()
    .padStart(2, '0');
  const ss = (seconds % 60).toString().padStart(2, '0');
  return `${mm}:${ss}`;
}

function formatOtpPhoneNumber(phoneNumber?: string) {
  if (!phoneNumber) return '';
  return phoneNumber.startsWith('+91') ? phoneNumber.slice(3) : phoneNumber;
}

export function OtpScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { login } = useAuth();
  const { phoneNumber } = useLocalSearchParams<{ phoneNumber: string }>();
  const [digits, setDigits] = useState<string[]>(['', '', '', '']);
  const [secondsLeft, setSecondsLeft] = useState(RESEND_COUNTDOWN_SECONDS);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);
  const displayPhoneNumber = formatOtpPhoneNumber(phoneNumber);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const timer = setInterval(() => {
      setSecondsLeft((value) => Math.max(0, value - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, [secondsLeft]);

  const handleChange = (next: string[]) => {
    setErrorMessage(null);
    setDigits(next);

    const code = next.join('');
    if (code.length === OTP_LENGTH) {
      void verify(code);
    }
  };

  const verify = async (code: string) => {
    setIsVerifying(true);
    try {
      const { profileCompleted } = await login(phoneNumber, code);
      router.push(profileCompleted ? '/(tabs)/home' : '/(auth)/complete-profile');
    } catch (error) {
      setErrorMessage('Enter a valid OTP');
      setDigits(['', '', '', '']);
    } finally {
      setIsVerifying(false);
    }
  };

  const handleResend = async () => {
    setSecondsLeft(RESEND_COUNTDOWN_SECONDS);
    setErrorMessage(null);
    setDigits(['', '', '', '']);
    try {
      await requestOtp(phoneNumber);
    } catch (error) {
      setErrorMessage(error instanceof ApiError ? error.message : 'Could not resend OTP. Please try again.');
    }
  };

  return (
    <View style={styles.screen}>
      <StatusBar style="light" />
      <LinearGradient
        colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
        locations={[0, 0.95]}
        start={{ x: 0.97, y: -0.43 }}
        end={{ x: 0.21, y: 1.21 }}
        style={styles.hero}>
        <View style={[styles.backRow, { top: insets.top + 16 }]}>
          <BackButton onPress={() => router.back()} />
        </View>
      </LinearGradient>

      <View style={styles.card}>
        <View style={styles.titleBlock}>
          <Text style={styles.title}>Verify OTP</Text>
          <Text style={styles.subtitle}>
            OTP sent to <Text style={styles.subtitleStrong}>{displayPhoneNumber}</Text>
          </Text>
        </View>

        <OtpInput value={digits} onChange={handleChange} hasError={errorMessage !== null} />

        {isVerifying && <ActivityIndicator />}
        {errorMessage && <Text style={styles.errorText}>{errorMessage}</Text>}

        <View style={errorMessage ? styles.statusAfterError : styles.statusBlock}>
          {secondsLeft > 0 ? (
            <Text style={styles.statusText}>Resend OTP in {formatCountdown(secondsLeft)}</Text>
          ) : (
            <Pressable onPress={handleResend} accessibilityRole="button" accessibilityLabel="Resend OTP">
              <Text style={styles.resendLink}>Resend OTP</Text>
            </Pressable>
          )}
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
    paddingHorizontal: 16,
    paddingTop: 24,
  },
  titleBlock: {
    width: 262,
    alignSelf: 'center',
    alignItems: 'center',
    gap: 12,
    marginBottom: 32,
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
    width: 262,
    alignSelf: 'center',
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.error,
    textAlign: 'center',
    marginTop: 16,
  },
  statusBlock: {
    width: 262,
    alignSelf: 'center',
    marginTop: 24,
  },
  statusAfterError: {
    width: 262,
    alignSelf: 'center',
    marginTop: 20,
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
