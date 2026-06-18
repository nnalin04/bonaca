import { useRouter } from 'expo-router';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { EmptyNotificationsState } from '@/features/notifications/components/EmptyNotificationsState';
import { NotificationRow } from '@/features/notifications/components/NotificationRow';
import { NotificationsHeader } from '@/features/notifications/components/NotificationsHeader';
import { Colors } from '@/theme/tokens';
import type { Notification } from '@/types';

interface NotificationDisplay extends Notification {
  avatarSource?: number;
}

const notifications: NotificationDisplay[] = [
  {
    id: 'notif-1',
    memberId: 'member-mom',
    type: 'subscription',
    read: false,
    deepLinkTarget: '/subscription/payment-gateway',
    createdAt: '2026-06-19T08:30:00.000Z',
    title: 'Mom',
    body: 'Subscription expiring soon, renew to continue uninterrupted health monitoring',
    displayTime: '1 hr ago',
  },
  {
    id: 'notif-2',
    memberId: 'member-dad',
    type: 'metric-anomaly',
    read: false,
    deepLinkTarget: '/member/member-dad/metric/heart_rate',
    createdAt: '2026-06-19T05:30:00.000Z',
    title: 'Dad',
    body: 'Average heart rate has been elevated since morning. Consider checking in.',
    displayTime: '4 hrs ago',
    avatarSource: require('../../../assets/images/avatars/prasanna-kumar.png'),
  },
  {
    id: 'notif-3',
    memberId: 'member-self',
    type: 'payment-request',
    read: false,
    deepLinkTarget: '/subscription/payment-gateway',
    createdAt: '2026-06-18T10:00:00.000Z',
    title: 'Prasanna Kumar',
    body: 'Rakesh has requested ₹249 to renew your wearable connection. Tap to pay.',
    displayTime: 'Yesterday, 3:30 PM',
    avatarSource: require('../../../assets/images/avatars/prasanna-kumar.png'),
  },
  {
    id: 'notif-4',
    memberId: 'member-rekha',
    type: 'payment-request',
    read: true,
    deepLinkTarget: '/subscription/payment-gateway',
    createdAt: '2026-06-12T09:15:00.000Z',
    title: 'Rekha P',
    body: 'Rakesh has requested ₹249 to renew your wearable connection. Tap to pay.',
    displayTime: '1 week ago',
  },
];

export function NotificationsScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.screen}>
      <NotificationsHeader onPressBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        {notifications.length === 0 ? (
          <EmptyNotificationsState />
        ) : (
          notifications.map((notification) => (
            <NotificationRow
              key={notification.id}
              avatarSource={notification.avatarSource}
              title={notification.title}
              body={notification.body}
              displayTime={notification.displayTime}
            />
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
    gap: 16,
  },
});
