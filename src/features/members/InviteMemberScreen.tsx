import { IconSearch } from '@tabler/icons-react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { Colors, Fonts } from '@/theme/tokens';

interface ContactOption {
  name: string;
  phone: string;
  initial: string;
  image?: number;
}

const contacts: ContactOption[] = [
  {
    name: 'Abhishek Kumar',
    phone: '97426 58812',
    initial: 'A',
    image: require('../../../assets/images/avatars/prasanna-kumar.png'),
  },
  { name: 'Anurag Patel', phone: '96761 66512', initial: 'A' },
  {
    name: 'Arthi',
    phone: '88812 58812',
    initial: 'A',
    image: require('../../../assets/images/avatars/prasanna-kumar.png'),
  },
  { name: 'Bhadresh Rao', phone: '74201 56320', initial: 'B' },
  { name: 'Bhuvan', phone: '94819 64577', initial: 'B' },
  {
    name: 'Chandrashekar',
    phone: '80561 67412',
    initial: 'C',
    image: require('../../../assets/images/avatars/prasanna-kumar.png'),
  },
  { name: 'Damodar', phone: '97420 11981', initial: 'D' },
  {
    name: 'Dinesh Rickshaw',
    phone: '99803 51212',
    initial: 'D',
    image: require('../../../assets/images/avatars/prasanna-kumar.png'),
  },
  { name: 'Gangadhar', phone: '88812 58812', initial: 'G' },
  { name: 'Arthi', phone: '88812 58812', initial: 'A' },
];

export function InviteMemberScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.screen}>
      <ProfileHeader title="Invite a Family Member" onPressBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        <Text style={styles.subtitle}>
          Invite a family member to approve billing after your free trial
        </Text>

        <View style={styles.searchBox}>
          <IconSearch size={24} color={Colors.textSecondary} strokeWidth={2} />
          <Text style={styles.searchPlaceholder}>Search contact</Text>
        </View>

        <View style={styles.contactList}>
          {contacts.map((contact, index) => (
            <View key={`${contact.name}-${contact.phone}-${index}`} style={styles.contactRow}>
              {contact.image ? (
                <Image source={contact.image} style={styles.avatarImage} contentFit="cover" />
              ) : (
                <View style={styles.avatarFallback}>
                  <Text style={styles.avatarInitial}>{contact.initial}</Text>
                </View>
              )}

              <View style={styles.contactText}>
                <Text style={styles.contactName}>{contact.name}</Text>
                <Text style={styles.contactPhone}>{contact.phone}</Text>
              </View>

              <Pressable
                hitSlop={8}
                accessibilityRole="button"
                accessibilityLabel={`Invite ${contact.name}`}>
                <Text style={styles.inviteLabel}>Invite</Text>
              </Pressable>
            </View>
          ))}
        </View>
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
  subtitle: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textSecondary,
  },
  searchBox: {
    height: 48,
    marginTop: 16,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.onboardingCardBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 12,
  },
  searchPlaceholder: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.searchPlaceholder,
  },
  contactList: {
    marginTop: 12,
    backgroundColor: Colors.white,
    paddingTop: 8,
  },
  contactRow: {
    height: 56,
    marginHorizontal: 8,
    marginBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: Colors.onboardingCardBorder,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
  },
  avatarImage: {
    width: 36,
    height: 36,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: Colors.avatarBorder,
  },
  avatarFallback: {
    width: 36,
    height: 36,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: Colors.avatarBorder,
    backgroundColor: Colors.avatarPlaceholderInnerBg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarInitial: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 18,
    lineHeight: 28,
    color: Colors.avatarPlaceholderFg,
  },
  contactText: {
    flex: 1,
    marginLeft: 8,
  },
  contactName: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textPrimary,
  },
  contactPhone: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  inviteLabel: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.accent,
  },
});
