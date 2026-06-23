import { StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts } from '@/theme/tokens';

export function PaymentGatewayScreen() {
  return (
    <View style={styles.screen}>
      <Text style={styles.title}>Payment Gateway</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: Colors.background,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  title: {
    width: '100%',
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
});
