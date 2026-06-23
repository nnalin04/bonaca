import { IconChevronDown } from '@tabler/icons-react-native';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface ProfileFieldProps {
  label: string;
  required?: boolean;
  placeholder: string;
  value: string;
  onChangeText?: (value: string) => void;
  onPress?: () => void;
  editable?: boolean;
}

export function ProfileField({
  label,
  required,
  placeholder,
  value,
  onChangeText,
  onPress,
  editable = true,
}: ProfileFieldProps) {
  const isSelect = !editable;
  const showAsterisk = required && value.length === 0;

  const content = (
    <View style={styles.container}>
      {isSelect ? (
        <Text style={value ? styles.valueText : styles.placeholderText}>
          {value || placeholder}
        </Text>
      ) : (
        <TextInput
          style={styles.input}
          value={value}
          onChangeText={onChangeText}
          placeholder={placeholder}
          placeholderTextColor={Colors.placeholderText}
        />
      )}
      {isSelect && <IconChevronDown size={24} color={Colors.inputChevron} strokeWidth={1.75} />}
    </View>
  );

  return (
    <View style={styles.field}>
      <Text style={styles.label}>
        {label}
        {showAsterisk && <Text style={styles.asterisk}> *</Text>}
      </Text>
      {isSelect ? (
        <Pressable
          onPress={onPress}
          accessibilityRole="button"
          accessibilityLabel={`${label} field, ${value || placeholder}`}>
          {content}
        </Pressable>
      ) : (
        content
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  field: {
    gap: 6,
  },
  label: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  asterisk: {
    color: Colors.error,
  },
  container: {
    height: 48,
    borderRadius: Radii.cta,
    borderWidth: 1,
    borderColor: Colors.inputBorderSubtle,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 12,
  },
  input: {
    flex: 1,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.textSecondary,
    padding: 0,
  },
  valueText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.textSecondary,
  },
  placeholderText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.placeholderText,
  },
});
