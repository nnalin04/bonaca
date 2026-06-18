import { useRouter } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { AuthHero } from '@/features/auth/components/AuthHero';
import { MobileNumberField } from '@/features/auth/components/MobileNumberField';
import { PrimaryButton } from '@/features/auth/components/PrimaryButton';
import { Colors, Fonts, Radii } from '@/theme/tokens';

export function LoginScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [mobileNumber, setMobileNumber] = useState('');

  const canSubmit = mobileNumber.length === 10;

  return (
    <KeyboardAvoidingView
      style={styles.screen}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <AuthHero
        tagline={'Stay gently connected to your\nfamily’s daily wellbeing'}
        height={464}
      />

      <View style={styles.card}>
        <View style={styles.titleBlock}>
          <Text style={styles.title}>Login or Signup</Text>
          <Text style={styles.subtitle}>Enter your mobile number to get started</Text>
        </View>

        <MobileNumberField countryCode="+91" value={mobileNumber} onChangeText={setMobileNumber} />

        <View style={[styles.ctaBlock, { paddingBottom: insets.bottom + 16 }]}>
          <PrimaryButton
            label="Send OTP"
            disabled={!canSubmit}
            onPress={() => router.push('/(auth)/otp')}
          />
          <Pressable accessibilityRole="link" accessibilityLabel="Privacy Policy">
            <Text style={styles.privacyLink}>Privacy Policy</Text>
          </Pressable>
        </View>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: Colors.headerGradientEnd,
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
  ctaBlock: {
    marginTop: 'auto',
    gap: 24,
    alignItems: 'center',
  },
  privacyLink: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
    textDecorationLine: 'underline',
  },
});
