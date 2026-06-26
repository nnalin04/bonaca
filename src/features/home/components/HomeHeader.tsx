import { IconBell, IconUserCircle } from '@tabler/icons-react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

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
  const insets = useSafeAreaInsets();
  return (
    <LinearGradient
      colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
      locations={[0, 0.95]}
      start={{ x: 0.97, y: -0.43 }}
      end={{ x: 0.21, y: 1.21 }}
      style={[styles.header, { paddingTop: insets.top + 16, paddingBottom: 16 }]}>
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
          accessibilityLabel="Open notifications">
          <IconBell size={28} color={Colors.tabBarTrack} strokeWidth={1.75} />
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
          accessibilityLabel="Open profile">
          <IconUserCircle size={28} color={Colors.tabBarTrack} strokeWidth={1.75} />
        </Pressable>
      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  header: {
    borderBottomLeftRadius: Radii.headerCorner,
    borderBottomRightRadius: Radii.headerCorner,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'flex-start',
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
    gap: 0,
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
