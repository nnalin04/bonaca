import { IconChevronLeft } from '@tabler/icons-react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface ProfileHeaderProps {
  title: string;
  onPressBack: () => void;
}

export function ProfileHeader({ title, onPressBack }: ProfileHeaderProps) {
  return (
    <LinearGradient
      colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
      locations={[0, 1]}
      start={{ x: 0.5, y: 0 }}
      end={{ x: 0.5, y: 1 }}
      style={styles.header}>
      <Pressable
        style={styles.iconButton}
        onPress={onPressBack}
        hitSlop={8}
        accessibilityRole="button"
        accessibilityLabel="Go back">
        <IconChevronLeft size={24} color={Colors.white} strokeWidth={1.75} />
      </Pressable>
      <Text style={styles.title}>{title}</Text>
      <View style={styles.iconButton} />
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  header: {
    height: 103,
    borderBottomLeftRadius: Radii.headerCorner,
    borderBottomRightRadius: Radii.headerCorner,
    paddingHorizontal: 16,
    paddingBottom: 16,
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'space-between',
  },
  iconButton: {
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
    color: Colors.white,
  },
});
