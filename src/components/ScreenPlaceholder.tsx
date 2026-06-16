import { StyleSheet, Text, View } from 'react-native';

import { Colors } from '@/theme/tokens';

interface ScreenPlaceholderProps {
  title: string;
  figmaSection: string;
}

export function ScreenPlaceholder({ title, figmaSection }: ScreenPlaceholderProps) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.section}>{figmaSection}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: Colors.background,
    padding: 24,
  },
  title: {
    fontSize: 20,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  section: {
    fontSize: 14,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
});
