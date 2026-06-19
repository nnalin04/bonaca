import { IconCheck } from '@tabler/icons-react-native';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface SelectModalProps {
  visible: boolean;
  title: string;
  options: string[];
  selectedValue?: string;
  onSelect: (value: string) => void;
  onClose: () => void;
}

export function SelectModal({
  visible,
  title,
  options,
  selectedValue,
  onSelect,
  onClose,
}: SelectModalProps) {
  const insets = useSafeAreaInsets();

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose} accessibilityLabel="Close" />
      <View style={[styles.sheet, { paddingBottom: insets.bottom + 16 }]}>
        <Text style={styles.title}>{title}</Text>
        <ScrollView style={styles.list} showsVerticalScrollIndicator={false}>
          {options.map((option) => {
            const isSelected = option === selectedValue;
            return (
              <Pressable
                key={option}
                style={styles.row}
                onPress={() => {
                  onSelect(option);
                  onClose();
                }}
                accessibilityRole="button"
                accessibilityLabel={option}>
                <Text style={[styles.rowLabel, isSelected && styles.rowLabelSelected]}>
                  {option}
                </Text>
                {isSelected && <IconCheck size={20} color={Colors.accent} strokeWidth={2} />}
              </Pressable>
            );
          })}
        </ScrollView>
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
    maxHeight: '60%',
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.textPrimary,
    textAlign: 'center',
    marginBottom: 12,
  },
  list: {
    flexGrow: 0,
  },
  row: {
    height: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    borderBottomColor: Colors.inputBorderSubtle,
  },
  rowLabel: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.textPrimary,
  },
  rowLabelSelected: {
    fontWeight: '600',
    color: Colors.accent,
  },
});
