import {
  IconRefresh,
  IconTrendingDown,
  IconTrendingUp,
  IconWaveSine,
} from '@tabler/icons-react-native';
import { Image } from 'expo-image';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';
import type { Member } from '@/types';

const alertChips = [
  {
    label: '2 High',
    icon: IconTrendingUp,
    color: '#e97961',
    backgroundColor: 'rgba(233, 121, 97, 0.1)',
  },
  {
    label: '2 Low',
    icon: IconTrendingDown,
    color: '#5b8def',
    backgroundColor: 'rgba(91, 141, 239, 0.1)',
  },
  {
    label: '2 Normal',
    icon: IconWaveSine,
    color: '#8b6f9c',
    backgroundColor: 'rgba(139, 111, 156, 0.1)',
  },
];

interface SharedMemberCardProps {
  member: Member;
  syncLabel: string;
  avatarSource: number;
  onPress: () => void;
}

export function SharedMemberCard({
  member,
  syncLabel,
  avatarSource,
  onPress,
}: SharedMemberCardProps) {
  return (
    <Pressable
      style={styles.sharedCard}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={member.nickname ?? member.name}
    >
      <Image
        source={avatarSource}
        style={styles.sharedAvatar}
        contentFit="cover"
      />
      <View style={styles.sharedContent}>
        <View style={styles.sharedTopRow}>
          <View style={styles.sharedTextBlock}>
            <Text style={styles.sharedName}>
              {member.nickname ?? member.name}
            </Text>
            <Text style={styles.sharedSync}>{syncLabel}</Text>
          </View>
          <IconRefresh size={24} color={Colors.accent} strokeWidth={1.75} />
        </View>

        <View style={styles.alertRow}>
          {alertChips.map((chip) => {
            const ChipIcon = chip.icon;
            return (
              <View
                key={chip.label}
                style={[
                  styles.alertChip,
                  { backgroundColor: chip.backgroundColor },
                ]}
              >
                <ChipIcon size={14} color={chip.color} strokeWidth={1.75} />
                <Text style={styles.alertChipText}>{chip.label}</Text>
              </View>
            );
          })}
        </View>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  sharedCard: {
    width: '100%',
    height: 102,
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'flex-start',
    padding: 12,
    gap: 8,
  },
  sharedAvatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
  },
  sharedContent: {
    flex: 1,
  },
  sharedTopRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
  },
  sharedTextBlock: {
    gap: 2,
  },
  sharedName: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textPrimary,
  },
  sharedSync: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
  alertRow: {
    marginTop: 14,
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 8,
  },
  alertChip: {
    height: 24,
    minWidth: 72,
    borderRadius: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingHorizontal: 8,
  },
  alertChipText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
});
