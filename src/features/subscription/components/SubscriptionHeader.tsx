import { IconChevronLeft } from '@tabler/icons-react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Pressable, StyleSheet, Text } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface SubscriptionHeaderProps {
  title: string;
  onPressBack?: () => void;
}

export function SubscriptionHeader({ title, onPressBack }: SubscriptionHeaderProps) {
  return (
    <LinearGradient
      colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
      locations={[0, 0.9504]}
      start={{ x: 0.9705, y: -0.432 }}
      end={{ x: 0.2064, y: 1.2136 }}
      style={styles.header}>
      {onPressBack ? (
        <Pressable
          style={styles.backButton}
          onPress={onPressBack}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="Go back">
          <IconChevronLeft size={24} color={Colors.white} strokeWidth={2} />
        </Pressable>
      ) : (
        <Text style={styles.backSpacer} />
      )}
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.backSpacer} />
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
  backButton: {
    width: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  backSpacer: {
    width: 24,
    height: 24,
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
});
