import { IconUsers } from '@tabler/icons-react-native';
import { StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

export function EmptySharedState() {
  return (
    <View style={styles.card}>
      <View style={styles.content}>
        <IconUsers size={80} color={Colors.emptyStateIcon} strokeWidth={1.5} />
        <View style={styles.textBlock}>
          <Text style={styles.title}>Nothing here yet</Text>
          <Text style={styles.subtitle}>
            When someone adds you as a family member, their updates will appear here
          </Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    height: 510,
    width: '100%',
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 48,
  },
  content: {
    alignItems: 'center',
    gap: 24,
  },
  textBlock: {
    alignItems: 'center',
    gap: 8,
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.textPrimary,
    textAlign: 'center',
  },
  subtitle: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
});
