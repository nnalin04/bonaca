import {
  DMSans_400Regular,
  DMSans_500Medium,
  DMSans_600SemiBold,
  DMSans_700Bold,
  useFonts,
} from '@expo-google-fonts/dm-sans';
import { Stack } from 'expo-router';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { AuthProvider } from '@/features/auth';

export default function RootLayout() {
  const [fontsLoaded] = useFonts({
    'DM Sans': DMSans_400Regular,
    'DM Sans_500Medium': DMSans_500Medium,
    'DM Sans_600SemiBold': DMSans_600SemiBold,
    'DM Sans_700Bold': DMSans_700Bold,
  });

  if (!fontsLoaded) return null;

  return (
    <SafeAreaProvider>
      <AuthProvider>
        <Stack screenOptions={{ headerShown: false }} />
      </AuthProvider>
    </SafeAreaProvider>
  );
}
