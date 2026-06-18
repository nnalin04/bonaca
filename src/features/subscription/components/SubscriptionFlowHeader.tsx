import { IconArrowLeft } from '@tabler/icons-react-native';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts } from '@/theme/tokens';

interface SubscriptionFlowHeaderProps {
  title: string;
  onPressBack?: () => void;
}

/**
 * Lightweight back + title header shared by the "Connecting a Wearable" /
 * subscription sub-flow screens, which are reached via push navigation
 * (not tabs) and so need their own back affordance.
 */
export function SubscriptionFlowHeader({ title, onPressBack }: SubscriptionFlowHeaderProps) {
  return (
    <View style={styles.header}>
      {onPressBack ? (
        <Pressable
          style={styles.backButton}
          onPress={onPressBack}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="Go back">
          <IconArrowLeft size={24} color={Colors.textPrimary} strokeWidth={1.75} />
        </Pressable>
      ) : (
        <View style={styles.backButtonPlaceholder} />
      )}
      <Text style={styles.title}>{title}</Text>
      <View style={styles.backButtonPlaceholder} />
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 16,
  },
  backButton: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  backButtonPlaceholder: {
    width: 40,
    height: 40,
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.textPrimary,
  },
});
