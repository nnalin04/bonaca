import { IconChevronDown } from '@tabler/icons-react-native';
import { StyleSheet, Text, TextInput, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface MobileNumberFieldProps {
  countryCode: string;
  value: string;
  onChangeText: (value: string) => void;
}

export function MobileNumberField({ countryCode, value, onChangeText }: MobileNumberFieldProps) {
  return (
    <View style={styles.field}>
      <View style={styles.countryCode}>
        <Text style={styles.countryCodeText}>{countryCode}</Text>
        <IconChevronDown size={20} color={Colors.textPrimary} strokeWidth={1.75} />
      </View>
      <View style={styles.divider} />
      <TextInput
        style={styles.input}
        value={value}
        onChangeText={onChangeText}
        placeholder="Mobile number"
        placeholderTextColor={Colors.placeholderText}
        keyboardType="number-pad"
        maxLength={10}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  field: {
    height: 48,
    borderRadius: Radii.cta,
    borderWidth: 1,
    borderColor: Colors.inputBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    gap: 8,
  },
  countryCode: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  countryCodeText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  divider: {
    width: 1,
    height: 24,
    backgroundColor: Colors.inputBorder,
  },
  input: {
    flex: 1,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 20,
    color: Colors.textPrimary,
    padding: 0,
  },
});
