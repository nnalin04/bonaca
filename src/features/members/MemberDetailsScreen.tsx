import { useRouter } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/AuthContext';
import { MemberDetailsHeader } from '@/features/members/components/MemberDetailsHeader';
import {
  MemberTabBar,
  type MemberDetailsTab,
} from '@/features/members/components/MemberTabBar';
import { MetricCard } from '@/features/members/components/MetricCard';
import { MetricCardRow } from '@/features/members/components/MetricCardRow';
import { MiniSparkline } from '@/features/members/components/MiniSparkline';
import { NicknameModal } from '@/features/members/components/NicknameModal';
import { SleepWeekBars } from '@/features/members/components/SleepWeekBars';
import { SelectModal } from '@/features/onboarding/components/SelectModal';
import {
  activityReadings,
  behaviourReadings,
  mockMember,
  sleepWeeklyBars,
  sparklinePoints,
  vitalsReadings,
} from '@/features/members/mockData';
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

/** X-axis labels for chart-style metric cards, per Figma (196:4233). Only metrics with a confirmed Figma reference get labels. */
const sparklineAxisLabels: Partial<Record<MetricReading['metricType'], string[]>> = {
  heart_rate: ['6AM', '12PM', '6PM', '12AM'],
  heart_rate_variability: ['1W', '2W', '3W', '4W'],
};

export function MemberDetailsScreen({ memberId }: MemberDetailsScreenProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { accessToken } = useAuth();
  const [activeTab, setActiveTab] = useState<MemberDetailsTab>('vitals');
  const [menuVisible, setMenuVisible] = useState(false);
  const [nicknameModalVisible, setNicknameModalVisible] = useState(false);
  const [member, setMember] = useState<MemberResponse | null>(null);

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

  const displayName = member?.nickname ?? member?.name ?? mockMember.nickname ?? mockMember.name;
  // Metric content below (Vitals/Activity/Behaviour tabs) is still mock data — real wearable
  // sync (TRD M1/M2) isn't built yet; this phase only wires member identity/permissions.

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
        trendText={reading.customCaption ?? formatTrendLabel(reading.trendLabel)}
        width={width}
        onPress={() => goToMetric(reading)}
      />
    );
  };

  const renderChartReading = (reading: MetricReading) => {
    const config = metricDisplayConfig[reading.metricType];
    const points = sparklinePoints[reading.metricType];
    return (
      <MetricCard
        key={reading.id}
        icon={config.icon}
        iconColor={config.iconColor}
        label={config.label}
        value={config.formatValue(reading.value)}
        unitSuffix={config.unitSuffix}
        trendText={formatTrendLabel(reading.trendLabel)}
        width="full"
        accessory={
          points ? (
            <MiniSparkline
              points={points}
              xAxisLabels={sparklineAxisLabels[reading.metricType]}
              lineColor={
                reading.metricType === 'heart_rate_variability'
                  ? Colors.chartLineSecondary
                  : undefined
              }
              areaColor={
                reading.metricType === 'heart_rate_variability'
                  ? Colors.chartAreaFillSecondary
                  : undefined
              }
            />
          ) : undefined
        }
        onPress={() => goToMetric(reading)}
      />
    );
  };

  const byType = (type: MetricReading['metricType'], list: MetricReading[]) =>
    list.find((r) => r.metricType === type);

  const renderVitalsTab = () => {
    const heartRate = byType('heart_rate', vitalsReadings)!;
    const hrv = byType('heart_rate_variability', vitalsReadings)!;
    const spo2 = byType('blood_oxygen', vitalsReadings)!;
    const respiration = byType('respiration_rate', vitalsReadings)!;
    const sleep = byType('sleep', vitalsReadings)!;
    const stress = byType('stress_level', vitalsReadings)!;
    const temperature = byType('body_temperature', vitalsReadings)!;
    const ecg = byType('ecg', vitalsReadings)!;
    const bloodGlucose = byType('blood_glucose', vitalsReadings)!;
    const vo2Max = byType('vo2_max', vitalsReadings)!;

    const sleepConfig = metricDisplayConfig.sleep;

    return (
      <View style={styles.cardStack}>
        {renderChartReading(heartRate)}
        {renderChartReading(hrv)}
        <MetricCardRow>
          {renderReading(spo2)}
          {renderReading(respiration)}
        </MetricCardRow>
        <MetricCard
          icon={sleepConfig.icon}
          iconColor={sleepConfig.iconColor}
          label={sleepConfig.label}
          value={sleepConfig.formatValue(sleep.value)}
          trendText={formatTrendLabel(sleep.trendLabel)}
          width="full"
          accessory={<SleepWeekBars bars={sleepWeeklyBars} />}
          onPress={() => goToMetric(sleep)}
        />
        <MetricCardRow>
          {renderReading(stress)}
          {renderReading(temperature)}
        </MetricCardRow>
        <MetricCardRow>
          {renderReading(ecg)}
          {renderReading(bloodGlucose)}
        </MetricCardRow>
        {renderChartReading(vo2Max)}
      </View>
    );
  };

  const renderActivityTab = () => {
    const steps = byType('steps', activityReadings)!;
    const calories = byType('calories', activityReadings)!;
    const workouts = byType('workouts', activityReadings)!;
    const trainingLoad = byType('training_load', activityReadings)!;

    return (
      <View style={styles.cardStack}>
        <MetricCardRow>
          {renderReading(steps)}
          {renderReading(calories)}
        </MetricCardRow>
        <MetricCardRow>
          {renderReading(workouts)}
          {renderReading(trainingLoad)}
        </MetricCardRow>
      </View>
    );
  };

  const renderBehaviourTab = () => {
    const routine = byType('routine_adherence', behaviourReadings)!;
    const screenTime = byType('screen_time', behaviourReadings)!;
    const outdoorTime = byType('outdoor_time', behaviourReadings)!;
    const lastActiveLocation = byType(
      'last_active_location',
      behaviourReadings,
    )!;

    return (
      <View style={styles.cardStack}>
        <MetricCardRow>
          {renderReading(routine)}
          {renderReading(screenTime)}
        </MetricCardRow>
        <MetricCardRow>
          {renderReading(outdoorTime)}
          {renderReading(lastActiveLocation)}
        </MetricCardRow>
      </View>
    );
  };

  return (
    <View style={styles.screen}>
      <MemberDetailsHeader
        avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
        displayName={displayName}
        statusMessage={member?.statusMessage ?? mockMember.statusMessage}
        onPressBack={() => router.back()}
        onPressMenu={() => setMenuVisible(true)}
      />

      <View style={styles.tabBarWrap}>
        <MemberTabBar activeTab={activeTab} onChangeTab={setActiveTab} />
      </View>

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + 24 },
        ]}
        showsVerticalScrollIndicator={false}>
        <Text style={styles.sectionTitle}>{tabTitles[activeTab]}</Text>

        {activeTab === 'vitals' && renderVitalsTab()}
        {activeTab === 'activity' && renderActivityTab()}
        {activeTab === 'behaviour' && renderBehaviourTab()}
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
    marginTop: -23,
    alignItems: 'center',
  },
  content: {
    paddingHorizontal: 16,
    paddingTop: 36,
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
});
