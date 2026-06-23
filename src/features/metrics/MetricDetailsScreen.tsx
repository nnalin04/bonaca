import { useRouter } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import {
  formatTrendLabel,
  metricDisplayConfig,
} from '@/features/members/metricDisplay';
import { BarChartCard } from '@/features/metrics/components/BarChartCard';
import { DateStepper } from '@/features/metrics/components/DateStepper';
import { MetricDetailsHeader } from '@/features/metrics/components/MetricDetailsHeader';
import { MetricSummaryCard } from '@/features/metrics/components/MetricSummaryCard';
import {
  RangeTabBar,
  type MetricRange,
} from '@/features/metrics/components/RangeTabBar';
import { useMetricDetail } from '@/features/metrics/hooks/useMetricDetail';
import {
  metricDetailSummaries,
  metricInsights,
} from '@/features/metrics/mockData';
import { Colors } from '@/theme/tokens';
import type { MetricType } from '@/types';
import type { MetricRangeQuery } from '@/types/metrics';

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

const backendRangeByUiRange: Record<MetricRange, MetricRangeQuery> = {
  '1D': '24h',
  '7D': '7d',
  '4W': '30d',
  '1Y': '30d',
};

export function MetricDetailsScreen({
  memberId,
  metricType,
}: MetricDetailsScreenProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [range, setRange] = useState<MetricRange>('1D');
  const [dateOffset, setDateOffset] = useState(0);
  const { summary: backendSummary, insightText } = useMetricDetail(
    memberId,
    metricType,
    backendRangeByUiRange[range],
  );

  const config = metricDisplayConfig[metricType];
  const summary = backendSummary ?? metricDetailSummaries[metricType];
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
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.rangeWrap}>
          <RangeTabBar activeRange={range} onChangeRange={setRange} />
        </View>

        <View style={styles.dateWrap}>
          <DateStepper
            label={dateLabel}
            onPressPrevious={() => setDateOffset((value) => value + 1)}
            onPressNext={() => setDateOffset((value) => Math.max(0, value - 1))}
            canGoNext={dateOffset > 0}
          />
        </View>

        <View style={styles.chartWrap}>
          <BarChartCard
            values={summary.chartValues}
            maxLabel={`${summary.chartAxisMax ?? summary.average.rangeMax ?? summary.average.value} ${config.unitSuffix}`.trim()}
            minLabel={`${summary.chartAxisMin ?? summary.average.rangeMin ?? summary.average.value} ${config.unitSuffix}`.trim()}
            xAxisLabels={xAxisLabelsByRange[range]}
          />
        </View>

        <MetricSummaryCard
          title={`Average ${config.label}`}
          value={config.formatValue(summary.average.value)}
          unitSuffix={config.unitSuffix}
          trendText={
            formatTrendLabel(summary.average.trendLabel) ??
            (metricType === 'heart_rate' ? 'Higher than usual' : undefined)
          }
          highestLabel={`${summary.average.rangeMax ?? summary.average.value} ${config.unitSuffix}`.trim()}
          lowestLabel={`${summary.average.rangeMin ?? summary.average.value} ${config.unitSuffix}`.trim()}
          insightText={insightText ?? insight?.generatedText}
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
  },
  rangeWrap: {
    marginBottom: 20,
  },
  dateWrap: {
    marginBottom: 16,
  },
  chartWrap: {
    marginBottom: 20,
  },
});
