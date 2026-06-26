import { useRouter, type Href } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useMembers } from '@/features/members';
import { EmptyNotificationsState } from '@/features/notifications/components/EmptyNotificationsState';
import { NotificationRow } from '@/features/notifications/components/NotificationRow';
import { NotificationsHeader } from '@/features/notifications/components/NotificationsHeader';
import { formatDisplayTime } from '@/features/notifications/formatDisplayTime';
import { useNotifications } from '@/features/notifications/useNotifications';
import { Colors, Fonts } from '@/theme/tokens';
import type { NotificationResponse } from '@/types/notifications';

export function NotificationsScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { self } = useMembers();
  const { notifications, isLoading, errorMessage, markRead, refresh } = useNotifications(self?.id);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await refresh();
    setIsRefreshing(false);
  };

  const handlePress = (notification: NotificationResponse) => {
    if (!notification.read) {
      void markRead(notification.id);
    }
    try {
      if (notification.deepLinkTarget) {
        router.push(notification.deepLinkTarget as Href);
      }
    } catch {
      console.warn('Invalid deep link target:', notification.deepLinkTarget);
    }
  };

  return (
    <View style={styles.screen}>
      <NotificationsHeader onPressBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={isRefreshing} onRefresh={handleRefresh} />}>
        {isLoading && notifications.length === 0 ? (
          <ActivityIndicator style={styles.loading} />
        ) : errorMessage ? (
          <Text style={styles.errorText}>{errorMessage}</Text>
        ) : notifications.length === 0 ? (
          <EmptyNotificationsState />
        ) : (
          notifications.map((notification) => (
            <NotificationRow
              key={notification.id}
              title={notification.title}
              body={notification.body}
              displayTime={formatDisplayTime(notification.createdAt)}
              onPress={() => handlePress(notification)}
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
