import { IconInfoHexagon } from '@tabler/icons-react-native';
import { StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface MetricSummaryCardProps {
  title: string;
  value: string;
  unitSuffix?: string;
  highestLabel: string;
  lowestLabel: string;
  insightText?: string;
}

export function MetricSummaryCard({
  title,
  value,
  unitSuffix,
  highestLabel,
  lowestLabel,
  insightText,
}: MetricSummaryCardProps) {
  return (
    <View style={styles.card}>
      <Text style={styles.title}>{title}</Text>

      <View style={styles.valueLine}>
        <Text style={styles.value}>{value}</Text>
        {unitSuffix ? <Text style={styles.unit}>{unitSuffix}</Text> : null}
      </View>

      <View style={styles.divider} />

      <View style={styles.minMaxRow}>
        <Text style={styles.minMaxText}>
          <Text style={styles.minMaxLabel}>Highest:</Text> {highestLabel}
        </Text>
        <Text style={styles.minMaxText}>
          <Text style={styles.minMaxLabel}>Lowest:</Text> {lowestLabel}
        </Text>
      </View>

      {insightText ? (
        <View style={styles.insightBox}>
          <IconInfoHexagon size={24} color={Colors.accent} strokeWidth={1.75} />
          <Text style={styles.insightText}>{insightText}</Text>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    width: '100%',
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    padding: 16,
    gap: 12,
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textPrimary,
  },
  valueLine: {
    flexDirection: 'row',
    alignItems: 'baseline',
    gap: 4,
  },
  value: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 28,
    lineHeight: 40,
    color: Colors.textPrimary,
  },
  unit: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  divider: {
    height: 1,
    backgroundColor: Colors.cardBorder,
  },
  minMaxRow: {
    flexDirection: 'row',
    gap: 24,
  },
  minMaxText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  minMaxLabel: {
    fontWeight: '500',
  },
  insightBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: Colors.insightCardBackground,
    borderRadius: 12,
    padding: 12,
  },
  insightText: {
    flex: 1,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
  },
});
