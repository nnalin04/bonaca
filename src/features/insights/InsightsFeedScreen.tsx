import { useRouter } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { InsightCard } from '@/features/insights/components/InsightCard';
import { useInsights } from '@/features/insights/hooks/useInsights';
import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { Colors, Fonts } from '@/theme/tokens';

interface InsightsFeedScreenProps {
  memberId: string;
}

export function InsightsFeedScreen({ memberId }: InsightsFeedScreenProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { insights, isLoading, errorMessage, refresh } = useInsights(memberId);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await refresh();
    setIsRefreshing(false);
  };

  return (
    <View style={styles.screen}>
      <ProfileHeader title="Health Insights" onPressBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + 24 },
        ]}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl refreshing={isRefreshing} onRefresh={handleRefresh} />
        }
      >
        {isLoading && !isRefreshing ? (
          <View style={styles.center}>
            <ActivityIndicator />
          </View>
        ) : errorMessage ? (
          <View style={styles.center}>
            <Text style={styles.errorText}>{errorMessage}</Text>
          </View>
        ) : insights.length === 0 ? (
          <View style={styles.center}>
            <Text style={styles.emptyTitle}>No insights yet</Text>
            <Text style={styles.emptyBody}>
              Insights appear here after your wearable syncs health data for a few days.
            </Text>
          </View>
        ) : (
          <View style={styles.list}>
            {insights.map((insight) => (
              <InsightCard key={insight.id} insight={insight} />
            ))}
          </View>
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
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 80,
    gap: 12,
  },
  list: {
    gap: 12,
  },
  errorText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.error,
    textAlign: 'center',
  },
  emptyTitle: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textPrimary,
    textAlign: 'center',
  },
  emptyBody: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
});
