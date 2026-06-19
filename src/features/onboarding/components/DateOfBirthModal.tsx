import { useState } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface DateOfBirthModalProps {
  visible: boolean;
  onConfirm: (date: { day: number; month: number; year: number }) => void;
  onClose: () => void;
}

const MONTHS = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
];
const DAYS = Array.from({ length: 31 }, (_, i) => i + 1);
const CURRENT_YEAR = new Date().getFullYear();
const YEARS = Array.from({ length: 100 }, (_, i) => CURRENT_YEAR - i);

function Column({
  values,
  labels,
  selectedIndex,
  onSelect,
}: {
  values: number[];
  labels?: string[];
  selectedIndex: number;
  onSelect: (index: number) => void;
}) {
  return (
    <ScrollView style={styles.column} showsVerticalScrollIndicator={false}>
      {values.map((value, index) => {
        const isSelected = index === selectedIndex;
        return (
          <Pressable
            key={value}
            style={styles.columnRow}
            onPress={() => onSelect(index)}
            accessibilityRole="button"
            accessibilityLabel={String(labels ? labels[index] : value)}>
            <Text style={[styles.columnLabel, isSelected && styles.columnLabelSelected]}>
              {labels ? labels[index] : value}
            </Text>
          </Pressable>
        );
      })}
    </ScrollView>
  );
}

export function DateOfBirthModal({ visible, onConfirm, onClose }: DateOfBirthModalProps) {
  const insets = useSafeAreaInsets();
  const [dayIndex, setDayIndex] = useState(0);
  const [monthIndex, setMonthIndex] = useState(0);
  const [yearIndex, setYearIndex] = useState(25);

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose} accessibilityLabel="Close" />
      <View style={[styles.sheet, { paddingBottom: insets.bottom + 16 }]}>
        <Text style={styles.title}>Date of Birth</Text>
        <View style={styles.columns}>
          <Column values={DAYS} selectedIndex={dayIndex} onSelect={setDayIndex} />
          <Column
            values={MONTHS.map((_, i) => i + 1)}
            labels={MONTHS}
            selectedIndex={monthIndex}
            onSelect={setMonthIndex}
          />
          <Column values={YEARS} selectedIndex={yearIndex} onSelect={setYearIndex} />
        </View>
        <Pressable
          style={styles.confirmButton}
          onPress={() => {
            onConfirm({
              day: DAYS[dayIndex],
              month: monthIndex + 1,
              year: YEARS[yearIndex],
            });
            onClose();
          }}
          accessibilityRole="button"
          accessibilityLabel="Confirm date of birth">
          <Text style={styles.confirmLabel}>Done</Text>
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
  columns: {
    flexDirection: 'row',
    height: 220,
    gap: 8,
  },
  column: {
    flex: 1,
  },
  columnRow: {
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  columnLabel: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    color: Colors.textSecondary,
  },
  columnLabelSelected: {
    fontWeight: '700',
    fontSize: 18,
    color: Colors.accent,
  },
  confirmButton: {
    height: 48,
    borderRadius: Radii.button,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 16,
  },
  confirmLabel: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    color: Colors.white,
  },
});
