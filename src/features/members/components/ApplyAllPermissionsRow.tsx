import { IconCheck } from '@tabler/icons-react-native';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts } from '@/theme/tokens';

interface ApplyAllPermissionsRowProps {
  checked: boolean;
  granteeName: string;
  onToggle: () => void;
}

export function ApplyAllPermissionsRow({
  checked,
  granteeName,
  onToggle,
}: ApplyAllPermissionsRowProps) {
  return (
    <Pressable
      style={styles.applyAllRow}
      onPress={onToggle}
      accessibilityRole="checkbox"
      accessibilityState={{ checked }}
      accessibilityLabel={`Apply these permissions to all members${granteeName ? ` for ${granteeName}` : ''}`}
    >
      <View
        style={[styles.applyCheckbox, checked && styles.applyCheckboxChecked]}
      >
        {checked ? (
          <IconCheck size={18} color={Colors.white} strokeWidth={2.4} />
        ) : null}
      </View>
      <Text style={styles.applyAllText}>
        Apply these permissions to all members
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  applyAllRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    height: 24,
  },
  applyCheckbox: {
    width: 24,
    height: 24,
    borderRadius: 2,
    borderWidth: 2,
    borderColor: '#8f8f8f',
    alignItems: 'center',
    justifyContent: 'center',
  },
  applyCheckboxChecked: {
    borderColor: '#3b72db',
    backgroundColor: '#3b72db',
  },
  applyAllText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 18,
    lineHeight: 22,
    color: Colors.textSecondary,
  },
});
