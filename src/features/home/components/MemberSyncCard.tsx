import { IconRefresh } from '@tabler/icons-react-native';
import { Image } from 'expo-image';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface MemberSyncCardProps {
  avatarSource: number;
  displayName: string;
  syncLabel: string;
  onPress?: () => void;
}

export function MemberSyncCard({
  avatarSource,
  displayName,
  syncLabel,
  onPress,
}: MemberSyncCardProps) {
  return (
    <Pressable
      style={styles.card}
      onPress={onPress}
      accessibilityRole={onPress ? 'button' : undefined}
      accessibilityLabel={displayName}>
      <Image source={avatarSource} style={styles.avatar} contentFit="cover" />
      <View style={styles.textBlock}>
        <Text style={styles.name}>{displayName}</Text>
        <Text style={styles.sync}>{syncLabel}</Text>
      </View>
      <View style={styles.refreshPill}>
        <IconRefresh size={16} color={Colors.accent} strokeWidth={1.75} />
      </View>
    </Pressable>
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
    gap: 8,
    paddingHorizontal: 12,
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
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
  refreshPill: {
    width: 24,
    height: 24,
    borderRadius: 8,
    backgroundColor: Colors.tabBarTrack,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
