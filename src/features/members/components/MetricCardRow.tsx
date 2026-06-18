import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

interface MetricCardRowProps {
  children: ReactNode;
}

/** Lays out two MetricCard(width="half") side by side, matching the Figma two-up grid. */
export function MetricCardRow({ children }: MetricCardRowProps) {
  return <View style={styles.row}>{children}</View>;
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    gap: 16,
  },
});
