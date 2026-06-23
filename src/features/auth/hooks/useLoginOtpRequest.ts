import { useState } from 'react';

import { ApiError, requestOtp } from '@/lib/api';

export function useLoginOtpRequest() {
  const [mobileNumber, setMobileNumber] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const canSubmit = mobileNumber.length === 10 && !isSubmitting;

  const requestLoginOtp = async (): Promise<string | null> => {
    const phoneNumberE164 = `+91${mobileNumber}`;
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await requestOtp(phoneNumberE164);
      return phoneNumberE164;
    } catch (error) {
      setErrorMessage(
        error instanceof ApiError
          ? error.message
          : 'Could not send OTP. Please try again.',
      );
      return null;
    } finally {
      setIsSubmitting(false);
    }
  };

  return {
    mobileNumber,
    setMobileNumber,
    isSubmitting,
    errorMessage,
    canSubmit,
    requestLoginOtp,
  };
}
