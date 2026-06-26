import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/AuthContext';
import { useMembers } from '@/features/members/useMembers';
import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { updateMember } from '@/lib/api/members';
import { Colors, Fonts, Radii } from '@/theme/tokens';

export function HiddenMembersScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { accessToken } = useAuth();
  const { others, isLoading, refresh } = useMembers();
  const hiddenMembers = others.filter((member) => member.hidden);

  const handleUnhide = async (memberId: string) => {
    if (!accessToken) return;
    await updateMember(accessToken, memberId, { hidden: false });
    await refresh();
  };

  return (
    <View style={styles.screen}>
      <ProfileHeader title="Hidden Members" onPressBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        {isLoading ? (
          <ActivityIndicator style={styles.loading} />
        ) : hiddenMembers.length === 0 ? (
          <Text style={styles.emptyText}>No hidden members</Text>
        ) : (
          hiddenMembers.map((member) => (
            <View key={member.id} style={styles.row}>
              <View style={styles.identity}>
                <Image
                  source={require('../../../assets/images/avatars/prasanna-kumar.png')}
                  style={styles.avatar}
                />
                <Text style={styles.name}>{member.nickname ?? member.name}</Text>
              </View>
              <Pressable
                onPress={() => void handleUnhide(member.id)}
                accessibilityRole="button"
                accessibilityLabel={`Unhide ${member.nickname ?? member.name}`}>
                <Text style={styles.unhideLabel}>Unhide</Text>
              </Pressable>
            </View>
          ))
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
    paddingTop: 20,
    gap: 12,
  },
  loading: {
    marginTop: 48,
  },
  emptyText: {
    marginTop: 48,
    textAlign: 'center',
    fontFamily: Fonts.family,
    fontSize: 14,
    color: Colors.textSecondary,
  },
  row: {
    height: 56,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.white,
    borderWidth: 1,
    borderColor: Colors.onboardingCardBorder,
    borderRadius: Radii.row,
    paddingHorizontal: 16,
  },
  identity: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  avatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
  },
  name: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  unhideLabel: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.accent,
  },
});
