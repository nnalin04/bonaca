import { IconChevronRight } from '@tabler/icons-react-native';
import type { ComponentType } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts } from '@/theme/tokens';

interface TablerIconProps {
  size?: number;
  color?: string;
  strokeWidth?: number;
}

interface SettingsListItemProps {
  icon: ComponentType<TablerIconProps>;
  label: string;
  destructive?: boolean;
  onPress: () => void;
}

export function SettingsListItem({
  icon: Icon,
  label,
  destructive = false,
  onPress,
}: SettingsListItemProps) {
  const labelColor = destructive ? Colors.badge : Colors.textPrimary;

  return (
    <Pressable
      style={styles.row}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}>
      <View style={styles.leading}>
        <Icon size={24} color={labelColor} strokeWidth={1.75} />
        <Text style={[styles.label, { color: labelColor }]}>{label}</Text>
      </View>
      <IconChevronRight size={24} color={Colors.textSecondary} strokeWidth={1.75} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    height: 56,
    width: '100%',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
  },
  leading: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  label: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
  },
});
