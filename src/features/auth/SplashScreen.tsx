import { useRouter } from 'expo-router';
import { useEffect } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { IconHeartbeat } from '@tabler/icons-react-native';

import { Colors, Fonts } from '@/theme/tokens';

const AUTO_ADVANCE_DELAY_MS = 1800;

export function SplashScreen() {
  const router = useRouter();

  useEffect(() => {
    const timer = setTimeout(() => {
      router.replace('/(auth)/login');
    }, AUTO_ADVANCE_DELAY_MS);

    return () => clearTimeout(timer);
  }, [router]);

  return (
    <LinearGradient
      colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
      locations={[0.03, 0.81]}
      start={{ x: 0.95, y: 0.29 }}
      end={{ x: 0.05, y: 0.71 }}
      style={styles.screen}>
      <View style={styles.content}>
        <View style={styles.iconBadge}>
          <IconHeartbeat size={40} color={Colors.white} strokeWidth={1.75} />
        </View>
        <Text style={styles.wordmark}>Bonaca</Text>
        <Text style={styles.tagline}>Stay close to the ones who matter</Text>
      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    alignItems: 'center',
    gap: 12,
  },
  iconBadge: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: 'rgba(255,255,255,0.12)',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  wordmark: {
    fontFamily: Fonts.family,
    fontWeight: '700',
    fontSize: 32,
    lineHeight: 40,
    color: Colors.white,
  },
  tagline: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textOnDark,
  },
});
