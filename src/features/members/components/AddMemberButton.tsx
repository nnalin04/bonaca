import { IconUserPlus } from '@tabler/icons-react-native';
import { Pressable, StyleSheet, Text } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface AddMemberButtonProps {
  onPress: () => void;
}

export function AddMemberButton({ onPress }: AddMemberButtonProps) {
  return (
    <Pressable
      style={styles.addButton}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel="Add Member"
    >
      <IconUserPlus size={24} color={Colors.accent} strokeWidth={1.75} />
      <Text style={styles.addLabel}>Add Member</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  addButton: {
    height: 56,
    borderRadius: Radii.row,
    backgroundColor: Colors.tabBarTrack,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  addLabel: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.accent,
  },
});
