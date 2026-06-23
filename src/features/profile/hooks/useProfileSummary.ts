import { useEffect, useState } from 'react';

import { useAuth } from '@/features/auth/AuthContext';
import { getMe } from '@/lib/api';

export function useProfileSummary() {
  const { accessToken } = useAuth();
  const [phoneNumber, setPhoneNumber] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (!accessToken) {
      Promise.resolve().then(() => {
        if (!cancelled) setPhoneNumber(null);
      });
      return () => {
        cancelled = true;
      };
    }
    getMe(accessToken).then((result) => {
      if (!cancelled) setPhoneNumber(result.phoneNumber);
    });
    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  return { phoneNumber };
}
