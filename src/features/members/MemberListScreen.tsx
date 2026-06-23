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

import { HomeHeader } from '@/features/home/components/HomeHeader';
import { MemberSyncCard } from '@/features/home/components/MemberSyncCard';
import { useMembers } from '@/features/members/useMembers';
import { Colors, Fonts, Radii } from '@/theme/tokens';

export function MemberListScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { self, others, isLoading, errorMessage } = useMembers();

  return (
    <View style={styles.screen}>
      <HomeHeader
        greetingName={self?.name.split(' ')[0] ?? ''}
        statusMessage="Manage your family members"
        unreadNotificationCount={0}
        onPressNotifications={() => router.push('/notifications')}
        onPressProfile={() => router.push('/profile')}
      />

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
            <View style={styles.trialBanner}>
              <View style={styles.bannerArcOne} />
              <View style={styles.bannerArcTwo} />
              <Text style={styles.trialCopy}>
                Try the full experience free for 7 days. Cancel anytime.
              </Text>
              <Pressable
                style={styles.connectButton}
                onPress={() =>
                  router.push('/subscription/select-wearable-account')
                }
                accessibilityRole="button"
                accessibilityLabel="Connect wearable account"
              >
                <Text style={styles.connectButtonText}>Connect</Text>
              </Pressable>
            </View>

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

            <Pressable
              style={styles.addButton}
              onPress={() => router.push('/members/invite')}
              accessibilityRole="button"
              accessibilityLabel="Add Member"
            >
              <IconUserPlus
                size={24}
                color={Colors.accent}
                strokeWidth={1.75}
              />
              <Text style={styles.addLabel}>Add Member</Text>
            </Pressable>
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
  content: {
    paddingHorizontal: 16,
    paddingTop: 20,
  },
  trialBanner: {
    height: 108,
    borderRadius: Radii.card,
    backgroundColor: Colors.wearableCardBackground,
    overflow: 'hidden',
    paddingHorizontal: 12,
    paddingTop: 24,
  },
  bannerArcOne: {
    position: 'absolute',
    right: -74,
    top: -80,
    width: 260,
    height: 260,
    borderRadius: 130,
    borderWidth: 3,
    borderColor: Colors.wearableCardIconBorder,
    opacity: 0.45,
  },
  bannerArcTwo: {
    position: 'absolute',
    right: 25,
    top: -118,
    width: 342,
    height: 342,
    borderRadius: 171,
    borderWidth: 3,
    borderColor: Colors.wearableCardIconBorder,
    opacity: 0.45,
  },
  trialCopy: {
    width: 283,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.white,
  },
  connectButton: {
    marginTop: 16,
    alignSelf: 'flex-start',
    height: 24,
    borderRadius: Radii.card,
    paddingHorizontal: 16,
    backgroundColor: Colors.headerGradientEnd,
    alignItems: 'center',
    justifyContent: 'center',
  },
  connectButtonText: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.white,
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
  addButton: {
    marginTop: 24,
    height: 56,
    borderRadius: Radii.row,
    backgroundColor: Colors.tabBarTrack,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
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
  addLabel: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.accent,
  },
});
