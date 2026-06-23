import { IconChevronLeft } from '@tabler/icons-react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface OnboardingHeaderProps {
  title: string;
  onBack: () => void;
}

export function OnboardingHeader({ title, onBack }: OnboardingHeaderProps) {
  return (
    <LinearGradient
      colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
      locations={[0, 0.95]}
      start={{ x: 0.97, y: -0.43 }}
      end={{ x: 0.21, y: 1.21 }}
      style={styles.header}>
      <View style={styles.row}>
        <Pressable
          style={styles.backButton}
          onPress={onBack}
          hitSlop={12}
          accessibilityRole="button"
          accessibilityLabel="Go back">
          <IconChevronLeft size={24} color={Colors.white} strokeWidth={2.5} />
        </Pressable>
        <Text style={styles.title}>{title}</Text>
        <View style={styles.spacer} />
      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  header: {
    height: 103,
    borderBottomLeftRadius: Radii.headerCorner,
    borderBottomRightRadius: Radii.headerCorner,
    paddingHorizontal: 16,
    justifyContent: 'flex-end',
    paddingBottom: 16,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    height: 24,
  },
  backButton: {
    width: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    flex: 1,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.white,
    textAlign: 'center',
  },
  spacer: {
    width: 24,
  },
});
