import { StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts } from '@/theme/tokens';

interface SleepWeekBarsProps {
  bars: { day: string; heightRatio: number }[];
}

const BAR_HEIGHT_MAX = 56;

export function SleepWeekBars({ bars }: SleepWeekBarsProps) {
  return (
    <View style={styles.row}>
      {bars.map((bar, index) => (
        <View key={`${bar.day}-${index}`} style={styles.barColumn}>
          <View style={styles.barTrack}>
            <View
              style={[
                styles.barFill,
                { height: Math.max(4, bar.heightRatio * BAR_HEIGHT_MAX) },
              ]}
            />
          </View>
          <Text style={styles.dayLabel}>{bar.day}</Text>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 8,
  },
  barColumn: {
    alignItems: 'center',
    gap: 4,
  },
  barTrack: {
    width: 8,
    height: BAR_HEIGHT_MAX,
    justifyContent: 'flex-end',
  },
  barFill: {
    width: 8,
    borderRadius: 5,
    backgroundColor: Colors.accent,
  },
  dayLabel: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 11,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
});
