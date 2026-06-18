import { IconChevronRight } from '@tabler/icons-react-native';
import { Image } from 'expo-image';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface ProfileSummaryCardProps {
  avatarSource: number;
  name: string;
  phoneNumber: string;
  onPress: () => void;
}

export function ProfileSummaryCard({
  avatarSource,
  name,
  phoneNumber,
  onPress,
}: ProfileSummaryCardProps) {
  return (
    <Pressable
      style={styles.card}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={`${name} profile details`}>
      <Image source={avatarSource} style={styles.avatar} contentFit="cover" />
      <View style={styles.textBlock}>
        <Text style={styles.name}>{name}</Text>
        <Text style={styles.phone}>{phoneNumber}</Text>
      </View>
      <View style={styles.chevron}>
        <IconChevronRight size={24} color={Colors.textSecondary} strokeWidth={1.75} />
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
    paddingHorizontal: 11,
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
  phone: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    color: Colors.textSecondary,
  },
  chevron: {
    width: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
