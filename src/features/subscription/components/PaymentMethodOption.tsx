import {
  IconBrandApple,
  IconBrandMastercard,
  IconBrandPaypal,
  IconCircleCheckFilled,
  IconCreditCard,
  IconQrcode,
} from '@tabler/icons-react-native';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';
import type { PaymentMethod, PaymentMethodType } from '@/types';

interface PaymentMethodOptionProps {
  method: PaymentMethod;
  selected: boolean;
  onPress: () => void;
}

const ICONS: Record<PaymentMethodType, typeof IconCreditCard> = {
  upi: IconQrcode,
  paypal: IconBrandPaypal,
  amex: IconCreditCard,
  mastercard: IconBrandMastercard,
  'apple-pay': IconBrandApple,
};

export function PaymentMethodOption({ method, selected, onPress }: PaymentMethodOptionProps) {
  const Icon = ICONS[method.type];

  return (
    <Pressable
      style={({ pressed }) => [
        styles.card,
        selected && styles.cardSelected,
        pressed && styles.cardPressed,
      ]}
      onPress={onPress}
      accessibilityRole="radio"
      accessibilityLabel={method.label}
      accessibilityState={{ selected }}>
      <View style={styles.iconWrap}>
        <Icon size={24} color={Colors.accent} strokeWidth={1.75} />
      </View>
      <View style={styles.textBlock}>
        <Text style={styles.label}>{method.label}</Text>
        {method.detail ? <Text style={styles.detail}>{method.detail}</Text> : null}
      </View>
      {selected ? (
        <IconCircleCheckFilled size={22} color={Colors.accent} />
      ) : (
        <View style={styles.radioOuter} />
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    minHeight: 64,
    width: '100%',
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  cardSelected: {
    borderColor: Colors.accent,
    borderWidth: 1.5,
  },
  cardPressed: {
    backgroundColor: Colors.background,
  },
  iconWrap: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.background,
  },
  textBlock: {
    flex: 1,
    gap: 2,
  },
  label: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 15,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  detail: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
  radioOuter: {
    width: 22,
    height: 22,
    borderRadius: 11,
    borderWidth: 1.5,
    borderColor: Colors.cardBorder,
  },
});
