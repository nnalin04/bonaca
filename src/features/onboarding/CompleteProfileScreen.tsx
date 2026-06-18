import { IconCamera, IconUserCircle } from '@tabler/icons-react-native';
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

export function CompleteProfileScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [name, setName] = useState('');

  const isValid = useMemo(() => name.trim().length > 1, [name]);

  const handleContinue = () => {
    if (!isValid) return;
    router.push('/(auth)/connect-wearable');
  };

  return (
    <KeyboardAvoidingView
      style={styles.screen}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={[styles.content, { paddingTop: insets.top + 48, paddingBottom: insets.bottom + 24 }]}>
        <Text style={styles.title}>Complete your profile</Text>
        <Text style={styles.subtitle}>Just a couple of details so your family knows it&apos;s you.</Text>

        <View style={styles.avatarWrap}>
          <Pressable
            style={styles.avatarCircle}
            accessibilityRole="button"
            accessibilityLabel="Add profile photo">
            <IconUserCircle size={56} color={Colors.iconMuted} strokeWidth={1.5} />
            <View style={styles.cameraBadge}>
              <IconCamera size={16} color={Colors.white} strokeWidth={1.75} />
            </View>
          </Pressable>
        </View>

        <Text style={styles.label}>Full name</Text>
        <View style={styles.inputRow}>
          <TextInput
            style={styles.input}
            value={name}
            onChangeText={setName}
            placeholder="Enter your full name"
            placeholderTextColor={Colors.textSecondary}
            autoFocus
            accessibilityLabel="Full name"
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
  avatarWrap: {
    alignItems: 'center',
    marginBottom: 32,
  },
  avatarCircle: {
    width: 96,
    height: 96,
    borderRadius: 48,
    backgroundColor: Colors.white,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cameraBadge: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: Colors.accent,
    borderWidth: 2,
    borderColor: Colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  label: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 13,
    color: Colors.textSecondary,
    marginBottom: 8,
  },
  inputRow: {
    height: 56,
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    paddingHorizontal: 16,
    justifyContent: 'center',
  },
  input: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    color: Colors.textPrimary,
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
});
