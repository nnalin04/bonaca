import {
  IconChevronLeft,
  IconPencil,
  IconRuler2,
  IconScale,
  IconStretching,
} from '@tabler/icons-react-native';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { useRouter } from 'expo-router';
import type { ComponentType } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useMembers } from '@/features/members';
import {
  getAgeLabel,
  getBmiUnit,
  getBmiValue,
  getHeightPrimaryValue,
  getHeightUnitValue,
  getProfileDisplayName,
  getWeightValue,
} from '@/features/profile/model/profileDetails';
import { useWearableConnection } from '@/features/wearable/hooks/useWearableConnection';
import { Colors, Fonts, Radii } from '@/theme/tokens';

interface PhysicalStatCardProps {
  icon: ComponentType<{ size?: number; color?: string; strokeWidth?: number }>;
  iconColor: string;
  label: string;
  value: string;
  unit?: string;
}

export function ProfileDetailsScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { self } = useMembers();
  const displayName = getProfileDisplayName(self);
  const { connection: wearableConn } = useWearableConnection(self?.id ?? null);
  const physicalStats: PhysicalStatCardProps[] = [
    {
      icon: IconScale,
      iconColor: '#e07a5f',
      label: 'Weight',
      value: getWeightValue(self?.weightKg),
      unit: self?.weightKg ? 'Kg' : undefined,
    },
    {
      icon: IconRuler2,
      iconColor: '#8b6f9c',
      label: 'Height',
      value: getHeightPrimaryValue(self?.heightCm),
      unit: getHeightUnitValue(self?.heightCm),
    },
    {
      icon: IconStretching,
      iconColor: '#5b8def',
      label: 'BMI',
      value: getBmiValue(self),
      unit: getBmiUnit(self),
    },
  ];

  return (
    <View style={styles.screen}>
      <LinearGradient
        colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
        locations={[0, 0.9504]}
        start={{ x: 0.9705, y: -0.432 }}
        end={{ x: 0.2064, y: 1.2136 }}
        style={styles.header}
      >
        <Pressable
          style={styles.backButton}
          onPress={() => router.back()}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="Go back"
        >
          <IconChevronLeft size={24} color={Colors.white} strokeWidth={1.75} />
        </Pressable>

        <View style={styles.identityRow}>
          <Image
            source={require('../../../assets/images/avatars/prasanna-kumar.png')}
            style={styles.avatar}
            contentFit="cover"
          />
          <View style={styles.identityText}>
            <Text style={styles.name}>{displayName}</Text>
            <Text style={styles.meta}>{getAgeLabel(self?.dob)}</Text>
          </View>
          <IconPencil size={24} color={Colors.white} strokeWidth={1.75} />
        </View>
      </LinearGradient>

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + 24 },
        ]}
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.sectionTitle}>Physical Stats</Text>

        <View style={styles.statsGrid}>
          {physicalStats.map((stat) => (
            <PhysicalStatCard key={stat.label} {...stat} />
          ))}
        </View>

        <Text style={[styles.sectionTitle, styles.wearableTitle]}>
          Connected Wearable
        </Text>
        <View style={styles.wearableCard}>
          <View style={styles.wearableText}>
            <Text style={styles.wearableName}>
              {wearableConn?.status === 'CONNECTED'
                ? (wearableConn.provider ?? 'Wearable connected')
                : 'No wearable connected'}
            </Text>
            <Text style={styles.wearableSync}>
              {wearableConn?.status === 'CONNECTED'
                ? (wearableConn.lastSyncedAt ? 'Syncing health data' : 'Connected — waiting for first sync')
                : 'Connect a wearable to start syncing health data'}
            </Text>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

function PhysicalStatCard({
  icon: StatIcon,
  iconColor,
  label,
  value,
  unit,
}: PhysicalStatCardProps) {
  return (
    <View style={styles.statCard}>
      <View style={styles.statHeader}>
        <View style={styles.statIcon}>
          <StatIcon size={24} color={iconColor} strokeWidth={1.75} />
        </View>
        <Text style={styles.statLabel}>{label}</Text>
      </View>

      <View style={styles.statValueLine}>
        <Text style={styles.statValue}>{value}</Text>
        {unit ? <Text style={styles.statUnit}>{unit}</Text> : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    height: 175,
    borderBottomLeftRadius: Radii.headerCorner,
    borderBottomRightRadius: Radii.headerCorner,
    paddingHorizontal: 16,
    paddingTop: 63,
    paddingBottom: 16,
    gap: 16,
  },
  backButton: {
    width: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  identityRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    borderWidth: 1,
    borderColor: Colors.memberHeaderAvatarBorder,
  },
  identityText: {
    flex: 1,
    gap: 2,
  },
  name: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 18,
    lineHeight: 24,
    color: Colors.white,
  },
  meta: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textOnDark,
  },
  content: {
    paddingHorizontal: 16,
    paddingTop: 20,
  },
  sectionTitle: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 20,
    lineHeight: 28,
    color: Colors.textPrimary,
  },
  statsGrid: {
    marginTop: 12,
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 16,
  },
  statCard: {
    width: 171,
    height: 116,
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    padding: 12,
  },
  statHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  statIcon: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.metricIconBackground,
    alignItems: 'center',
    justifyContent: 'center',
  },
  statLabel: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  statValueLine: {
    marginTop: 16,
    flexDirection: 'row',
    alignItems: 'baseline',
    gap: 4,
  },
  statValue: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 28,
    lineHeight: 40,
    color: Colors.textPrimary,
  },
  statUnit: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  wearableTitle: {
    marginTop: 52,
  },
  wearableCard: {
    height: 62,
    marginTop: 12,
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
  },
  wearableText: {
    flex: 1,
  },
  wearableName: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  wearableSync: {
    marginTop: 2,
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
});
