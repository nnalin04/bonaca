import { Fragment, type ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

import { Colors, Radii } from '@/theme/tokens';

interface SettingsListCardProps {
  children: ReactNode[];
}

export function SettingsListCard({ children }: SettingsListCardProps) {
  return (
    <View style={styles.card}>
      {children.map((child, index) => (
        <Fragment key={index}>
          {index > 0 && <View style={styles.divider} />}
          {child}
        </Fragment>
      ))}
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
    overflow: 'hidden',
  },
  divider: {
    height: 1,
    backgroundColor: Colors.cardBorder,
    marginHorizontal: 16,
  },
});
