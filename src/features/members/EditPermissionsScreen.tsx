import {
  IconActivity,
  IconCheck,
  IconRun,
  IconTimeline,
} from '@tabler/icons-react-native';
import { useRouter } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/AuthContext';
import { useMembers } from '@/features/members/useMembers';
import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import {
  ApiError,
  getMember,
  getSharingGrants,
  updateSharingGrant,
} from '@/lib/api';
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
const SCOPE_METRICS: Record<SharingScope, string[]> = {
  vitals: [
    'Heart Rate',
    'HRV',
    'Stress',
    'SpO2',
    'Respiration',
    'Sleep',
    'ECG',
    'VO2 max',
  ],
  activity: ['Steps', 'Calories', 'Workouts', 'Training Load'],
  behaviour: [],
};

export function EditPermissionsScreen({
  memberId,
}: EditPermissionsScreenProps) {
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

        const nextToggles = {
          vitals: false,
          activity: false,
          behaviour: false,
        };
        for (const scope of SCOPES) {
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

  const updateAllScopes = (value: boolean) => {
    SCOPES.forEach((scope) => void applyScope(scope, value));
  };

  return (
    <View style={styles.screen}>
      <ProfileHeader
        title="Edit Permissions"
        onPressBack={() => router.back()}
      />

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + 24 },
        ]}
        showsVerticalScrollIndicator={false}
      >
        {isLoading ? (
          <ActivityIndicator style={styles.loading} />
        ) : (
          <>
            <Pressable
              style={styles.applyAllRow}
              onPress={() => {
                setApplyToAllMembers((prev) => !prev);
                updateAllScopes(!applyToAllMembers);
              }}
              accessibilityRole="checkbox"
              accessibilityState={{ checked: applyToAllMembers }}
              accessibilityLabel={`Apply these permissions to all members${granteeName ? ` for ${granteeName}` : ''}`}
            >
              <View
                style={[
                  styles.applyCheckbox,
                  applyToAllMembers && styles.applyCheckboxChecked,
                ]}
              >
                {applyToAllMembers ? (
                  <IconCheck size={18} color={Colors.white} strokeWidth={2.4} />
                ) : null}
              </View>
              <Text style={styles.applyAllText}>
                Apply these permissions to all members
              </Text>
            </Pressable>

            <View style={styles.scopeStack}>
              {SCOPES.map((scope) => {
                const Icon = SCOPE_ICONS[scope];
                const isSaving = savingScopes.has(scope);
                const isEnabled = toggles[scope];
                const metricLabels = SCOPE_METRICS[scope];
                return (
                  <View
                    key={scope}
                    style={[
                      styles.scopeCard,
                      metricLabels.length === 0 && styles.scopeCardCollapsed,
                    ]}
                  >
                    <View style={styles.scopeHeader}>
                      <View style={styles.scopeTitleRow}>
                        <Icon
                          size={24}
                          color={Colors.accent}
                          strokeWidth={1.75}
                        />
                        <Text style={styles.scopeTitle}>
                          {SCOPE_LABELS[scope]}
                        </Text>
                      </View>
                      {isSaving ? (
                        <ActivityIndicator size="small" />
                      ) : (
                        <TogglePill
                          value={isEnabled}
                          onPress={() => void applyScope(scope, !isEnabled)}
                        />
                      )}
                    </View>

                    {metricLabels.length > 0 ? (
                      <View style={styles.metricGrid}>
                        {metricLabels.map((label) => (
                          <MetricPermissionTile
                            key={label}
                            label={label}
                            checked={isEnabled}
                            onPress={() => void applyScope(scope, !isEnabled)}
                          />
                        ))}
                      </View>
                    ) : null}
                  </View>
                );
              })}
            </View>

            {errorMessage && (
              <Text style={styles.errorText}>{errorMessage}</Text>
            )}
          </>
        )}
      </ScrollView>
    </View>
  );
}

function TogglePill({
  value,
  onPress,
}: {
  value: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      style={[
        styles.toggleTrack,
        value ? styles.toggleTrackOn : styles.toggleTrackOff,
      ]}
      onPress={onPress}
      accessibilityRole="switch"
      accessibilityState={{ checked: value }}
    >
      <View
        style={[
          styles.toggleThumb,
          value ? styles.toggleThumbOn : styles.toggleThumbOff,
        ]}
      />
    </Pressable>
  );
}

function MetricPermissionTile({
  label,
  checked,
  onPress,
}: {
  label: string;
  checked: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      style={styles.metricTile}
      onPress={onPress}
      accessibilityRole="checkbox"
      accessibilityState={{ checked }}
      accessibilityLabel={label}
    >
      <Text style={styles.metricLabel}>{label}</Text>
      <View
        style={[styles.metricCheckbox, checked && styles.metricCheckboxChecked]}
      >
        {checked ? (
          <IconCheck size={18} color={Colors.white} strokeWidth={2.5} />
        ) : null}
      </View>
    </Pressable>
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
  },
  loading: {
    marginTop: 48,
  },
  applyAllRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    height: 24,
  },
  applyCheckbox: {
    width: 24,
    height: 24,
    borderRadius: 2,
    borderWidth: 2,
    borderColor: '#8f8f8f',
    alignItems: 'center',
    justifyContent: 'center',
  },
  applyCheckboxChecked: {
    borderColor: '#3b72db',
    backgroundColor: '#3b72db',
  },
  applyAllText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 18,
    lineHeight: 22,
    color: Colors.textSecondary,
  },
  scopeStack: {
    marginTop: 20,
    gap: 16,
  },
  scopeCard: {
    backgroundColor: Colors.white,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    borderRadius: Radii.card,
    padding: 16,
  },
  scopeCardCollapsed: {
    height: 56,
    paddingVertical: 0,
    justifyContent: 'center',
  },
  scopeHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  scopeTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  scopeTitle: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 20,
    lineHeight: 28,
    color: Colors.textSecondary,
  },
  toggleTrack: {
    width: 44,
    height: 24,
    borderRadius: 12,
    justifyContent: 'center',
  },
  toggleTrackOn: {
    backgroundColor: '#6bc49b',
  },
  toggleTrackOff: {
    backgroundColor: '#dde3ec',
  },
  toggleThumb: {
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: Colors.white,
  },
  toggleThumbOn: {
    marginLeft: 22,
  },
  toggleThumbOff: {
    marginLeft: 2,
  },
  metricGrid: {
    marginTop: 24,
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 16,
  },
  metricTile: {
    width: 154,
    height: 48,
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 12,
  },
  metricLabel: {
    flex: 1,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.textSecondary,
  },
  metricCheckbox: {
    width: 18,
    height: 18,
    borderRadius: 3,
    borderWidth: 1.5,
    borderColor: Colors.cardBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  metricCheckboxChecked: {
    borderColor: '#3b72db',
    backgroundColor: '#3b72db',
  },
  errorText: {
    marginTop: 16,
    fontFamily: Fonts.family,
    fontSize: 13,
    lineHeight: 18,
    color: Colors.error,
    textAlign: 'center',
  },
});
