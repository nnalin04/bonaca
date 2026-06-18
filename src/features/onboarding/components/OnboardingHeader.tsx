import { LinearGradient } from 'expo-linear-gradient';
import { StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BackButton } from '@/features/auth/components/BackButton';
import { Colors, Fonts, Radii } from '@/theme/tokens';

interface OnboardingHeaderProps {
  title: string;
  onBack: () => void;
}

export function OnboardingHeader({ title, onBack }: OnboardingHeaderProps) {
  const insets = useSafeAreaInsets();

  return (
    <LinearGradient
      colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
      locations={[0.03, 0.81]}
      start={{ x: 0.95, y: 0.29 }}
      end={{ x: 0.05, y: 0.71 }}
      style={[styles.header, { paddingTop: insets.top + 8 }]}>
      <View style={styles.row}>
        <BackButton onPress={onBack} />
        <Text style={styles.title}>{title}</Text>
        <View style={styles.spacer} />
      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  header: {
    height: 103,
    borderBottomLeftRadius: Radii.headerCorner,
    borderBottomRightRadius: Radii.headerCorner,
    paddingHorizontal: 16,
    justifyContent: 'flex-end',
    paddingBottom: 16,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  title: {
    flex: 1,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.white,
    textAlign: 'center',
  },
  spacer: {
    width: 40,
  },
});
