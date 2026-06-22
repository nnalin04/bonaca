import { IconBell, IconHome, IconUserCircle } from '@tabler/icons-react-native';
import { Tabs } from 'expo-router';

import { Colors } from '@/theme/tokens';

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: Colors.accent,
        tabBarInactiveTintColor: Colors.textSecondary,
      }}>
      <Tabs.Screen
        name="home"
        options={{
          title: 'Home',
          tabBarIcon: ({ color, size }) => <IconHome size={size} color={color} strokeWidth={1.75} />,
        }}
      />
      <Tabs.Screen
        name="notifications"
        options={{
          title: 'Notifications',
          tabBarIcon: ({ color, size }) => <IconBell size={size} color={color} strokeWidth={1.75} />,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Profile',
          tabBarIcon: ({ color, size }) => <IconUserCircle size={size} color={color} strokeWidth={1.75} />,
        }}
      />
    </Tabs>
  );
}
