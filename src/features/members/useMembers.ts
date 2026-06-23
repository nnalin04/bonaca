import { useCallback, useEffect, useState } from 'react';

import { useAuth } from '@/features/auth/AuthContext';
import { toMember } from '@/features/members/model/memberMapper';
import { ApiError, getMembers } from '@/lib/api';
import type { Member } from '@/types';
import type { MemberResponse } from '@/types/members';

interface UseMembersResult {
  /** The requesting user's own Member row, or undefined while loading/on error. */
  self: Member | undefined;
  /** All other members visible to the requester (account-mates the Primary manages, or members a SharingGrant exposes). */
  others: Member[];
  isLoading: boolean;
  errorMessage: string | null;
  refresh: () => Promise<void>;
}

export function useMembers(): UseMembersResult {
  const { accessToken } = useAuth();
  const [responses, setResponses] = useState<MemberResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!accessToken) {
      setResponses([]);
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setErrorMessage(null);
    try {
      setResponses(await getMembers(accessToken));
    } catch (error) {
      setErrorMessage(
        error instanceof ApiError
          ? error.message
          : 'Could not load your family members.',
      );
    } finally {
      setIsLoading(false);
    }
  }, [accessToken]);

  useEffect(() => {
    let cancelled = false;
    if (!accessToken) {
      Promise.resolve().then(() => {
        if (!cancelled) {
          setResponses([]);
          setIsLoading(false);
        }
      });
      return () => {
        cancelled = true;
      };
    }
    getMembers(accessToken)
      .then((result) => {
        if (!cancelled) setResponses(result);
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setErrorMessage(
            error instanceof ApiError
              ? error.message
              : 'Could not load your family members.',
          );
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  const self = responses.find((response) => response.self);
  const others = responses.filter((response) => !response.self);

  return {
    self: self ? toMember(self) : undefined,
    others: others.map(toMember),
    isLoading,
    errorMessage,
    refresh,
  };
}
