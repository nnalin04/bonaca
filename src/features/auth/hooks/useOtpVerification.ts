import { useEffect, useState } from 'react';

import { useAuth } from '@/features/auth/AuthContext';
import { ApiError, requestOtp } from '@/lib/api';

const RESEND_COUNTDOWN_SECONDS = 30;
const OTP_LENGTH = 6;

export function formatCountdown(seconds: number) {
  const mm = Math.floor(seconds / 60)
    .toString()
    .padStart(2, '0');
  const ss = (seconds % 60).toString().padStart(2, '0');
  return `${mm}:${ss}`;
}

export function formatOtpPhoneNumber(phoneNumber?: string) {
  if (!phoneNumber) return '';
  return phoneNumber.startsWith('+91') ? phoneNumber.slice(3) : phoneNumber;
}

export function useOtpVerification({
  phoneNumber,
  onVerified,
}: {
  phoneNumber: string;
  onVerified: (profileCompleted: boolean) => void;
}) {
  const { login } = useAuth();
  const [digits, setDigits] = useState<string[]>(['', '', '', '', '', '']);
  const [secondsLeft, setSecondsLeft] = useState(RESEND_COUNTDOWN_SECONDS);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const timer = setInterval(() => {
      setSecondsLeft((value) => Math.max(0, value - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, [secondsLeft]);

  const verify = async (code: string) => {
    setIsVerifying(true);
    try {
      const { profileCompleted } = await login(phoneNumber, code);
      onVerified(profileCompleted);
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : 'Enter a valid OTP');
      setDigits(['', '', '', '', '', '']);
    } finally {
      setIsVerifying(false);
    }
  };

  const handleChange = (next: string[]) => {
    setErrorMessage(null);
    setDigits(next);

    const code = next.join('');
    if (code.length === OTP_LENGTH) {
      void verify(code);
    }
  };

  const handleResend = async () => {
    setSecondsLeft(RESEND_COUNTDOWN_SECONDS);
    setErrorMessage(null);
    setDigits(['', '', '', '', '', '']);
    try {
      await requestOtp(phoneNumber);
    } catch (error) {
      setErrorMessage(
        error instanceof ApiError
          ? error.message
          : 'Could not resend OTP. Please try again.',
      );
    }
  };

  return {
    digits,
    secondsLeft,
    errorMessage,
    isVerifying,
    displayPhoneNumber: formatOtpPhoneNumber(phoneNumber),
    handleChange,
    handleResend,
  };
}
