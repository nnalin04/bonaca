import { useRouter } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/AuthContext';
import { MemberDetailsHeader } from '@/features/members/components/MemberDetailsHeader';
import {
  MemberTabBar,
  type MemberDetailsTab,
} from '@/features/members/components/MemberTabBar';
import { MetricCard } from '@/features/members/components/MetricCard';
import { MetricCardRow } from '@/features/members/components/MetricCardRow';
import { NicknameModal } from '@/features/members/components/NicknameModal';
import { SelectModal } from '@/features/onboarding/components/SelectModal';
import { useMemberMetricSummaries } from '@/features/metrics/hooks/useMemberMetricSummaries';
import {
  formatTrendLabel,
  metricDisplayConfig,
} from '@/features/members/metricDisplay';
import { getMember, updateMember } from '@/lib/api/members';
import { Colors, Fonts } from '@/theme/tokens';
import type { MetricReading } from '@/types';
import type { MemberResponse } from '@/types/members';

interface MemberDetailsScreenProps {
  memberId: string;
}

const tabTitles: Record<MemberDetailsTab, string> = {
  vitals: 'Vitals',
  activity: 'Activity',
  behaviour: 'Behaviour',
};

const metricOrderByTab: Record<
  MemberDetailsTab,
  MetricReading['metricType'][]
> = {
  vitals: [
    'heart_rate',
    'heart_rate_variability',
    'blood_oxygen',
    'respiration_rate',
    'sleep',
    'stress_level',
    'body_temperature',
    'ecg',
    'blood_glucose',
    'vo2_max',
  ],
  activity: ['steps', 'calories', 'workouts', 'training_load'],
  behaviour: [
    'routine_adherence',
    'screen_time',
    'outdoor_time',
    'last_active_location',
  ],
};

export function MemberDetailsScreen({ memberId }: MemberDetailsScreenProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { accessToken } = useAuth();
  const [activeTab, setActiveTab] = useState<MemberDetailsTab>('vitals');
  const [menuVisible, setMenuVisible] = useState(false);
  const [nicknameModalVisible, setNicknameModalVisible] = useState(false);
  const [member, setMember] = useState<MemberResponse | null>(null);
  const {
    readings: backendReadings,
    isLoading: metricsLoading,
    errorMessage: metricsError,
  } = useMemberMetricSummaries(memberId, '7d');

  const refreshMember = useCallback(async () => {
    if (!accessToken) return;
    setMember(await getMember(accessToken, memberId));
  }, [accessToken, memberId]);

  useEffect(() => {
    let cancelled = false;
    if (!accessToken) return;
    getMember(accessToken, memberId).then((result) => {
      if (!cancelled) setMember(result);
    });
    return () => {
      cancelled = true;
    };
  }, [accessToken, memberId]);

  const displayName = member?.nickname ?? member?.name ?? 'Member';

  const menuOptions = [
    member?.pinned ? 'Unpin from top' : 'Pin to top',
    'Edit Nick Name',
    'Edit Permissions',
    'Hidden Members',
  ];

  const handleMenuSelect = async (option: string) => {
    if (!accessToken || !member) return;
    if (option === 'Pin to top' || option === 'Unpin from top') {
      await updateMember(accessToken, memberId, { pinned: !member.pinned });
      await refreshMember();
    } else if (option === 'Edit Nick Name') {
      setNicknameModalVisible(true);
    } else if (option === 'Edit Permissions') {
      router.push(`/members/${memberId}/permissions`);
    } else if (option === 'Hidden Members') {
      router.push('/members/hidden');
    }
  };

  const handleSaveNickname = async (nickname: string) => {
    if (!accessToken) return;
    await updateMember(accessToken, memberId, { nickname });
    await refreshMember();
  };

  const goToMetric = (reading: MetricReading) =>
    router.push(`/member/${memberId}/metric/${reading.metricType}`);

  const renderReading = (
    reading: MetricReading,
    width: 'full' | 'half' = 'half',
  ) => {
    const config = metricDisplayConfig[reading.metricType];
    return (
      <MetricCard
        key={reading.id}
        icon={config.icon}
        iconColor={config.iconColor}
        label={config.label}
        value={config.formatValue(reading.value)}
        unitSuffix={config.unitSuffix}
        trendText={
          reading.customCaption ?? formatTrendLabel(reading.trendLabel)
        }
        width={width}
        onPress={() => goToMetric(reading)}
      />
    );
  };

  const renderActiveTab = () => {
    if (metricsLoading) {
      return (
        <View style={styles.stateCard}>
          <ActivityIndicator />
          <Text style={styles.stateText}>Loading health metrics…</Text>
        </View>
      );
    }

    if (metricsError) {
      return (
        <View style={styles.stateCard}>
          <Text style={styles.stateTitle}>Could not load metrics</Text>
          <Text style={styles.stateText}>{metricsError}</Text>
        </View>
      );
    }

    const orderedReadings = metricOrderByTab[activeTab]
      .map((metricType) =>
        backendReadings.find((reading) => reading.metricType === metricType),
      )
      .filter((reading): reading is MetricReading => Boolean(reading));

    if (orderedReadings.length === 0) {
      return (
        <View style={styles.stateCard}>
          <Text style={styles.stateTitle}>No data yet</Text>
          <Text style={styles.stateText}>
            This section will fill in once wearable data syncs from the backend.
          </Text>
        </View>
      );
    }

    return (
      <View style={styles.cardStack}>
        {orderedReadings.map((reading, index) => {
          const nextReading = orderedReadings[index + 1];
          if (index % 2 === 1) return null;

          if (!nextReading) {
            return renderReading(reading, 'full');
          }

          return (
            <MetricCardRow key={`${reading.id}-${nextReading.id}`}>
              {renderReading(reading)}
              {renderReading(nextReading)}
            </MetricCardRow>
          );
        })}
      </View>
    );
  };

  return (
    <View style={styles.screen}>
      <MemberDetailsHeader
        avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
        displayName={displayName}
        statusMessage={
          member?.statusMessage ?? 'Health insights appear after data syncs'
        }
        onPressBack={() => router.back()}
        onPressMenu={() => setMenuVisible(true)}
      />

      <View style={[styles.tabBarWrap, { bottom: insets.bottom + 20 }]}>
        <MemberTabBar activeTab={activeTab} onChangeTab={setActiveTab} />
      </View>

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + 98 },
        ]}
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.sectionTitle}>{tabTitles[activeTab]}</Text>

        {renderActiveTab()}
      </ScrollView>

      <SelectModal
        visible={menuVisible}
        title="Member Options"
        options={menuOptions}
        onSelect={(option) => void handleMenuSelect(option)}
        onClose={() => setMenuVisible(false)}
      />

      <NicknameModal
        visible={nicknameModalVisible}
        initialValue={displayName}
        onSave={(nickname) => void handleSaveNickname(nickname)}
        onClose={() => setNicknameModalVisible(false)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  tabBarWrap: {
    position: 'absolute',
    left: 26,
    right: 25,
    alignItems: 'center',
    zIndex: 10,
  },
  content: {
    paddingHorizontal: 16,
    paddingTop: 24,
  },
  sectionTitle: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 20,
    lineHeight: 28,
    color: Colors.textPrimary,
    marginBottom: 12,
  },
  cardStack: {
    gap: 16,
  },
  stateCard: {
    minHeight: 136,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
    gap: 8,
  },
  stateTitle: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textPrimary,
    textAlign: 'center',
  },
  stateText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
});
