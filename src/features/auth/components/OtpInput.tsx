import { useRef } from 'react';
import { StyleSheet, TextInput, View } from 'react-native';

import { Colors, Fonts } from '@/theme/tokens';

const OTP_LENGTH = 4;

interface OtpInputProps {
  value: string[];
  onChange: (value: string[]) => void;
  hasError?: boolean;
}

export function OtpInput({ value, onChange, hasError }: OtpInputProps) {
  const inputRefs = useRef<(TextInput | null)[]>([]);

  const handleChangeDigit = (digit: string, index: number) => {
    const sanitized = digit.replace(/[^0-9]/g, '');
    const next = [...value];
    next[index] = sanitized.slice(-1) ?? '';
    onChange(next);

    if (sanitized && index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyPress = (key: string, index: number) => {
    if (key === 'Backspace' && !value[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  return (
    <View style={styles.row}>
      {Array.from({ length: OTP_LENGTH }).map((_, index) => (
        <TextInput
          key={index}
          testID={`otp-digit-${index}`}
          ref={(ref) => {
            inputRefs.current[index] = ref;
          }}
          style={[styles.box, hasError && styles.boxError]}
          value={value[index] ?? ''}
          onChangeText={(digit) => handleChangeDigit(digit, index)}
          onKeyPress={({ nativeEvent }) => handleKeyPress(nativeEvent.key, index)}
          keyboardType="number-pad"
          maxLength={1}
          textAlign="center"
          accessibilityLabel={`OTP digit ${index + 1} of ${OTP_LENGTH}`}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 28,
  },
  box: {
    width: 56,
    height: 56,
    borderRadius: 8,
    borderWidth: 1.5,
    borderColor: Colors.inputBorder,
    backgroundColor: Colors.white,
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 24,
    color: Colors.textPrimary,
  },
  boxError: {
    borderColor: Colors.error,
  },
});
