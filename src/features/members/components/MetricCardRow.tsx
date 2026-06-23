import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

interface MetricCardRowProps {
  children: ReactNode;
}

export function MetricCardRow({ children }: MetricCardRowProps) {
  return <View style={styles.row}>{children}</View>;
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    gap: 16,
  },
});
