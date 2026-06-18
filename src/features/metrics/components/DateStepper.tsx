import { IconChevronLeft, IconChevronRight } from '@tabler/icons-react-native';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts } from '@/theme/tokens';

interface DateStepperProps {
  label: string;
  onPressPrevious: () => void;
  onPressNext: () => void;
  canGoNext?: boolean;
}

export function DateStepper({
  label,
  onPressPrevious,
  onPressNext,
  canGoNext = true,
}: DateStepperProps) {
  return (
    <View style={styles.row}>
      <Pressable
        onPress={onPressPrevious}
        hitSlop={8}
        accessibilityRole="button"
        accessibilityLabel="Previous period">
        <IconChevronLeft
          size={24}
          color={Colors.textPrimary}
          strokeWidth={1.75}
        />
      </Pressable>

      <Text style={styles.label}>{label}</Text>

      <Pressable
        onPress={onPressNext}
        hitSlop={8}
        disabled={!canGoNext}
        accessibilityRole="button"
        accessibilityLabel="Next period">
        <IconChevronRight
          size={24}
          color={canGoNext ? Colors.textPrimary : Colors.iconMuted}
          strokeWidth={1.75}
        />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
  },
  label: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.textPrimary,
    textAlign: 'center',
  },
});
