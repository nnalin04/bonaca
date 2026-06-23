import { useCallback, useEffect, useMemo, useState } from 'react';

import { useAuth } from '@/features/auth/AuthContext';
import { useMembers } from '@/features/members/useMembers';
import {
  sharingScopeLabels,
  sharingScopeMetrics,
  sharingScopes,
} from '@/features/members/model/permissions';
import {
  ApiError,
  getMember,
  getSharingGrants,
  updateSharingGrant,
} from '@/lib/api';
import type { SharingScope } from '@/types';
import type { SharingGrantResponse } from '@/types/members';

type ScopeToggleMap = Record<SharingScope, boolean>;

export interface PermissionScopeViewModel {
  scope: SharingScope;
  label: string;
  metricLabels: string[];
  enabled: boolean;
  saving: boolean;
}

interface UseEditPermissionsResult {
  granteeName: string;
  scopes: PermissionScopeViewModel[];
  applyToAllMembers: boolean;
  isLoading: boolean;
  errorMessage: string | null;
  toggleApplyToAllMembers: () => void;
  toggleScope: (scope: SharingScope) => Promise<void>;
}

const disabledToggles: ScopeToggleMap = {
  vitals: false,
  activity: false,
  behaviour: false,
};

export function useEditPermissions(memberId: string): UseEditPermissionsResult {
  const { accessToken } = useAuth();
  const { self } = useMembers();
  const [granteeName, setGranteeName] = useState('');
  const [grants, setGrants] = useState<SharingGrantResponse[]>([]);
  const [toggles, setToggles] = useState<ScopeToggleMap>(disabledToggles);
  const [savingScopes, setSavingScopes] = useState<Set<SharingScope>>(
    new Set(),
  );
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [applyToAllMembers, setApplyToAllMembers] = useState(false);

  useEffect(() => {
    let cancelled = false;
    if (!accessToken || !self) return;

    Promise.all([
      getMember(accessToken, memberId),
      getSharingGrants(accessToken, self.accountId),
    ])
      .then(([member, accountGrants]) => {
        if (cancelled) return;
        setGranteeName(member.nickname ?? member.name);

        const granteeGrants = accountGrants.filter(
          (grant) => grant.granteeMemberId === memberId,
        );
        setGrants(granteeGrants);

        const nextToggles: ScopeToggleMap = { ...disabledToggles };
        for (const scope of sharingScopes) {
          const scopeGrants = granteeGrants.filter(
            (grant) => grant.scope === scope,
          );
          nextToggles[scope] =
            scopeGrants.length > 0 &&
            scopeGrants.every((grant) => grant.visible);
        }
        setToggles(nextToggles);
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setErrorMessage(
            error instanceof ApiError
              ? error.message
              : 'Could not load permissions.',
          );
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, self, memberId]);

  const applyScope = useCallback(
    async (scope: SharingScope, value: boolean) => {
      if (!accessToken) return;
      setErrorMessage(null);
      setToggles((prev) => ({ ...prev, [scope]: value }));
      setSavingScopes((prev) => new Set(prev).add(scope));
      try {
        const scopeGrants = grants.filter((grant) => grant.scope === scope);
        await Promise.all(
          scopeGrants.map((grant) =>
            updateSharingGrant(accessToken, grant.id, value),
          ),
        );
        setGrants((prev) =>
          prev.map((grant) =>
            grant.scope === scope ? { ...grant, visible: value } : grant,
          ),
        );
      } catch (error) {
        setToggles((prev) => ({ ...prev, [scope]: !value }));
        setErrorMessage(
          error instanceof ApiError
            ? error.message
            : 'Could not save permissions.',
        );
      } finally {
        setSavingScopes((prev) => {
          const next = new Set(prev);
          next.delete(scope);
          return next;
        });
      }
    },
    [accessToken, grants],
  );

  const toggleScope = useCallback(
    (scope: SharingScope) => applyScope(scope, !toggles[scope]),
    [applyScope, toggles],
  );

  const toggleApplyToAllMembers = useCallback(() => {
    const nextValue = !applyToAllMembers;
    setApplyToAllMembers(nextValue);
    sharingScopes.forEach((scope) => void applyScope(scope, nextValue));
  }, [applyScope, applyToAllMembers]);

  const scopes = useMemo<PermissionScopeViewModel[]>(
    () =>
      sharingScopes.map((scope) => ({
        scope,
        label: sharingScopeLabels[scope],
        metricLabels: sharingScopeMetrics[scope],
        enabled: toggles[scope],
        saving: savingScopes.has(scope),
      })),
    [savingScopes, toggles],
  );

  return {
    granteeName,
    scopes,
    applyToAllMembers,
    isLoading,
    errorMessage,
    toggleApplyToAllMembers,
    toggleScope,
  };
}
