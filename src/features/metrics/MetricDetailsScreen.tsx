import { useRouter } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
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
import { Colors, Fonts, Radii } from '@/theme/tokens';
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
  const { summary, insightText, isLoading, errorMessage } = useMetricDetail(
    memberId,
    metricType,
    backendRangeByUiRange[range],
  );

  const config = metricDisplayConfig[metricType];

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

        {isLoading ? (
          <MetricStateCard message="Loading metric details…" />
        ) : errorMessage ? (
          <MetricStateCard
            title="Could not load metric"
            message={errorMessage}
          />
        ) : summary ? (
          <>
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
                formatTrendLabel(summary.average.trendLabel) ?? undefined
              }
              highestLabel={`${summary.average.rangeMax ?? summary.average.value} ${config.unitSuffix}`.trim()}
              lowestLabel={`${summary.average.rangeMin ?? summary.average.value} ${config.unitSuffix}`.trim()}
              insightText={insightText ?? undefined}
            />
          </>
        ) : (
          <MetricStateCard
            title="No data yet"
            message="This metric will appear once wearable data syncs from the backend."
          />
        )}
      </ScrollView>
    </View>
  );
}

interface MetricStateCardProps {
  title?: string;
  message: string;
}

function MetricStateCard({ title, message }: MetricStateCardProps) {
  return (
    <View style={styles.stateCard}>
      {title ? (
        <Text style={styles.stateTitle}>{title}</Text>
      ) : (
        <ActivityIndicator />
      )}
      <Text style={styles.stateText}>{message}</Text>
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
  stateCard: {
    minHeight: 220,
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
    gap: 10,
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
