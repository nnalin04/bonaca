import { IconUserPlus } from '@tabler/icons-react-native';
import { useRouter } from 'expo-router';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { MemberSyncCard } from '@/features/home/components/MemberSyncCard';
import { AddMemberButton } from '@/features/members/components/AddMemberButton';
import { MemberTrialBanner } from '@/features/members/components/MemberTrialBanner';
import { useMembers } from '@/features/members/useMembers';
import { Colors, Fonts } from '@/theme/tokens';

export function MemberListScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { self, others, isLoading, errorMessage } = useMembers();

  return (
    <View style={styles.screen}>
      <View style={[styles.pageHeader, { paddingTop: insets.top + 16 }]}>
        <Text style={styles.pageTitle}>Family Members</Text>
        <Pressable
          hitSlop={8}
          onPress={() => router.push('/members/invite')}
          accessibilityRole="button"
          accessibilityLabel="Invite a member">
          <IconUserPlus size={24} color={Colors.accent} strokeWidth={1.75} />
        </Pressable>
      </View>

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + 24 },
        ]}
        showsVerticalScrollIndicator={false}
      >
        {isLoading && !self ? (
          <ActivityIndicator style={styles.loading} />
        ) : errorMessage ? (
          <Text style={styles.errorText}>{errorMessage}</Text>
        ) : (
          <>
            <MemberTrialBanner
              onPressConnect={() =>
                router.push('/subscription/select-wearable-account')
              }
            />

            {self && (
              <View style={styles.selfCard}>
                <MemberSyncCard
                  avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
                  displayName={`${self.name} (You)`}
                  syncLabel="No wearable connected"
                  onPress={() => router.push(`/member/${self.id}`)}
                />
              </View>
            )}

            <Text style={styles.sectionTitle}>Members</Text>

            <View style={styles.memberRows}>
              {others.map((member, index) => (
                <MemberSyncCard
                  key={member.id}
                  avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
                  displayName={member.nickname ?? member.name}
                  syncLabel={`10 metrics  •  Last synced: ${index === 0 ? '1 hr' : '2 hrs'} ago`}
                  onPress={() => router.push(`/member/${member.id}`)}
                />
              ))}
            </View>

            <View style={styles.addMember}>
              <AddMemberButton onPress={() => router.push('/members/invite')} />
            </View>
          </>
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
  pageHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 12,
    backgroundColor: Colors.white,
    borderBottomWidth: 1,
    borderBottomColor: Colors.cardBorder,
  },
  pageTitle: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 20,
    lineHeight: 28,
    color: Colors.textPrimary,
  },
  content: {
    paddingHorizontal: 16,
    paddingTop: 20,
  },
  selfCard: {
    marginTop: 20,
  },
  sectionTitle: {
    marginTop: 24,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 20,
    lineHeight: 28,
    color: Colors.textPrimary,
  },
  memberRows: {
    marginTop: 12,
    gap: 16,
  },
  addMember: {
    marginTop: 24,
  },
  loading: {
    marginTop: 48,
  },
  errorText: {
    marginTop: 48,
    textAlign: 'center',
    color: Colors.error,
    fontFamily: Fonts.family,
  },
});
