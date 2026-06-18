import { IconDeviceWatchPlus } from '@tabler/icons-react-native';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface WearableConnectCardProps {
  onPressConnect: () => void;
}

export function WearableConnectCard({ onPressConnect }: WearableConnectCardProps) {
  return (
    <View style={styles.card}>
      <View style={styles.iconWrap}>
        <IconDeviceWatchPlus size={32} color={Colors.white} strokeWidth={1.5} />
      </View>
      <Text style={styles.label}>Connect your wearable account</Text>
      <Pressable
        style={styles.button}
        onPress={onPressConnect}
        accessibilityRole="button"
        accessibilityLabel="Connect your wearable account">
        <Text style={styles.buttonText}>Connect</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    height: 80,
    width: '100%',
    borderRadius: Radii.card,
    backgroundColor: Colors.headerGradientStart,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 12,
  },
  iconWrap: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: 'rgba(255,255,255,0.12)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  label: {
    flex: 1,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textOnDark,
  },
  button: {
    height: 24,
    paddingHorizontal: 8,
    borderRadius: Radii.cta,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.white,
  },
});
