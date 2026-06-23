import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface MemberTrialBannerProps {
  onPressConnect: () => void;
}

export function MemberTrialBanner({ onPressConnect }: MemberTrialBannerProps) {
  return (
    <View style={styles.trialBanner}>
      <View style={styles.bannerArcOne} />
      <View style={styles.bannerArcTwo} />
      <Text style={styles.trialCopy}>
        Try the full experience free for 7 days. Cancel anytime.
      </Text>
      <Pressable
        style={styles.connectButton}
        onPress={onPressConnect}
        accessibilityRole="button"
        accessibilityLabel="Connect wearable account"
      >
        <Text style={styles.connectButtonText}>Connect</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  trialBanner: {
    height: 108,
    borderRadius: Radii.card,
    backgroundColor: Colors.wearableCardBackground,
    overflow: 'hidden',
    paddingHorizontal: 12,
    paddingTop: 24,
  },
  bannerArcOne: {
    position: 'absolute',
    right: -74,
    top: -80,
    width: 260,
    height: 260,
    borderRadius: 130,
    borderWidth: 3,
    borderColor: Colors.wearableCardIconBorder,
    opacity: 0.45,
  },
  bannerArcTwo: {
    position: 'absolute',
    right: 25,
    top: -118,
    width: 342,
    height: 342,
    borderRadius: 171,
    borderWidth: 3,
    borderColor: Colors.wearableCardIconBorder,
    opacity: 0.45,
  },
  trialCopy: {
    width: 283,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.white,
  },
  connectButton: {
    marginTop: 16,
    alignSelf: 'flex-start',
    height: 24,
    borderRadius: Radii.card,
    paddingHorizontal: 16,
    backgroundColor: Colors.headerGradientEnd,
    alignItems: 'center',
    justifyContent: 'center',
  },
  connectButtonText: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.white,
  },
});
