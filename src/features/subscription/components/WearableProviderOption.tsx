import { IconBrandApple, IconChevronRight, IconDeviceWatch } from '@tabler/icons-react-native';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';
import type { WearableProvider } from '@/types';

interface WearableProviderOptionProps {
  provider: WearableProvider;
  label: string;
  description: string;
  available: boolean;
  onPress: () => void;
}

const ICONS: Record<WearableProvider, typeof IconBrandApple> = {
  'apple-health': IconBrandApple,
  'health-connect': IconDeviceWatch,
  fitbit: IconDeviceWatch,
  garmin: IconDeviceWatch,
};

export function WearableProviderOption({
  provider,
  label,
  description,
  available,
  onPress,
}: WearableProviderOptionProps) {
  const Icon = ICONS[provider];

  return (
    <Pressable
      style={({ pressed }) => [
        styles.card,
        !available && styles.cardDisabled,
        pressed && available && styles.cardPressed,
      ]}
      onPress={onPress}
      disabled={!available}
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled: !available }}>
      <View style={styles.iconWrap}>
        <Icon size={28} color={available ? Colors.accent : Colors.iconMuted} strokeWidth={1.75} />
      </View>
      <View style={styles.textBlock}>
        <Text style={[styles.label, !available && styles.labelDisabled]}>{label}</Text>
        <Text style={styles.description}>
          {available ? description : 'Coming soon'}
        </Text>
      </View>
      {available && (
        <IconChevronRight size={20} color={Colors.textSecondary} strokeWidth={1.75} />
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    minHeight: 76,
    width: '100%',
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  cardPressed: {
    backgroundColor: Colors.background,
  },
  cardDisabled: {
    opacity: 0.6,
  },
  iconWrap: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.background,
  },
  textBlock: {
    flex: 1,
    gap: 2,
  },
  label: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textPrimary,
  },
  labelDisabled: {
    color: Colors.textSecondary,
  },
  description: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 13,
    lineHeight: 18,
    color: Colors.textSecondary,
  },
});
