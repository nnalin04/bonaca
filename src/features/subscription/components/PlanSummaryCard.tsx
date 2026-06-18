import { StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface PlanSummaryCardProps {
  planName: string;
  priceLabel: string;
  billingCycleLabel: string;
}

export function PlanSummaryCard({ planName, priceLabel, billingCycleLabel }: PlanSummaryCardProps) {
  return (
    <View style={styles.card}>
      <View style={styles.row}>
        <Text style={styles.planName}>{planName}</Text>
        <Text style={styles.price}>{priceLabel}</Text>
      </View>
      <Text style={styles.cycle}>{billingCycleLabel}</Text>
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
    gap: 4,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  planName: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 17,
    lineHeight: 22,
    color: Colors.textPrimary,
  },
  price: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 17,
    lineHeight: 22,
    color: Colors.accent,
  },
  cycle: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 13,
    lineHeight: 18,
    color: Colors.textSecondary,
  },
});
