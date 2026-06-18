import { Pressable, StyleSheet, Text } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface PrimaryButtonProps {
  label: string;
  onPress?: () => void;
  disabled?: boolean;
}

export function PrimaryButton({ label, onPress, disabled }: PrimaryButtonProps) {
  return (
    <Pressable
      style={[styles.button, disabled && styles.buttonDisabled]}
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="button"
      accessibilityLabel={label}>
      <Text style={styles.label}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    height: 56,
    borderRadius: Radii.button,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonDisabled: {
    opacity: 0.5,
  },
  label: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.white,
  },
});
