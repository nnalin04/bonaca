import { IconDeviceMobile, IconPhone } from '@tabler/icons-react-native';
import { useRouter } from 'expo-router';
import { useMemo, useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Colors, Fonts, Radii } from '@/theme/tokens';

const PHONE_LENGTH = 10;

export function LoginScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [phoneNumber, setPhoneNumber] = useState('');

  const isValid = useMemo(() => phoneNumber.length === PHONE_LENGTH, [phoneNumber]);

  const handleContinue = () => {
    if (!isValid) return;
    router.push({ pathname: '/(auth)/otp', params: { phoneNumber } });
  };

  return (
    <KeyboardAvoidingView
      style={styles.screen}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={[styles.content, { paddingTop: insets.top + 48, paddingBottom: insets.bottom + 24 }]}>
        <View style={styles.iconBadge}>
          <IconDeviceMobile size={36} color={Colors.accent} strokeWidth={1.75} />
        </View>

        <Text style={styles.title}>What&apos;s your number?</Text>
        <Text style={styles.subtitle}>
          We&apos;ll send a one-time code to verify it&apos;s really you. No password needed.
        </Text>

        <View style={styles.inputRow}>
          <View style={styles.countryCode}>
            <IconPhone size={18} color={Colors.textSecondary} strokeWidth={1.75} />
            <Text style={styles.countryCodeText}>+91</Text>
          </View>
          <TextInput
            style={styles.input}
            value={phoneNumber}
            onChangeText={(text) => setPhoneNumber(text.replace(/[^0-9]/g, '').slice(0, PHONE_LENGTH))}
            placeholder="Mobile number"
            placeholderTextColor={Colors.textSecondary}
            keyboardType="number-pad"
            maxLength={PHONE_LENGTH}
            autoFocus
            accessibilityLabel="Mobile number"
          />
        </View>

        <View style={styles.spacer} />

        <Pressable
          style={[styles.cta, !isValid && styles.ctaDisabled]}
          onPress={handleContinue}
          disabled={!isValid}
          accessibilityRole="button"
          accessibilityLabel="Continue">
          <Text style={styles.ctaText}>Continue</Text>
        </Pressable>

        <Text style={styles.legal}>
          By continuing, you agree to Bonaca&apos;s Terms of Service and Privacy Policy.
        </Text>
      </View>
    </KeyboardAvoidingView>
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
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    height: 56,
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    paddingHorizontal: 16,
    gap: 12,
  },
  countryCode: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingRight: 12,
    borderRightWidth: 1,
    borderRightColor: Colors.cardBorder,
  },
  countryCodeText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    color: Colors.textPrimary,
  },
  input: {
    flex: 1,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    color: Colors.textPrimary,
    height: '100%',
  },
  spacer: {
    flex: 1,
  },
  cta: {
    height: 56,
    borderRadius: Radii.pill,
    backgroundColor: Colors.headerGradientEnd,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  ctaDisabled: {
    opacity: 0.4,
  },
  ctaText: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    color: Colors.white,
  },
  legal: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
});
