import { IconChevronLeft, IconDotsVertical } from '@tabler/icons-react-native';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface MemberDetailsHeaderProps {
  avatarSource: number;
  displayName: string;
  statusMessage?: string;
  onPressBack: () => void;
  onPressMenu?: () => void;
}

export function MemberDetailsHeader({
  avatarSource,
  displayName,
  statusMessage,
  onPressBack,
  onPressMenu,
}: MemberDetailsHeaderProps) {
  return (
    <LinearGradient
      colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
      locations={[0, 0.9504]}
      start={{ x: 0.9705, y: -0.432 }}
      end={{ x: 0.2064, y: 1.2136 }}
      style={styles.header}>
      <Pressable
        style={styles.backButton}
        onPress={onPressBack}
        hitSlop={8}
        accessibilityRole="button"
        accessibilityLabel="Go back">
        <IconChevronLeft size={24} color={Colors.white} strokeWidth={1.75} />
      </Pressable>

      <View style={styles.profileRow}>
        <Image source={avatarSource} style={styles.avatar} contentFit="cover" />
        <View style={styles.textBlock}>
          <Text style={styles.name}>{displayName}</Text>
          {statusMessage ? (
            <Text style={styles.status}>{statusMessage}</Text>
          ) : null}
        </View>

        {onPressMenu ? (
          <Pressable
            style={styles.menuButton}
            onPress={onPressMenu}
            hitSlop={8}
            accessibilityRole="button"
            accessibilityLabel="More options">
            <IconDotsVertical
              size={24}
              color={Colors.white}
              strokeWidth={1.75}
            />
          </Pressable>
        ) : null}
      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  header: {
    height: 175,
    paddingTop: 63,
    paddingBottom: 16,
    paddingHorizontal: 16,
    borderBottomLeftRadius: Radii.headerCorner,
    borderBottomRightRadius: Radii.headerCorner,
    gap: 16,
  },
  backButton: {
    width: 24,
    height: 24,
  },
  profileRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    borderWidth: 1,
    borderColor: Colors.memberHeaderAvatarBorder,
  },
  textBlock: {
    flex: 1,
    gap: 2,
  },
  name: {
    fontFamily: Fonts.family,
    fontWeight: '500',
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
  menuButton: {
    width: 24,
    height: 24,
    marginRight: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
