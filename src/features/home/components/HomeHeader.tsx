import { IconBell, IconUserCircle } from '@tabler/icons-react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface HomeHeaderProps {
  greetingName: string;
  statusMessage: string;
  unreadNotificationCount: number;
  onPressNotifications: () => void;
  onPressProfile: () => void;
}

export function HomeHeader({
  greetingName,
  statusMessage,
  unreadNotificationCount,
  onPressNotifications,
  onPressProfile,
}: HomeHeaderProps) {
  return (
    <LinearGradient
      colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
      locations={[0.03, 0.81]}
      start={{ x: 0.95, y: 0.29 }}
      end={{ x: 0.05, y: 0.71 }}
      style={styles.header}>
      <View style={styles.greetingBlock}>
        <Text style={styles.greeting}>Hello {greetingName}!</Text>
        <Text style={styles.status}>{statusMessage}</Text>
      </View>

      <View style={styles.actions}>
        <Pressable
          style={styles.cta}
          onPress={onPressNotifications}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="Notifications">
          <IconBell size={28} color={Colors.white} strokeWidth={1.75} />
          {unreadNotificationCount > 0 && (
            <View style={styles.badge}>
              <Text style={styles.badgeText}>{unreadNotificationCount}</Text>
            </View>
          )}
        </Pressable>

        <Pressable
          style={styles.cta}
          onPress={onPressProfile}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="Profile">
          <IconUserCircle size={28} color={Colors.white} strokeWidth={1.75} />
        </Pressable>
      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  header: {
    height: 125,
    borderBottomLeftRadius: Radii.headerCorner,
    borderBottomRightRadius: Radii.headerCorner,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  greetingBlock: {
    gap: 2,
  },
  greeting: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.white,
  },
  status: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textOnDark,
  },
  actions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  cta: {
    width: 48,
    height: 48,
    borderRadius: Radii.cta,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badge: {
    position: 'absolute',
    top: 4,
    right: 4,
    minWidth: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: Colors.badge,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 4,
  },
  badgeText: {
    fontSize: 11.67,
    lineHeight: 11.67,
    fontWeight: '500',
    color: Colors.white,
  },
});
