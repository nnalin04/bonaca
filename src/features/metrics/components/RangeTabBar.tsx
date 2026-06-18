import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

export type MetricRange = '1D' | '7D' | '4W' | '1Y';

const ranges: MetricRange[] = ['1D', '7D', '4W', '1Y'];

interface RangeTabBarProps {
  activeRange: MetricRange;
  onChangeRange: (range: MetricRange) => void;
}

export function RangeTabBar({ activeRange, onChangeRange }: RangeTabBarProps) {
  return (
    <View style={styles.floater}>
      {ranges.map((range) => {
        const isActive = range === activeRange;
        return (
          <Pressable
            key={range}
            style={[styles.tab, isActive && styles.tabActive]}
            onPress={() => onChangeRange(range)}
            accessibilityRole="button"
            accessibilityLabel={range}
            accessibilityState={{ selected: isActive }}>
            <Text style={[styles.tabLabel, isActive && styles.tabLabelActive]}>
              {range}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  floater: {
    flexDirection: 'row',
    alignSelf: 'center',
    backgroundColor: Colors.tabBarTrack,
    borderWidth: 1,
    borderColor: Colors.tabBarBorder,
    borderRadius: Radii.pill,
    padding: 2,
  },
  tab: {
    alignItems: 'center',
    justifyContent: 'center',
    height: 36,
    paddingHorizontal: 20,
    borderRadius: Radii.pill,
  },
  tabActive: {
    backgroundColor: Colors.accent,
  },
  tabLabel: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
  },
  tabLabelActive: {
    color: Colors.white,
  },
});
