import { IconDeviceWatchX } from '@tabler/icons-react-native';
import { StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface ConnectionErrorToastProps {
  message: string;
}

export function ConnectionErrorToast({ message }: ConnectionErrorToastProps) {
  return (
    <View style={styles.toast}>
      <IconDeviceWatchX size={20} color={Colors.errorText} strokeWidth={1.75} />
      <Text style={styles.message}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  toast: {
    minHeight: 40,
    width: '100%',
    borderRadius: Radii.toast,
    borderWidth: 1,
    borderColor: Colors.errorBorder,
    backgroundColor: Colors.errorBackground,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  message: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    color: Colors.errorText,
  },
});
