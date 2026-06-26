import { StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';
import type { InsightResponse } from '@/types/metrics';

interface InsightCardProps {
  insight: InsightResponse;
}

export function InsightCard({ insight }: InsightCardProps) {
  const kindLabel = insight.kind === 'anomaly' ? 'Alert' : 'Trend';
  const kindColor = insight.kind === 'anomaly' ? '#e97961' : Colors.accent;
  const kindBg = insight.kind === 'anomaly' ? 'rgba(233,121,97,0.1)' : 'rgba(87,95,180,0.1)';

  const metricLabel = insight.metricType
    ? insight.metricType.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
    : 'Health Summary';

  const dateLabel = formatInsightDate(insight.insightDate);

  return (
    <View style={styles.card}>
      <View style={styles.topRow}>
        <View style={[styles.kindBadge, { backgroundColor: kindBg }]}>
          <Text style={[styles.kindText, { color: kindColor }]}>{kindLabel}</Text>
        </View>
        <Text style={styles.metric}>{metricLabel}</Text>
        <Text style={styles.date}>{dateLabel}</Text>
      </View>
      <Text style={styles.body}>{insight.generatedText}</Text>
    </View>
  );
}

function formatInsightDate(dateStr: string): string {
  try {
    const date = new Date(dateStr);
    return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short' }).format(date);
  } catch {
    return dateStr;
  }
}

const styles = StyleSheet.create({
  card: {
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    padding: 16,
    gap: 10,
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  kindBadge: {
    borderRadius: Radii.pill,
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
  kindText: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 11,
    lineHeight: 16,
  },
  metric: {
    flex: 1,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 13,
    lineHeight: 18,
    color: Colors.textPrimary,
  },
  date: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
  body: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
  },
});
