import { IconChevronRight } from '@tabler/icons-react-native';
import type { ComponentType } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface TablerIconProps {
  size?: number;
  color?: string;
  strokeWidth?: number;
}

interface SettingsListItemProps {
  icon: ComponentType<TablerIconProps>;
  label: string;
  onPress: () => void;
}

export function SettingsListItem({
  icon: Icon,
  label,
  onPress,
}: SettingsListItemProps) {
  return (
    <Pressable
      style={styles.row}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}>
      <View style={styles.leading}>
        <Icon size={24} color={Colors.accent} strokeWidth={1.75} />
        <Text style={styles.label}>{label}</Text>
      </View>
      <IconChevronRight size={24} color={Colors.textSecondary} strokeWidth={1.75} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    height: 56,
    width: '100%',
    borderWidth: 1,
    borderColor: Colors.onboardingCardBorder,
    borderRadius: Radii.row,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
  },
  leading: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    flexShrink: 1,
  },
  label: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
  },
});
