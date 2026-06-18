import { Image, type ImageSource } from 'expo-image';
import { StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface NotificationRowProps {
  avatarSource?: ImageSource | number;
  initials: string;
  title: string;
  body: string;
  displayTime: string;
  unread: boolean;
}

export function NotificationRow({
  avatarSource,
  initials,
  title,
  body,
  displayTime,
  unread,
}: NotificationRowProps) {
  return (
    <View style={styles.card}>
      {avatarSource ? (
        <Image source={avatarSource} style={styles.avatar} contentFit="cover" />
      ) : (
        <View style={styles.avatarFallback}>
          <Text style={styles.avatarInitials}>{initials}</Text>
        </View>
      )}

      <View style={styles.textBlock}>
        <Text style={styles.title} numberOfLines={1}>
          {title}
        </Text>
        <Text style={styles.body} numberOfLines={2}>
          {body}
        </Text>
      </View>

      <View style={styles.metaBlock}>
        {unread && <View style={styles.unreadDot} />}
        <Text style={styles.time}>{displayTime}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    minHeight: 80,
    width: '100%',
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 12,
    paddingVertical: 12,
  },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
  },
  avatarFallback: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarInitials: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 14,
    color: Colors.white,
  },
  textBlock: {
    flex: 1,
    gap: 4,
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  body: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
  metaBlock: {
    alignItems: 'flex-end',
    gap: 6,
  },
  unreadDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: Colors.badge,
  },
  time: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 11,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
});
