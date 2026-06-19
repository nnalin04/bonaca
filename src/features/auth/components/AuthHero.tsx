import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { StyleSheet, Text } from 'react-native';

import { Colors, Fonts } from '@/theme/tokens';

interface AuthHeroProps {
  tagline: string;
  height: number;
}

export function AuthHero({ tagline, height }: AuthHeroProps) {
  return (
    <LinearGradient
      colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
      locations={[0, 0.95]}
      start={{ x: 0.97, y: -0.43 }}
      end={{ x: 0.21, y: 1.21 }}
      style={[styles.hero, { height }]}>
      <Image
        source={require('../../../../assets/images/brand/bonaca-mark.png')}
        style={styles.mark}
        contentFit="contain"
      />
      <Text style={styles.tagline}>{tagline}</Text>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  hero: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 18,
    gap: 24,
  },
  mark: {
    width: 80,
    height: 80,
  },
  tagline: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 20,
    lineHeight: 28,
    color: Colors.textOnDark,
    textAlign: 'center',
  },
});
