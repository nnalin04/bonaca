import * as SecureStore from 'expo-secure-store';
import { createContext, type PropsWithChildren, useContext, useEffect, useMemo, useState } from 'react';

import { logout as apiLogout, refreshAccessToken, verifyOtp as apiVerifyOtp } from '@/lib/api/auth';

const ACCESS_TOKEN_KEY = 'bonaca.accessToken';
const REFRESH_TOKEN_KEY = 'bonaca.refreshToken';

interface AuthContextValue {
  isLoading: boolean;
  isAuthenticated: boolean;
  /** Only meaningful at cold-start routing time (see SplashScreen) — not kept live after CompleteProfileScreen succeeds mid-session. */
  profileCompleted: boolean;
  accessToken: string | null;
  login: (phoneNumberE164: string, code: string) => Promise<{ profileCompleted: boolean }>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: PropsWithChildren) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [profileCompleted, setProfileCompleted] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function restoreSession() {
      const storedRefreshToken = await SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
      if (!storedRefreshToken) {
        setIsLoading(false);
        return;
      }
      try {
        const tokens = await refreshAccessToken(storedRefreshToken);
        await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, tokens.accessToken);
        await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, tokens.refreshToken);
        setAccessToken(tokens.accessToken);
        setProfileCompleted(tokens.profileCompleted);
      } catch {
        // Stored refresh token is no longer valid — fall through to the logged-out state.
        await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY);
        await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
      } finally {
        setIsLoading(false);
      }
    }
    restoreSession();
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      isLoading,
      isAuthenticated: accessToken !== null,
      profileCompleted,
      accessToken,
      async login(phoneNumberE164, code) {
        const tokens = await apiVerifyOtp(phoneNumberE164, code);
        await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, tokens.accessToken);
        await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, tokens.refreshToken);
        setAccessToken(tokens.accessToken);
        setProfileCompleted(tokens.profileCompleted);
        return { profileCompleted: tokens.profileCompleted };
      },
      async logout() {
        const storedRefreshToken = await SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
        if (storedRefreshToken) {
          await apiLogout(storedRefreshToken).catch(() => undefined);
        }
        await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY);
        await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
        setAccessToken(null);
        setProfileCompleted(false);
      },
    }),
    [accessToken, profileCompleted, isLoading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
