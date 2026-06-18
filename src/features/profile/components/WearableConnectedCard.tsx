import { IconDeviceWatch } from '@tabler/icons-react-native';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface WearableConnectedCardProps {
  providerLabel: string;
  syncLabel: string;
  onPressDisconnect: () => void;
}

export function WearableConnectedCard({
  providerLabel,
  syncLabel,
  onPressDisconnect,
}: WearableConnectedCardProps) {
  return (
    <View style={styles.card}>
      <View style={styles.iconWrap}>
        <IconDeviceWatch size={28} color={Colors.accent} strokeWidth={1.5} />
      </View>
      <View style={styles.textBlock}>
        <Text style={styles.name}>{providerLabel}</Text>
        <Text style={styles.sync}>{syncLabel}</Text>
      </View>
      <Pressable
        style={styles.button}
        onPress={onPressDisconnect}
        accessibilityRole="button"
        accessibilityLabel={`Disconnect ${providerLabel}`}>
        <Text style={styles.buttonText}>Disconnect</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    height: 80,
    width: '100%',
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 12,
  },
  iconWrap: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: Colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  textBlock: {
    flex: 1,
    gap: 2,
  },
  name: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textPrimary,
  },
  sync: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    color: Colors.textSecondary,
  },
  button: {
    height: 24,
    paddingHorizontal: 8,
    borderRadius: Radii.cta,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
});
