import type { Icon } from '@tabler/icons-react-native';
import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface MetricCardProps {
  icon: Icon;
  label: string;
  value: string;
  unitSuffix?: string;
  trendText?: string | null;
  /** 'full' spans the row; 'half' is meant to sit two-up via MetricCardRow. */
  width?: 'full' | 'half';
  /** Optional right-aligned accessory, e.g. a sparkline or mini bar chart. */
  accessory?: ReactNode;
  onPress?: () => void;
}

export function MetricCard({
  icon: MetricIcon,
  label,
  value,
  unitSuffix,
  trendText,
  width = 'full',
  accessory,
  onPress,
}: MetricCardProps) {
  return (
    <Pressable
      style={[styles.card, width === 'half' && styles.cardHalf]}
      onPress={onPress}
      accessibilityRole={onPress ? 'button' : undefined}
      accessibilityLabel={label}>
      <View style={styles.iconCircle}>
        <MetricIcon size={24} color={Colors.accent} strokeWidth={1.75} />
      </View>
      <Text style={styles.label}>{label}</Text>

      <View style={styles.bottomRow}>
        <View style={styles.valueBlock}>
          <View style={styles.valueLine}>
            <Text style={styles.value}>{value}</Text>
            {unitSuffix ? <Text style={styles.unit}>{unitSuffix}</Text> : null}
          </View>
          {trendText ? <Text style={styles.trend}>{trendText}</Text> : null}
        </View>

        {accessory ? <View style={styles.accessory}>{accessory}</View> : null}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    width: '100%',
    minHeight: 136,
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    padding: 12,
    gap: 8,
  },
  cardHalf: {
    width: '100%',
    flex: 1,
  },
  iconCircle: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.tabBarTrack,
    alignItems: 'center',
    justifyContent: 'center',
  },
  label: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  bottomRow: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'space-between',
    gap: 8,
  },
  valueBlock: {
    gap: 4,
  },
  valueLine: {
    flexDirection: 'row',
    alignItems: 'baseline',
    gap: 4,
  },
  value: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 24,
    lineHeight: 32,
    color: Colors.textPrimary,
  },
  unit: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 13,
    lineHeight: 18,
    color: Colors.textPrimary,
  },
  trend: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
  accessory: {
    alignItems: 'flex-end',
    justifyContent: 'flex-end',
  },
});
