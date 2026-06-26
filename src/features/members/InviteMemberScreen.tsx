import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useInviteMember } from '@/features/members/hooks/useInviteMember';
import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { Colors, Fonts, Radii } from '@/theme/tokens';
import type { InviteResponse } from '@/types/members';

export function InviteMemberScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [phoneInput, setPhoneInput] = useState('');
  const { invites, pendingPhone, errorMessage, inviteContact } = useInviteMember();

  const normalizedInput = phoneInput.trim().replace(/\s/g, '');
  const canInvite =
    normalizedInput.length >= 10 &&
    pendingPhone !== normalizedInput &&
    pendingPhone !== `+91${normalizedInput}`;

  const handleInvite = async () => {
    if (!canInvite) return;
    await inviteContact(normalizedInput);
    setPhoneInput('');
  };

  return (
    <View style={styles.screen}>
      <ProfileHeader
        title="Invite a Family Member"
        onPressBack={() => router.back()}
      />

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + 24 },
        ]}
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.subtitle}>
          Enter the mobile number of the family member you want to invite.
        </Text>

        {errorMessage ? (
          <Text style={styles.errorText}>{errorMessage}</Text>
        ) : null}

        <View style={styles.inputRow}>
          <View style={styles.countryCode}>
            <Text style={styles.countryCodeText}>+91</Text>
          </View>
          <TextInput
            style={styles.input}
            value={phoneInput}
            onChangeText={setPhoneInput}
            placeholder="Mobile number"
            placeholderTextColor={Colors.placeholderText}
            keyboardType="number-pad"
            maxLength={10}
            returnKeyType="done"
            onSubmitEditing={handleInvite}
          />
          <Pressable
            style={[styles.inviteButton, !canInvite && styles.inviteButtonDisabled]}
            onPress={handleInvite}
            disabled={!canInvite || Boolean(pendingPhone)}
            accessibilityRole="button"
            accessibilityLabel="Send invite"
          >
            {pendingPhone ? (
              <ActivityIndicator size="small" color={Colors.white} />
            ) : (
              <Text style={styles.inviteButtonText}>Invite</Text>
            )}
          </Pressable>
        </View>

        {invites.length > 0 && (
          <>
            <Text style={styles.sentTitle}>Sent Invites</Text>
            <View style={styles.inviteList}>
              {invites.map((invite) => (
                <SentInviteRow key={invite.id} invite={invite} />
              ))}
            </View>
          </>
        )}
      </ScrollView>
    </View>
  );
}

function SentInviteRow({ invite }: { invite: InviteResponse }) {
  const statusLabel =
    invite.status === 'pending'
      ? 'Invited'
      : invite.status === 'accepted'
        ? 'Joined'
        : invite.status === 'expired'
          ? 'Expired'
          : invite.status;

  const statusColor =
    invite.status === 'accepted'
      ? Colors.connectedChipText
      : invite.status === 'expired'
        ? Colors.error
        : Colors.textSecondary;

  const initial = invite.phoneNumber ? invite.phoneNumber.slice(-4, -3) : '?';

  return (
    <View style={styles.inviteRow}>
      <View style={styles.avatarFallback}>
        <Text style={styles.avatarInitial}>{initial.toUpperCase()}</Text>
      </View>
      <View style={styles.inviteText}>
        <Text style={styles.invitePhone}>{invite.phoneNumber}</Text>
        <Text style={[styles.inviteStatus, { color: statusColor }]}>{statusLabel}</Text>
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
    paddingTop: 20,
    gap: 16,
  },
  subtitle: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 15,
    lineHeight: 22,
    color: Colors.textSecondary,
  },
  errorText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.error,
  },
  inputRow: {
    height: 48,
    borderRadius: Radii.cta,
    borderWidth: 1,
    borderColor: Colors.inputBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    overflow: 'hidden',
  },
  countryCode: {
    paddingHorizontal: 12,
    borderRightWidth: 1,
    borderRightColor: Colors.inputBorder,
    height: '100%',
    justifyContent: 'center',
  },
  countryCodeText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  input: {
    flex: 1,
    paddingHorizontal: 12,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 20,
    color: Colors.textPrimary,
    padding: 0,
  },
  inviteButton: {
    height: '100%',
    paddingHorizontal: 16,
    backgroundColor: Colors.accent,
    justifyContent: 'center',
    alignItems: 'center',
  },
  inviteButtonDisabled: {
    opacity: 0.5,
  },
  inviteButtonText: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 15,
    lineHeight: 20,
    color: Colors.white,
  },
  sentTitle: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textPrimary,
    marginTop: 8,
  },
  inviteList: {
    backgroundColor: Colors.white,
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    overflow: 'hidden',
  },
  inviteRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: Colors.cardBorder,
    gap: 12,
  },
  avatarFallback: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.avatarPlaceholderInnerBg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarInitial: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.avatarPlaceholderFg,
  },
  inviteText: {
    flex: 1,
    gap: 2,
  },
  invitePhone: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  inviteStatus: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
  },
});
