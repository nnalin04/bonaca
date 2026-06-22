import { useRouter } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/AuthContext';
import { PrimaryButton } from '@/features/auth/components/PrimaryButton';
import { DateOfBirthModal } from '@/features/onboarding/components/DateOfBirthModal';
import { OnboardingHeader } from '@/features/onboarding/components/OnboardingHeader';
import { ProfileAvatar } from '@/features/onboarding/components/ProfileAvatar';
import { ProfileField } from '@/features/onboarding/components/ProfileField';
import { SelectModal } from '@/features/onboarding/components/SelectModal';
import { ApiError, completeProfile } from '@/lib/api';
import { Colors } from '@/theme/tokens';

interface ProfileFormState {
  name: string;
  gender: string;
  dob: string;
  height: string;
  weight: string;
}

/** Parses a "5ft 11in" label (see HEIGHT_OPTIONS below) into whole centimeters. */
function parseHeightCm(label: string): number | undefined {
  const match = /^(\d+)ft (\d+)in$/.exec(label);
  if (!match) return undefined;
  const totalInches = Number(match[1]) * 12 + Number(match[2]);
  return Math.round(totalInches * 2.54);
}

/** Parses a "70 Kg" label (see WEIGHT_OPTIONS below) into whole kilograms. */
function parseWeightKg(label: string): number | undefined {
  const match = /^(\d+) Kg$/.exec(label);
  return match ? Number(match[1]) : undefined;
}

const EMPTY_FORM: ProfileFormState = {
  name: '',
  gender: '',
  dob: '',
  height: '',
  weight: '',
};

const GENDER_OPTIONS = ['Male', 'Female', 'Other', 'Prefer not to say'];
const HEIGHT_OPTIONS = Array.from(
  { length: 5 },
  (_, ft) =>
    Array.from({ length: 12 }, (_, inch) => `${ft + 4}ft ${inch}in`),
).flat();
const WEIGHT_OPTIONS = Array.from({ length: 121 }, (_, i) => `${i + 30} Kg`);

const MONTH_NAMES = [
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

function formatDob(day: number, month: number, year: number): string {
  const today = new Date();
  let age = today.getFullYear() - year;
  const hasHadBirthdayThisYear =
    today.getMonth() + 1 > month || (today.getMonth() + 1 === month && today.getDate() >= day);
  if (!hasHadBirthdayThisYear) age -= 1;

  const dayLabel = day.toString().padStart(2, '0');
  return `${dayLabel} ${MONTH_NAMES[month - 1]} ${year} (${age} yrs)`;
}

export function CompleteProfileScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { accessToken } = useAuth();
  const [form, setForm] = useState<ProfileFormState>(EMPTY_FORM);
  const [dobIso, setDobIso] = useState<string | undefined>(undefined);
  const [activeModal, setActiveModal] = useState<
    'gender' | 'dob' | 'height' | 'weight' | null
  >(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isComplete = Boolean(form.name && form.gender && form.dob);

  const handleContinue = async () => {
    if (!accessToken) return;
    setErrorMessage(null);
    setIsSubmitting(true);
    try {
      await completeProfile(accessToken, {
        name: form.name,
        gender: form.gender,
        dob: dobIso,
        heightCm: form.height ? parseHeightCm(form.height) : undefined,
        weightKg: form.weight ? parseWeightKg(form.weight) : undefined,
      });
      router.push('/(auth)/connect-wearable');
    } catch (error) {
      setErrorMessage(error instanceof ApiError ? error.message : 'Could not save your profile. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <View style={styles.screen}>
      <OnboardingHeader title="Complete Your Profile" onBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        <ProfileAvatar onPressEdit={() => {}} />

        <View style={styles.form}>
          <ProfileField
            label="Name"
            required
            placeholder="Enter name"
            value={form.name}
            onChangeText={(name) => setForm((prev) => ({ ...prev, name }))}
          />
          <ProfileField
            label="Gender"
            required
            placeholder="Select"
            value={form.gender}
            editable={false}
            onPress={() => setActiveModal('gender')}
          />
          <ProfileField
            label="DOB"
            required
            placeholder="DD/MM/YYYY"
            value={form.dob}
            editable={false}
            onPress={() => setActiveModal('dob')}
          />
          <ProfileField
            label="Height"
            placeholder="Select"
            value={form.height}
            editable={false}
            onPress={() => setActiveModal('height')}
          />
          <ProfileField
            label="Weight"
            placeholder="Select"
            value={form.weight}
            editable={false}
            onPress={() => setActiveModal('weight')}
          />
        </View>

        {isSubmitting && <ActivityIndicator />}
        {errorMessage && <Text style={styles.errorText}>{errorMessage}</Text>}

        <PrimaryButton
          label="Continue"
          disabled={!isComplete || isSubmitting}
          onPress={() => void handleContinue()}
        />
      </ScrollView>

      <SelectModal
        visible={activeModal === 'gender'}
        title="Gender"
        options={GENDER_OPTIONS}
        selectedValue={form.gender}
        onSelect={(gender) => setForm((prev) => ({ ...prev, gender }))}
        onClose={() => setActiveModal(null)}
      />
      <SelectModal
        visible={activeModal === 'height'}
        title="Height"
        options={HEIGHT_OPTIONS}
        selectedValue={form.height}
        onSelect={(height) => setForm((prev) => ({ ...prev, height }))}
        onClose={() => setActiveModal(null)}
      />
      <SelectModal
        visible={activeModal === 'weight'}
        title="Weight"
        options={WEIGHT_OPTIONS}
        selectedValue={form.weight}
        onSelect={(weight) => setForm((prev) => ({ ...prev, weight }))}
        onClose={() => setActiveModal(null)}
      />
      <DateOfBirthModal
        visible={activeModal === 'dob'}
        onConfirm={({ day, month, year }) => {
          setForm((prev) => ({ ...prev, dob: formatDob(day, month, year) }));
          setDobIso(`${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`);
        }}
        onClose={() => setActiveModal(null)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  content: {
    paddingHorizontal: 16,
    paddingTop: 24,
    gap: 32,
  },
  form: {
    gap: 20,
  },
  errorText: {
    color: Colors.error,
    textAlign: 'center',
  },
});
