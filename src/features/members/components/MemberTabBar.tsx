import {
  IconActivity,
  IconRun,
  IconTimeline,
  type Icon,
} from '@tabler/icons-react-native';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Colors, Fonts, Radii } from '@/theme/tokens';

export type MemberDetailsTab = 'vitals' | 'activity' | 'behaviour';

const tabs: { key: MemberDetailsTab; label: string; icon: Icon }[] = [
  { key: 'vitals', label: 'Vitals', icon: IconActivity },
  { key: 'activity', label: 'Activity', icon: IconRun },
  { key: 'behaviour', label: 'Behaviour', icon: IconTimeline },
];

interface MemberTabBarProps {
  activeTab: MemberDetailsTab;
  onChangeTab: (tab: MemberDetailsTab) => void;
}

export function MemberTabBar({ activeTab, onChangeTab }: MemberTabBarProps) {
  return (
    <View style={styles.floater}>
      {tabs.map(({ key, label, icon: TabIcon }) => {
        const isActive = key === activeTab;
        return (
          <Pressable
            key={key}
            style={[styles.tab, isActive && styles.tabActive]}
            onPress={() => onChangeTab(key)}
            accessibilityRole="button"
            accessibilityLabel={label}
            accessibilityState={{ selected: isActive }}>
            <TabIcon
              size={20}
              color={isActive ? Colors.white : Colors.textSecondary}
              strokeWidth={1.75}
            />
            <Text style={[styles.tabLabel, isActive && styles.tabLabelActive]}>
              {label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  floater: {
    flexDirection: 'row',
    alignSelf: 'center',
    backgroundColor: Colors.tabBarTrack,
    borderWidth: 1,
    borderColor: Colors.white,
    borderRadius: Radii.pill,
    padding: 2,
  },
  tab: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    height: 42,
    paddingHorizontal: 16,
    borderRadius: Radii.pill,
  },
  tabActive: {
    backgroundColor: Colors.accent,
  },
  tabLabel: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
  },
  tabLabelActive: {
    color: Colors.white,
  },
});
