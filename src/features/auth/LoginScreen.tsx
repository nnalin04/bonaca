import { useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import {
  ActivityIndicator,
  Keyboard,
  KeyboardAvoidingView,
  Linking,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TouchableWithoutFeedback,
  View,
} from 'react-native';

import { AuthHero } from '@/features/auth/components/AuthHero';
import { MobileNumberField } from '@/features/auth/components/MobileNumberField';
import { PrimaryButton } from '@/features/auth/components/PrimaryButton';
import { useLoginOtpRequest } from '@/features/auth/hooks/useLoginOtpRequest';
import { Colors, Fonts, Radii } from '@/theme/tokens';

export function LoginScreen() {
  const router = useRouter();
  const {
    mobileNumber,
    setMobileNumber,
    isSubmitting,
    errorMessage,
    canSubmit,
    requestLoginOtp,
  } = useLoginOtpRequest();

  const handleSubmit = async () => {
    const phoneNumberE164 = await requestLoginOtp();
    if (phoneNumberE164) {
      router.push({
        pathname: '/(auth)/otp',
        params: { phoneNumber: phoneNumberE164 },
      });
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.screen}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <TouchableWithoutFeedback onPress={Keyboard.dismiss} accessible={false}>
        <View style={styles.dismissArea}>
          <StatusBar style="light" />
          <AuthHero
            tagline={'Stay gently connected to your\nfamily’s daily wellbeing'}
            height={464}
            contentTop={232}
            contentGap={32}
          />

          <View style={styles.card}>
            <View style={styles.titleBlock}>
              <Text style={styles.title}>Login or Signup</Text>
              <Text style={styles.subtitle}>
                Enter your mobile number to get started
              </Text>
            </View>

            <MobileNumberField
              countryCode="+91"
              value={mobileNumber}
              onChangeText={setMobileNumber}
            />
            {errorMessage && (
              <Text style={styles.errorText}>{errorMessage}</Text>
            )}

            <View style={styles.ctaBlock}>
              {isSubmitting ? (
                <ActivityIndicator />
              ) : (
                <PrimaryButton
                  label="Send OTP"
                  disabled={!canSubmit}
                  onPress={handleSubmit}
                />
              )}
              <Pressable
                accessibilityRole="link"
                accessibilityLabel="Privacy Policy"
                onPress={() => Linking.openURL('https://bonaca.in/privacy')}
              >
                <Text style={styles.privacyLink}>Privacy Policy</Text>
              </Pressable>
            </View>
          </View>
        </View>
      </TouchableWithoutFeedback>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: Colors.headerGradientEnd,
  },
  dismissArea: {
    flex: 1,
  },
  card: {
    flex: 1,
    backgroundColor: Colors.white,
    borderTopLeftRadius: Radii.cardTop,
    borderTopRightRadius: Radii.cardTop,
    paddingHorizontal: 16,
    paddingTop: 24,
    gap: 32,
  },
  titleBlock: {
    width: 262,
    alignSelf: 'center',
    alignItems: 'center',
    gap: 12,
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
  errorText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 13,
    lineHeight: 18,
    color: Colors.error,
    textAlign: 'center',
  },
  ctaBlock: {
    width: '100%',
    marginTop: 26,
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
