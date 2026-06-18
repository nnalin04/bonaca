import { IconChevronLeft } from '@tabler/icons-react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface NotificationsHeaderProps {
  onPressBack: () => void;
}

export function NotificationsHeader({ onPressBack }: NotificationsHeaderProps) {
  return (
    <LinearGradient
      colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
      locations={[0.03, 0.81]}
      start={{ x: 0.95, y: 0.29 }}
      end={{ x: 0.05, y: 0.71 }}
      style={styles.header}>
      <Pressable
        style={styles.backButton}
        onPress={onPressBack}
        hitSlop={8}
        accessibilityRole="button"
        accessibilityLabel="Go back">
        <IconChevronLeft size={24} color={Colors.white} strokeWidth={2} />
      </Pressable>
      <Text style={styles.title}>Notifications</Text>
      <View style={styles.spacer} />
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  header: {
    height: 103,
    borderBottomLeftRadius: Radii.headerCorner,
    borderBottomRightRadius: Radii.headerCorner,
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 16,
  },
  backButton: {
    width: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.white,
  },
  spacer: {
    width: 24,
  },
});
