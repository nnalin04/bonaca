import { useRouter } from 'expo-router';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ApplyAllPermissionsRow } from '@/features/members/components/ApplyAllPermissionsRow';
import { PermissionScopeCard } from '@/features/members/components/PermissionScopeCard';
import { useEditPermissions } from '@/features/members/hooks/useEditPermissions';
import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { Colors, Fonts } from '@/theme/tokens';

interface EditPermissionsScreenProps {
  memberId: string;
}

export function EditPermissionsScreen({
  memberId,
}: EditPermissionsScreenProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const {
    granteeName,
    scopes,
    applyToAllMembers,
    isLoading,
    errorMessage,
    toggleApplyToAllMembers,
    toggleScope,
  } = useEditPermissions(memberId);

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
            <ApplyAllPermissionsRow
              checked={applyToAllMembers}
              granteeName={granteeName}
              onToggle={toggleApplyToAllMembers}
            />

            <View style={styles.scopeStack}>
              {scopes.map((scope) => (
                <PermissionScopeCard
                  key={scope.scope}
                  {...scope}
                  onToggle={() => void toggleScope(scope.scope)}
                />
              ))}
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
  scopeStack: {
    marginTop: 20,
    gap: 16,
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
