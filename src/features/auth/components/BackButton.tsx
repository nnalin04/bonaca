import { IconChevronLeft } from '@tabler/icons-react-native';
import { Pressable, StyleSheet } from 'react-native';

import { Colors } from '@/theme/tokens';

interface BackButtonProps {
  onPress: () => void;
  tone?: 'light' | 'dark';
}

export function BackButton({ onPress, tone = 'light' }: BackButtonProps) {
  return (
    <Pressable
      style={[styles.button, tone === 'light' && styles.buttonOnDark]}
      onPress={onPress}
      hitSlop={8}
      accessibilityRole="button"
      accessibilityLabel="Go back">
      <IconChevronLeft
        size={20}
        color={tone === 'light' ? Colors.textPrimary : Colors.white}
        strokeWidth={2}
      />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    width: 40,
    height: 40,
    borderRadius: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonOnDark: {
    backgroundColor: Colors.tabBarTrack,
  },
});
