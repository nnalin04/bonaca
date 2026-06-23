import { useState } from 'react';

import { useAuth } from '@/features/auth/AuthContext';
import {
  emptyProfileForm,
  formatDob,
  parseHeightCm,
  parseWeightKg,
  toDobIso,
  type ProfileFormState,
  type ProfileModalKey,
} from '@/features/onboarding/model/profileForm';
import { ApiError, completeProfile } from '@/lib/api';

export function useCompleteProfileForm() {
  const { accessToken } = useAuth();
  const [form, setForm] = useState<ProfileFormState>(emptyProfileForm);
  const [dobIso, setDobIso] = useState<string | undefined>(undefined);
  const [activeModal, setActiveModal] = useState<ProfileModalKey | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const isComplete = Boolean(form.name && form.gender && form.dob);

  const updateField = <Key extends keyof ProfileFormState>(
    key: Key,
    value: ProfileFormState[Key],
  ) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const setDateOfBirth = (day: number, month: number, year: number) => {
    setForm((prev) => ({ ...prev, dob: formatDob(day, month, year) }));
    setDobIso(toDobIso(day, month, year));
  };

  const submitProfile = async (): Promise<boolean> => {
    if (!accessToken) return false;
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
      return true;
    } catch (error) {
      setErrorMessage(
        error instanceof ApiError
          ? error.message
          : 'Could not save your profile. Please try again.',
      );
      return false;
    } finally {
      setIsSubmitting(false);
    }
  };

  return {
    form,
    activeModal,
    errorMessage,
    isSubmitting,
    isComplete,
    setActiveModal,
    updateField,
    setDateOfBirth,
    submitProfile,
  };
}
