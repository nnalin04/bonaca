import {
  IconActivity,
  IconCheck,
  IconRun,
  IconTimeline,
  type Icon,
} from '@tabler/icons-react-native';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';
import type { SharingScope } from '@/types';

const scopeIcons: Record<SharingScope, Icon> = {
  vitals: IconActivity,
  activity: IconRun,
  behaviour: IconTimeline,
};

interface PermissionScopeCardProps {
  scope: SharingScope;
  label: string;
  metricLabels: string[];
  enabled: boolean;
  saving: boolean;
  onToggle: () => void;
}

export function PermissionScopeCard({
  scope,
  label,
  metricLabels,
  enabled,
  saving,
  onToggle,
}: PermissionScopeCardProps) {
  const ScopeIcon = scopeIcons[scope];

  return (
    <View
      style={[
        styles.scopeCard,
        metricLabels.length === 0 && styles.scopeCardCollapsed,
      ]}
    >
      <View style={styles.scopeHeader}>
        <View style={styles.scopeTitleRow}>
          <ScopeIcon size={24} color={Colors.accent} strokeWidth={1.75} />
          <Text style={styles.scopeTitle}>{label}</Text>
        </View>
        {saving ? (
          <ActivityIndicator size="small" />
        ) : (
          <TogglePill value={enabled} onPress={onToggle} />
        )}
      </View>

      {metricLabels.length > 0 ? (
        <View style={styles.metricGrid}>
          {metricLabels.map((metricLabel) => (
            <MetricPermissionTile
              key={metricLabel}
              label={metricLabel}
              checked={enabled}
              onPress={onToggle}
            />
          ))}
        </View>
      ) : null}
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
});
