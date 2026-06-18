import { useRouter } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PrimaryButton } from '@/features/auth/components/PrimaryButton';
import { OnboardingHeader } from '@/features/onboarding/components/OnboardingHeader';
import { ProfileAvatar } from '@/features/onboarding/components/ProfileAvatar';
import { ProfileField } from '@/features/onboarding/components/ProfileField';
import { Colors } from '@/theme/tokens';

interface ProfileFormState {
  name: string;
  gender: string;
  dob: string;
  height: string;
  weight: string;
}

const EMPTY_FORM: ProfileFormState = {
  name: '',
  gender: '',
  dob: '',
  height: '',
  weight: '',
};

export function CompleteProfileScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [form, setForm] = useState<ProfileFormState>(EMPTY_FORM);

  const isComplete = Boolean(form.name && form.gender && form.dob);

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
            onPress={() => {}}
          />
          <ProfileField
            label="DOB"
            required
            placeholder="DD/MM/YYYY"
            value={form.dob}
            editable={false}
            onPress={() => {}}
          />
          <ProfileField
            label="Height"
            placeholder="Select"
            value={form.height}
            editable={false}
            onPress={() => {}}
          />
          <ProfileField
            label="Weight"
            placeholder="Select"
            value={form.weight}
            editable={false}
            onPress={() => {}}
          />
        </View>

        <PrimaryButton
          label="Continue"
          disabled={!isComplete}
          onPress={() => router.push('/(auth)/connect-wearable')}
        />
      </ScrollView>
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
});
