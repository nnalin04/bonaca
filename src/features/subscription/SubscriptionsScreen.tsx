import { IconChevronRight, IconUsers, IconX } from '@tabler/icons-react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { Colors, Fonts, Radii } from '@/theme/tokens';

interface SubscriptionRow {
  id: string;
  name: string;
  status: string;
  statusTone?: 'error';
  image?: number;
  initial?: string;
  drawer?: {
    chip: string;
    title: string;
    body: string;
  };
}

const subscriptions: SubscriptionRow[] = [
  {
    id: 'dad',
    name: 'Dad',
    status: 'Renews on 28 Jan',
    image: require('../../../assets/images/avatars/prasanna-kumar.png'),
  },
  {
    id: 'mom',
    name: 'Mom',
    status: 'Subscription expires in 3 days',
    image: require('../../../assets/images/avatars/prasanna-kumar.png'),
    drawer: {
      chip: 'Expires in 3 days',
      title: 'Mom',
      body: 'Renew to continue uninterrupted health insights',
    },
  },
  {
    id: 'brother',
    name: 'Brother',
    status: 'Subscription expired 2 months ago',
    statusTone: 'error',
    initial: 'B',
    drawer: {
      chip: 'Subscription Expired',
      title: 'Brother',
      body: 'Subscription has ended, health tracking is paused',
    },
  },
];

export function SubscriptionsScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [selected, setSelected] = useState<SubscriptionRow | null>(null);

  return (
    <View style={styles.screen}>
      <ProfileHeader title="Subscriptions" onPressBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        <View style={styles.list}>
          {subscriptions.map((subscription) => (
            <Pressable
              key={subscription.id}
              style={styles.row}
              onPress={() => setSelected(subscription.drawer ? subscription : null)}
              accessibilityRole="button"
              accessibilityLabel={subscription.name}>
              {subscription.image ? (
                <Image source={subscription.image} style={styles.avatar} contentFit="cover" />
              ) : (
                <PlaceholderAvatar size={36} />
              )}

              <View style={styles.textBlock}>
                <Text style={styles.name}>{subscription.name}</Text>
                <Text
                  style={[
                    styles.status,
                    subscription.statusTone === 'error' && styles.statusError,
                  ]}>
                  {subscription.status}
                </Text>
              </View>

              <IconChevronRight size={24} color={Colors.textPrimary} strokeWidth={2} />
            </Pressable>
          ))}
        </View>
      </ScrollView>

      {selected ? (
        <View style={styles.overlay}>
          <Pressable
            style={styles.closeButton}
            onPress={() => setSelected(null)}
            accessibilityRole="button"
            accessibilityLabel="Close subscription renewal">
            <IconX size={24} color={Colors.white} strokeWidth={2} />
          </Pressable>

          <View style={[styles.drawer, { paddingBottom: insets.bottom + 20 }]}>
            <View style={styles.drawerAvatarWrap}>
              {selected.image ? (
                <Image source={selected.image} style={styles.drawerAvatar} contentFit="cover" />
              ) : (
                <PlaceholderAvatar size={80} />
              )}
              <View style={styles.chip}>
                <Text style={styles.chipText}>{selected.drawer?.chip}</Text>
              </View>
            </View>

            <Text style={styles.drawerTitle}>{selected.drawer?.title}</Text>
            <Text style={styles.drawerBody}>{selected.drawer?.body}</Text>

            <Pressable
              style={styles.renewButton}
              onPress={() => router.push('/subscription/payment-gateway')}
              accessibilityRole="button"
              accessibilityLabel="Renew at ₹249">
              <Text style={styles.renewText}>Renew at ₹249</Text>
            </Pressable>
          </View>
        </View>
      ) : null}
    </View>
  );
}

function PlaceholderAvatar({ size }: { size: number }) {
  const headSize = size * 0.32;
  const bodyWidth = size * 0.76;
  const bodyHeight = size * 0.44;

  return (
    <View style={[styles.placeholderAvatar, { width: size, height: size, borderRadius: size / 2 }]}>
      <View
        style={[
          styles.placeholderHead,
          {
            width: headSize,
            height: headSize,
            borderRadius: headSize / 2,
            top: size * 0.25,
            left: (size - headSize) / 2,
          },
        ]}
      />
      <View
        style={[
          styles.placeholderBody,
          {
            width: bodyWidth,
            height: bodyHeight,
            borderRadius: bodyWidth / 2,
            top: size * 0.67,
            left: (size - bodyWidth) / 2,
          },
        ]}
      />
    </View>
  );
}

export function EmptySubscriptionsScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.screen}>
      <ProfileHeader title="Subscriptions" onPressBack={() => router.back()} />
      <View style={[styles.emptyCard, { marginBottom: insets.bottom + 45 }]}>
        <IconUsers size={80} color={Colors.emptyStateIcon} strokeWidth={1.75} />
        <Text style={styles.emptyTitle}>No Subscriptions Yet</Text>
        <Text style={styles.emptyText}>
          Subscriptions will be visible here when a family member shares access
        </Text>
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
  },
  list: {
    gap: 16,
  },
  row: {
    height: 56,
    borderRadius: Radii.row,
    borderWidth: 1,
    borderColor: Colors.onboardingCardBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
  },
  avatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: Colors.avatarBorder,
  },
  placeholderAvatar: {
    borderWidth: 1,
    borderColor: Colors.avatarBorder,
    backgroundColor: Colors.avatarPlaceholderInnerBg,
    overflow: 'hidden',
  },
  placeholderHead: {
    position: 'absolute',
    backgroundColor: Colors.avatarPlaceholderFg,
  },
  placeholderBody: {
    position: 'absolute',
    backgroundColor: Colors.avatarPlaceholderFg,
  },
  textBlock: {
    flex: 1,
    marginLeft: 8,
  },
  name: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  status: {
    marginTop: 2,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
  statusError: {
    color: Colors.error,
  },
  overlay: {
    position: 'absolute',
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
    backgroundColor: Colors.modalScrim,
    justifyContent: 'flex-end',
  },
  closeButton: {
    position: 'absolute',
    bottom: 358,
    alignSelf: 'center',
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: Colors.modalClose,
    alignItems: 'center',
    justifyContent: 'center',
  },
  drawer: {
    minHeight: 326,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    backgroundColor: Colors.white,
    paddingHorizontal: 16,
    paddingTop: 24,
    alignItems: 'center',
  },
  drawerAvatarWrap: {
    alignItems: 'center',
  },
  drawerAvatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    borderWidth: 1,
    borderColor: Colors.avatarBorder,
  },
  chip: {
    minHeight: 24,
    marginTop: -18,
    borderRadius: 6,
    backgroundColor: Colors.subscriptionChipBackground,
    paddingHorizontal: 6,
    justifyContent: 'center',
  },
  chipText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.error,
  },
  drawerTitle: {
    marginTop: 20,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 24,
    lineHeight: 32,
    color: Colors.textPrimary,
  },
  drawerBody: {
    marginTop: 26,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
  renewButton: {
    width: '100%',
    height: 56,
    marginTop: 52,
    borderRadius: Radii.button,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  renewText: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.white,
  },
  emptyCard: {
    flex: 1,
    marginHorizontal: 16,
    marginTop: 20,
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 48,
  },
  emptyTitle: {
    marginTop: 24,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.textPrimary,
    textAlign: 'center',
  },
  emptyText: {
    marginTop: 8,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
});
