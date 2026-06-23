import { LinearGradient } from 'expo-linear-gradient';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BackButton } from '@/features/auth/components/BackButton';
import { OtpInput } from '@/features/auth/components/OtpInput';
import {
  formatCountdown,
  useOtpVerification,
} from '@/features/auth/hooks/useOtpVerification';
import { Colors, Fonts, Radii } from '@/theme/tokens';

export function OtpScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { phoneNumber } = useLocalSearchParams<{ phoneNumber: string }>();
  const {
    digits,
    secondsLeft,
    errorMessage,
    isVerifying,
    displayPhoneNumber,
    handleChange,
    handleResend,
  } = useOtpVerification({
    phoneNumber,
    onVerified: (profileCompleted) =>
      router.push(
        profileCompleted ? '/(tabs)/home' : '/(auth)/complete-profile',
      ),
  });

  return (
    <View style={styles.screen}>
      <StatusBar style="light" />
      <LinearGradient
        colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
        locations={[0, 0.95]}
        start={{ x: 0.97, y: -0.43 }}
        end={{ x: 0.21, y: 1.21 }}
        style={styles.hero}
      >
        <View style={[styles.backRow, { top: insets.top + 16 }]}>
          <BackButton onPress={() => router.back()} />
        </View>
      </LinearGradient>

      <View style={styles.card}>
        <View style={styles.titleBlock}>
          <Text style={styles.title}>Verify OTP</Text>
          <Text style={styles.subtitle}>
            OTP sent to{' '}
            <Text style={styles.subtitleStrong}>{displayPhoneNumber}</Text>
          </Text>
        </View>

        <OtpInput
          value={digits}
          onChange={handleChange}
          hasError={errorMessage !== null}
        />

        {isVerifying && <ActivityIndicator />}
        {errorMessage && <Text style={styles.errorText}>{errorMessage}</Text>}

        <View
          style={errorMessage ? styles.statusAfterError : styles.statusBlock}
        >
          {secondsLeft > 0 ? (
            <Text style={styles.statusText}>
              Resend OTP in {formatCountdown(secondsLeft)}
            </Text>
          ) : (
            <Pressable
              onPress={handleResend}
              accessibilityRole="button"
              accessibilityLabel="Resend OTP"
            >
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
