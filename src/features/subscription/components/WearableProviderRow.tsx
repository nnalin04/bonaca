import { IconChevronRight } from '@tabler/icons-react-native';
import { Image, type ImageSource } from 'expo-image';
import { Pressable, StyleSheet, Text } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';
import type { WearableProvider } from '@/types';

interface WearableProviderRowProps {
  provider: WearableProvider;
  label: string;
  iconSource: ImageSource | number;
  onPress?: () => void;
}

export function WearableProviderRow({
  label,
  iconSource,
  onPress,
}: WearableProviderRowProps) {
  return (
    <Pressable
      style={styles.row}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}>
      <Image source={iconSource} style={styles.iconCircle} contentFit="cover" />
      <Text style={styles.label}>{label}</Text>
      <IconChevronRight size={20} color={Colors.textSecondary} strokeWidth={2} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    height: 56,
    width: '100%',
    borderRadius: Radii.row,
    borderWidth: 1,
    borderColor: Colors.rowBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 12,
  },
  iconCircle: {
    width: 32,
    height: 32,
    borderRadius: 16,
  },
  label: {
    flex: 1,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    color: Colors.textSecondary,
  },
});
