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
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Colors, Fonts, Radii } from '@/theme/tokens';

interface PhysicalStatCardProps {
  icon: typeof IconScale;
  iconColor: string;
  label: string;
  value: string;
  unit?: string;
}

const physicalStats: PhysicalStatCardProps[] = [
  { icon: IconScale, iconColor: '#e07a5f', label: 'Weight', value: '75', unit: 'Kg' },
  { icon: IconRuler2, iconColor: '#8b6f9c', label: 'Height', value: '5’7”', unit: '(170 cm)' },
  { icon: IconStretching, iconColor: '#5b8def', label: 'BMI', value: '23.6', unit: '(Normal)' },
];

export function ProfileDetailsScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.screen}>
      <LinearGradient
        colors={[Colors.headerGradientStart, Colors.headerGradientEnd]}
        locations={[0, 0.9504]}
        start={{ x: 0.9705, y: -0.432 }}
        end={{ x: 0.2064, y: 1.2136 }}
        style={styles.header}>
        <Pressable
          style={styles.backButton}
          onPress={() => router.back()}
          hitSlop={8}
          accessibilityRole="button"
          accessibilityLabel="Go back">
          <IconChevronLeft size={24} color={Colors.white} strokeWidth={1.75} />
        </Pressable>

        <View style={styles.identityRow}>
          <Image
            source={require('../../../assets/images/avatars/prasanna-kumar.png')}
            style={styles.avatar}
            contentFit="cover"
          />
          <View style={styles.identityText}>
            <Text style={styles.name}>Prasanna Kumar (Dad)</Text>
            <Text style={styles.meta}>56 yrs</Text>
          </View>
          <IconPencil size={24} color={Colors.white} strokeWidth={1.75} />
        </View>
      </LinearGradient>

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        <Text style={styles.sectionTitle}>Physical Stats</Text>

        <View style={styles.statsGrid}>
          {physicalStats.map((stat) => (
            <PhysicalStatCard key={stat.label} {...stat} />
          ))}
        </View>

        <Text style={[styles.sectionTitle, styles.wearableTitle]}>Connected Wearable</Text>
        <View style={styles.wearableCard}>
          <Image
            source={require('../../../assets/images/wearables/fitbit.png')}
            style={styles.wearableIcon}
            contentFit="cover"
          />
          <View style={styles.wearableText}>
            <Text style={styles.wearableName}>Fitbit - Charge 5</Text>
            <Text style={styles.wearableSync}>Last synced: 10 mins ago</Text>
          </View>
          <View style={styles.connectedChip}>
            <Text style={styles.connectedText}>Connected</Text>
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
  wearableIcon: {
    width: 36,
    height: 36,
    borderRadius: 18,
  },
  wearableText: {
    flex: 1,
    marginLeft: 10,
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
  connectedChip: {
    height: 24,
    borderRadius: 3,
    backgroundColor: Colors.connectedChipBackground,
    paddingHorizontal: 6,
    justifyContent: 'center',
  },
  connectedText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.connectedChipText,
  },
});
