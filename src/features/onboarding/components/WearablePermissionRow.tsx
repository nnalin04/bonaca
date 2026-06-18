import type { ComponentProps, ComponentType } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { IconBluetooth } from '@tabler/icons-react-native';

import { Colors, Fonts } from '@/theme/tokens';

interface WearablePermissionRowProps {
  icon: ComponentType<ComponentProps<typeof IconBluetooth>>;
  title: string;
  description: string;
}

export function WearablePermissionRow({ icon: IconComponent, title, description }: WearablePermissionRowProps) {
  return (
    <View style={styles.row}>
      <View style={styles.iconBadge}>
        <IconComponent size={22} color={Colors.accent} strokeWidth={1.75} />
      </View>
      <View style={styles.textBlock}>
        <Text style={styles.title}>{title}</Text>
        <Text style={styles.description}>{description}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 12,
  },
  iconBadge: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: Colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  textBlock: {
    flex: 1,
    gap: 2,
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  description: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 13,
    lineHeight: 18,
    color: Colors.textSecondary,
  },
});
