import { IconActivity, IconRun, IconTimeline } from '@tabler/icons-react-native';
import { useRouter } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/AuthContext';
import { useMembers } from '@/features/members/useMembers';
import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { ApiError, getMember, getSharingGrants, updateSharingGrant } from '@/lib/api';
import { Colors, Fonts, Radii } from '@/theme/tokens';
import type { SharingScope } from '@/types';
import type { SharingGrantResponse } from '@/types/members';

interface EditPermissionsScreenProps {
  memberId: string;
}

const SCOPES: SharingScope[] = ['vitals', 'activity', 'behaviour'];
const SCOPE_LABELS: Record<SharingScope, string> = {
  vitals: 'Vitals',
  activity: 'Activity',
  behaviour: 'Behaviour',
};
const SCOPE_ICONS: Record<SharingScope, typeof IconActivity> = {
  vitals: IconActivity,
  activity: IconRun,
  behaviour: IconTimeline,
};

export function EditPermissionsScreen({ memberId }: EditPermissionsScreenProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { accessToken } = useAuth();
  const { self } = useMembers();
  const [granteeName, setGranteeName] = useState('');
  const [grants, setGrants] = useState<SharingGrantResponse[]>([]);
  const [toggles, setToggles] = useState<Record<SharingScope, boolean>>({
    vitals: false,
    activity: false,
    behaviour: false,
  });
  const [savingScopes, setSavingScopes] = useState<Set<SharingScope>>(new Set());
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (!accessToken || !self) return;
    Promise.all([getMember(accessToken, memberId), getSharingGrants(accessToken, self.accountId)])
      .then(([member, accountGrants]) => {
        if (cancelled) return;
        setGranteeName(member.nickname ?? member.name);

        const granteeGrants = accountGrants.filter((grant) => grant.granteeMemberId === memberId);
        setGrants(granteeGrants);

        const nextToggles = { vitals: false, activity: false, behaviour: false };
        for (const scope of SCOPES) {
          const scopeGrants = granteeGrants.filter((grant) => grant.scope === scope);
          nextToggles[scope] = scopeGrants.length > 0 && scopeGrants.every((grant) => grant.visible);
        }
        setToggles(nextToggles);
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setErrorMessage(error instanceof ApiError ? error.message : 'Could not load permissions.');
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
        await Promise.all(scopeGrants.map((grant) => updateSharingGrant(accessToken, grant.id, value)));
        setGrants((prev) => prev.map((grant) => (grant.scope === scope ? { ...grant, visible: value } : grant)));
      } catch (error) {
        setToggles((prev) => ({ ...prev, [scope]: !value }));
        setErrorMessage(error instanceof ApiError ? error.message : 'Could not save permissions.');
      } finally {
        setSavingScopes((prev) => {
          const next = new Set(prev);
          next.delete(scope);
          return next;
        });
      }
    },
    [accessToken, grants]
  );

  const allEnabled = SCOPES.every((scope) => toggles[scope]);
  const isSavingAll = savingScopes.size > 0;

  return (
    <View style={styles.screen}>
      <ProfileHeader title="Edit Permissions" onPressBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        {isLoading ? (
          <ActivityIndicator style={styles.loading} />
        ) : (
          <>
            <Text style={styles.subtitle}>Manage access for {granteeName}</Text>

            <View style={styles.card}>
              <View style={styles.row}>
                <View style={styles.rowLabel}>
                  <Text style={styles.rowText}>All</Text>
                </View>
                {isSavingAll ? (
                  <ActivityIndicator size="small" />
                ) : (
                  <Switch
                    value={allEnabled}
                    onValueChange={(value) => SCOPES.forEach((scope) => void applyScope(scope, value))}
                    trackColor={{ true: Colors.accent, false: Colors.cardBorder }}
                  />
                )}
              </View>
              {SCOPES.map((scope) => {
                const Icon = SCOPE_ICONS[scope];
                const isSaving = savingScopes.has(scope);
                return (
                  <View key={scope} style={styles.row}>
                    <View style={styles.rowLabel}>
                      <Icon size={24} color={Colors.textPrimary} strokeWidth={1.75} />
                      <Text style={styles.rowText}>{SCOPE_LABELS[scope]}</Text>
                    </View>
                    {isSaving ? (
                      <ActivityIndicator size="small" />
                    ) : (
                      <Switch
                        value={toggles[scope]}
                        onValueChange={(value) => void applyScope(scope, value)}
                        trackColor={{ true: Colors.accent, false: Colors.cardBorder }}
                      />
                    )}
                  </View>
                );
              })}
            </View>

            {errorMessage && <Text style={styles.errorText}>{errorMessage}</Text>}
          </>
        )}
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
    gap: 16,
  },
  loading: {
    marginTop: 48,
  },
  subtitle: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.textPrimary,
  },
  card: {
    backgroundColor: Colors.white,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    borderRadius: Radii.card,
    paddingHorizontal: 16,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    height: 56,
    borderBottomWidth: 1,
    borderBottomColor: Colors.inputBorderSubtle,
  },
  rowLabel: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  rowText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.textPrimary,
  },
  errorText: {
    fontFamily: Fonts.family,
    fontSize: 13,
    lineHeight: 18,
    color: Colors.error,
    textAlign: 'center',
  },
});
