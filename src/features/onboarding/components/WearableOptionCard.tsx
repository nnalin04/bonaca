import { IconChevronRight } from '@tabler/icons-react-native';
import { Image, type ImageSource } from 'expo-image';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface WearableOptionCardProps {
  label: string;
  iconSource: ImageSource | number;
  onPress?: () => void;
}

export function WearableOptionCard({ label, iconSource, onPress }: WearableOptionCardProps) {
  return (
    <Pressable
      style={styles.card}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}>
      <View style={styles.leading}>
        <Image source={iconSource} style={styles.icon} contentFit="cover" />
        <Text style={styles.label}>{label}</Text>
      </View>
      <IconChevronRight size={20} color={Colors.textSecondary} strokeWidth={1.75} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    height: 56,
    borderRadius: Radii.cta + 4,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
  },
  leading: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  icon: {
    width: 32,
    height: 32,
    borderRadius: 16,
  },
  label: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
  },
});
