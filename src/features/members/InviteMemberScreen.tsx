import { useRouter } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/AuthContext';
import { MobileNumberField } from '@/features/auth/components/MobileNumberField';
import { PrimaryButton } from '@/features/auth/components/PrimaryButton';
import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { ApiError, createInvite } from '@/lib/api';
import { Colors, Fonts } from '@/theme/tokens';

/**
 * Adapted from Figma's "Invite a Family Member" (node 222:1723), which designs a
 * device-contacts picker with a per-contact "Invite" button. Built as manual phone
 * entry instead — expo-contacts isn't installed and CLAUDE.md says not to add new
 * SDKs without an explicit task; this keeps the same header/title and end result
 * (an invite sent to a phone number) without the contacts permission dependency.
 *
 * No role picker — per docs/PRD.pdf §2.1/§9 Flow C, Secondary Member is the only
 * role a Primary can invite (up to 2 per account, enforced server-side).
 */
export function InviteMemberScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { accessToken } = useAuth();
  const [mobileNumber, setMobileNumber] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const canSubmit = mobileNumber.length === 10 && !isSubmitting;

  const handleSubmit = async () => {
    if (!accessToken) return;
    setIsSubmitting(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await createInvite(accessToken, { phoneNumber: `+91${mobileNumber}` });
      setSuccessMessage('Invite sent successfully');
      setMobileNumber('');
    } catch (error) {
      setErrorMessage(error instanceof ApiError ? error.message : 'Could not send the invite. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <View style={styles.screen}>
      <ProfileHeader title="Invite a Family Member" onPressBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        <Text style={styles.subtitle}>
          Invite a family member to view your shared health and routine updates. You can share with up to 2 family
          members.
        </Text>

        <MobileNumberField countryCode="+91" value={mobileNumber} onChangeText={setMobileNumber} />

        {errorMessage && <Text style={styles.errorText}>{errorMessage}</Text>}
        {successMessage && <Text style={styles.successText}>{successMessage}</Text>}

        {isSubmitting ? (
          <ActivityIndicator />
        ) : (
          <PrimaryButton label="Send Invite" disabled={!canSubmit} onPress={() => void handleSubmit()} />
        )}
      </ScrollView>
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
    paddingTop: 24,
    gap: 20,
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
    fontSize: 13,
    lineHeight: 18,
    color: Colors.error,
    textAlign: 'center',
  },
  successText: {
    fontFamily: Fonts.family,
    fontSize: 13,
    lineHeight: 18,
    color: Colors.accent,
    textAlign: 'center',
  },
});
