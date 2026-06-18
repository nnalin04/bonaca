import { useRouter } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { MemberDetailsHeader } from '@/features/members/components/MemberDetailsHeader';
import {
  MemberTabBar,
  type MemberDetailsTab,
} from '@/features/members/components/MemberTabBar';
import { MetricCard } from '@/features/members/components/MetricCard';
import { MetricCardRow } from '@/features/members/components/MetricCardRow';
import { MiniSparkline } from '@/features/members/components/MiniSparkline';
import { SleepWeekBars } from '@/features/members/components/SleepWeekBars';
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
import { Colors, Fonts } from '@/theme/tokens';
import type { MetricReading } from '@/types';

interface MemberDetailsScreenProps {
  memberId: string;
}

const tabTitles: Record<MemberDetailsTab, string> = {
  vitals: 'Vitals',
  activity: 'Activity',
  behaviour: 'Behaviour',
};

export function MemberDetailsScreen({ memberId }: MemberDetailsScreenProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [activeTab, setActiveTab] = useState<MemberDetailsTab>('vitals');

  // memberId isn't yet wired to a real data source — mock data always represents the same
  // "Dad" member from the Figma design until a backend exists (see CLAUDE.md "Not Set Up Yet").
  void memberId;

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
        label={config.label}
        value={config.formatValue(reading.value)}
        unitSuffix={config.unitSuffix}
        trendText={formatTrendLabel(reading.trendLabel)}
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
        label={config.label}
        value={config.formatValue(reading.value)}
        unitSuffix={config.unitSuffix}
        trendText={formatTrendLabel(reading.trendLabel)}
        width="full"
        accessory={points ? <MiniSparkline points={points} /> : undefined}
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
        displayName={mockMember.nickname ?? mockMember.name}
        statusMessage={mockMember.statusMessage}
        onPressBack={() => router.back()}
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
