import { useRouter } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { metricDisplayConfig } from '@/features/members/metricDisplay';
import { BarChartCard } from '@/features/metrics/components/BarChartCard';
import { DateStepper } from '@/features/metrics/components/DateStepper';
import { MetricDetailsHeader } from '@/features/metrics/components/MetricDetailsHeader';
import { MetricSummaryCard } from '@/features/metrics/components/MetricSummaryCard';
import {
  RangeTabBar,
  type MetricRange,
} from '@/features/metrics/components/RangeTabBar';
import {
  metricDetailSummaries,
  metricInsights,
} from '@/features/metrics/mockData';
import { Colors } from '@/theme/tokens';
import type { MetricType } from '@/types';

interface MetricDetailsScreenProps {
  memberId: string;
  metricType: MetricType;
}

const xAxisLabelsByRange: Record<MetricRange, string[]> = {
  '1D': ['12 AM', '6 AM', '12 PM', '6 PM'],
  '7D': ['Mon', 'Wed', 'Fri', 'Sun'],
  '4W': ['Wk 1', 'Wk 2', 'Wk 3', 'Wk 4'],
  '1Y': ['Jan', 'Apr', 'Jul', 'Oct'],
};

export function MetricDetailsScreen({
  memberId,
  metricType,
}: MetricDetailsScreenProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [range, setRange] = useState<MetricRange>('1D');
  const [dateOffset, setDateOffset] = useState(0);

  // memberId isn't yet wired to a real data source — see CLAUDE.md "Not Set Up Yet".
  void memberId;

  const config = metricDisplayConfig[metricType];
  const summary = metricDetailSummaries[metricType];
  const insight = metricInsights[metricType];

  const dateLabel =
    dateOffset === 0 ? 'Wednesday, 14 Jan (Today)' : `Tuesday, 13 Jan`;

  return (
    <View style={styles.screen}>
      <MetricDetailsHeader
        title={config.label}
        onPressBack={() => router.back()}
      />

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + 24 },
        ]}
        showsVerticalScrollIndicator={false}>
        <RangeTabBar activeRange={range} onChangeRange={setRange} />

        <DateStepper
          label={dateLabel}
          onPressPrevious={() => setDateOffset((value) => value + 1)}
          onPressNext={() => setDateOffset((value) => Math.max(0, value - 1))}
          canGoNext={dateOffset > 0}
        />

        <BarChartCard
          values={summary.chartValues}
          maxLabel={`${summary.average.rangeMax ?? summary.average.value} ${config.unitSuffix}`.trim()}
          minLabel={`${summary.average.rangeMin ?? summary.average.value} ${config.unitSuffix}`.trim()}
          xAxisLabels={xAxisLabelsByRange[range]}
        />

        <MetricSummaryCard
          title={`Average ${config.label}`}
          value={config.formatValue(summary.average.value)}
          unitSuffix={config.unitSuffix}
          highestLabel={`${summary.average.rangeMax ?? summary.average.value} ${config.unitSuffix}`.trim()}
          lowestLabel={`${summary.average.rangeMin ?? summary.average.value} ${config.unitSuffix}`.trim()}
          insightText={insight?.generatedText}
        />
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
