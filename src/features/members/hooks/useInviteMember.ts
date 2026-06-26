import { useCallback, useEffect, useMemo, useState } from 'react';

import { useAuth } from '@/features/auth/AuthContext';
import { toInvitePhoneNumber } from '@/features/members/model/invitePhone';
import { ApiError, createInvite, listInvites } from '@/lib/api';
import type { InviteResponse } from '@/types/members';

interface UseInviteMemberResult {
  invites: InviteResponse[];
  invitesByPhone: Map<string, InviteResponse>;
  pendingPhone: string | null;
  errorMessage: string | null;
  inviteContact: (phone: string) => Promise<void>;
}

export function useInviteMember(): UseInviteMemberResult {
  const { accessToken } = useAuth();
  const [invites, setInvites] = useState<InviteResponse[]>([]);
  const [pendingPhone, setPendingPhone] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (!accessToken) {
      Promise.resolve().then(() => {
        if (!cancelled) setInvites([]);
      });
      return () => {
        cancelled = true;
      };
    }

    listInvites(accessToken)
      .then((result) => {
        if (!cancelled) setInvites(result);
      })
      .catch(() => undefined);

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  const invitesByPhone = useMemo(
    () =>
      new Map(
        invites.map((invite) => [
          toInvitePhoneNumber(invite.phoneNumber),
          invite,
        ]),
      ),
    [invites],
  );

  const inviteContact = useCallback(
    async (phone: string) => {
      if (!accessToken) {
        setErrorMessage('Please sign in again to send an invite.');
        return;
      }

      const phoneNumber = toInvitePhoneNumber(phone);
      setPendingPhone(phoneNumber);
      setErrorMessage(null);
      try {
        const invite = await createInvite(accessToken, { phoneNumber });
        setInvites((current) => [
          invite,
          ...current.filter(
            (item) => toInvitePhoneNumber(item.phoneNumber) !== phoneNumber,
          ),
        ]);
      } catch (error) {
        setErrorMessage(
          error instanceof ApiError
            ? error.message
            : 'Could not send this invite. Please try again.',
        );
      } finally {
        setPendingPhone(null);
      }
    },
    [accessToken],
  );

  return { invites, invitesByPhone, pendingPhone, errorMessage, inviteContact };
}
