import { useRef } from 'react';
import { NativeSyntheticEvent, StyleSheet, TextInput, TextInputKeyPressEventData, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface OtpInputProps {
  length: number;
  value: string;
  onChange: (value: string) => void;
  hasError?: boolean;
}

export function OtpInput({ length, value, onChange, hasError }: OtpInputProps) {
  const inputs = useRef<(TextInput | null)[]>([]);
  const digits = Array.from({ length }, (_, i) => value[i] ?? '');

  const handleChangeDigit = (text: string, index: number) => {
    const sanitized = text.replace(/[^0-9]/g, '');
    if (!sanitized) return;
    const nextDigit = sanitized[sanitized.length - 1];
    const nextValue = value.split('');
    nextValue[index] = nextDigit;
    onChange(nextValue.join('').slice(0, length));

    if (index < length - 1) {
      inputs.current[index + 1]?.focus();
    }
  };

  const handleKeyPress = (
    event: NativeSyntheticEvent<TextInputKeyPressEventData>,
    index: number,
  ) => {
    if (event.nativeEvent.key === 'Backspace' && !digits[index] && index > 0) {
      const nextValue = value.split('');
      nextValue[index - 1] = '';
      onChange(nextValue.join(''));
      inputs.current[index - 1]?.focus();
    }
  };

  return (
    <View style={styles.row}>
      {digits.map((digit, index) => (
        <TextInput
          key={index}
          ref={(ref) => {
            inputs.current[index] = ref;
          }}
          style={[styles.box, hasError && styles.boxError, digit && styles.boxFilled]}
          value={digit}
          onChangeText={(text) => handleChangeDigit(text, index)}
          onKeyPress={(event) => handleKeyPress(event, index)}
          keyboardType="number-pad"
          maxLength={1}
          autoFocus={index === 0}
          accessibilityLabel={`OTP digit ${index + 1}`}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
  },
  box: {
    flex: 1,
    height: 56,
    borderRadius: Radii.cta,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    textAlign: 'center',
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 20,
    color: Colors.textPrimary,
  },
  boxFilled: {
    borderColor: Colors.accent,
  },
  boxError: {
    borderColor: Colors.badge,
  },
});
