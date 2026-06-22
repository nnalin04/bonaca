import { useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface NicknameModalProps {
  visible: boolean;
  initialValue: string;
  onSave: (nickname: string) => void;
  onClose: () => void;
}

export function NicknameModal({ visible, initialValue, onSave, onClose }: NicknameModalProps) {
  const insets = useSafeAreaInsets();
  const [value, setValue] = useState(initialValue);
  // Reset the field whenever the modal transitions to visible, adjusting state during
  // render (React's recommended pattern) rather than in an effect — avoids the extra
  // render-then-effect-then-render cascade for what's just "sync this prop in".
  const [prevVisible, setPrevVisible] = useState(visible);
  if (visible !== prevVisible) {
    setPrevVisible(visible);
    if (visible) setValue(initialValue);
  }

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose} accessibilityLabel="Close" />
      <View style={[styles.sheet, { paddingBottom: insets.bottom + 16 }]}>
        <Text style={styles.title}>Edit Nick Name</Text>
        <TextInput
          style={styles.input}
          value={value}
          onChangeText={setValue}
          placeholder="Enter nickname"
          placeholderTextColor={Colors.placeholderText}
          autoFocus
        />
        <Pressable
          style={[styles.saveButton, !value.trim() && styles.saveButtonDisabled]}
          disabled={!value.trim()}
          onPress={() => {
            onSave(value.trim());
            onClose();
          }}
          accessibilityRole="button"
          accessibilityLabel="Save nickname">
          <Text style={styles.saveLabel}>Save</Text>
        </Pressable>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
  },
  sheet: {
    backgroundColor: Colors.white,
    borderTopLeftRadius: Radii.cardTop,
    borderTopRightRadius: Radii.cardTop,
    paddingTop: 20,
    paddingHorizontal: 16,
    gap: 16,
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.textPrimary,
    textAlign: 'center',
  },
  input: {
    height: 48,
    borderWidth: 1,
    borderColor: Colors.inputBorder,
    borderRadius: Radii.row,
    paddingHorizontal: 16,
    fontFamily: Fonts.family,
    fontSize: 16,
    color: Colors.textPrimary,
  },
  saveButton: {
    height: 48,
    borderRadius: Radii.button,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  saveButtonDisabled: {
    opacity: 0.5,
  },
  saveLabel: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    color: Colors.white,
  },
});
