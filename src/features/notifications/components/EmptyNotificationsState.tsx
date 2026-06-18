import { IconBellOff } from '@tabler/icons-react-native';
import { StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

export function EmptyNotificationsState() {
  return (
    <View style={styles.card}>
      <View style={styles.content}>
        <IconBellOff size={80} color={Colors.iconMuted} strokeWidth={1.5} />
        <View style={styles.textBlock}>
          <Text style={styles.title}>No notifications yet</Text>
          <Text style={styles.subtitle}>
            Updates about your family’s health and account will show up here
          </Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    minHeight: 400,
    width: '100%',
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 48,
    paddingVertical: 48,
  },
  content: {
    alignItems: 'center',
    gap: 24,
  },
  textBlock: {
    alignItems: 'center',
    gap: 8,
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.textPrimary,
    textAlign: 'center',
  },
  subtitle: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
});
