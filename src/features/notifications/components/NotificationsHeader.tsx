import { IconChevronLeft } from '@tabler/icons-react-native';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts } from '@/theme/tokens';

interface NotificationsHeaderProps {
  onPressBack: () => void;
}

export function NotificationsHeader({ onPressBack }: NotificationsHeaderProps) {
  return (
    <View style={styles.header}>
      <Pressable
        style={styles.backButton}
        onPress={onPressBack}
        hitSlop={8}
        accessibilityRole="button"
        accessibilityLabel="Go back">
        <IconChevronLeft size={24} color={Colors.textPrimary} strokeWidth={1.75} />
      </Pressable>
      <Text style={styles.title}>Notifications</Text>
      <View style={styles.spacer} />
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    height: 56,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
  },
  backButton: {
    width: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.textPrimary,
  },
  spacer: {
    width: 24,
  },
});
