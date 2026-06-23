import { useRouter } from 'expo-router';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PrimaryButton } from '@/features/auth/components/PrimaryButton';
import { DateOfBirthModal } from '@/features/onboarding/components/DateOfBirthModal';
import { OnboardingHeader } from '@/features/onboarding/components/OnboardingHeader';
import { ProfileAvatar } from '@/features/onboarding/components/ProfileAvatar';
import { ProfileField } from '@/features/onboarding/components/ProfileField';
import { SelectModal } from '@/features/onboarding/components/SelectModal';
import { useCompleteProfileForm } from '@/features/onboarding/hooks/useCompleteProfileForm';
import {
  genderOptions,
  heightOptions,
  weightOptions,
} from '@/features/onboarding/model/profileForm';
import { Colors } from '@/theme/tokens';

export function CompleteProfileScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const {
    form,
    activeModal,
    errorMessage,
    isSubmitting,
    isComplete,
    setActiveModal,
    updateField,
    setDateOfBirth,
    submitProfile,
  } = useCompleteProfileForm();

  const handleContinue = async () => {
    const saved = await submitProfile();
    if (saved) {
      router.push('/(auth)/connect-wearable');
    }
  };

  return (
    <View style={styles.screen}>
      <OnboardingHeader
        title="Complete Your Profile"
        onBack={() => router.back()}
      />

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + 24 },
        ]}
        showsVerticalScrollIndicator={false}
      >
        <ProfileAvatar onPressEdit={() => {}} />

        <View style={styles.form}>
          <ProfileField
            label="Name"
            required
            placeholder="Enter name"
            value={form.name}
            onChangeText={(name) => updateField('name', name)}
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
        options={genderOptions}
        selectedValue={form.gender}
        onSelect={(gender) => updateField('gender', gender)}
        onClose={() => setActiveModal(null)}
      />
      <SelectModal
        visible={activeModal === 'height'}
        title="Height"
        options={heightOptions}
        selectedValue={form.height}
        onSelect={(height) => updateField('height', height)}
        onClose={() => setActiveModal(null)}
      />
      <SelectModal
        visible={activeModal === 'weight'}
        title="Weight"
        options={weightOptions}
        selectedValue={form.weight}
        onSelect={(weight) => updateField('weight', weight)}
        onClose={() => setActiveModal(null)}
      />
      <DateOfBirthModal
        visible={activeModal === 'dob'}
        onConfirm={({ day, month, year }) => {
          setDateOfBirth(day, month, year);
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
    paddingTop: 20,
    gap: 32,
  },
  form: {
    gap: 16,
  },
  errorText: {
    color: Colors.error,
    textAlign: 'center',
  },
});
