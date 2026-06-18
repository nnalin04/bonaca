import { IconPlus, IconUserCircle } from '@tabler/icons-react-native';
import { Image, type ImageSource } from 'expo-image';
import { Pressable, StyleSheet, View } from 'react-native';

import { Colors } from '@/theme/tokens';

interface ProfileAvatarProps {
  photoSource?: ImageSource | number;
  onPressEdit?: () => void;
}

export function ProfileAvatar({ photoSource, onPressEdit }: ProfileAvatarProps) {
  return (
    <View style={styles.wrapper}>
      <View style={styles.circle}>
        {photoSource ? (
          <Image source={photoSource} style={styles.photo} contentFit="cover" />
        ) : (
          <IconUserCircle size={88} color={Colors.avatarPlaceholderFg} strokeWidth={1} />
        )}
      </View>
      <Pressable
        style={styles.editButton}
        onPress={onPressEdit}
        hitSlop={8}
        accessibilityRole="button"
        accessibilityLabel="Edit profile photo">
        <IconPlus size={18} color={Colors.white} strokeWidth={2.25} />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    alignSelf: 'center',
    width: 104,
    height: 104,
  },
  circle: {
    width: 104,
    height: 104,
    borderRadius: 52,
    backgroundColor: Colors.avatarPlaceholderBg,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  photo: {
    width: 104,
    height: 104,
  },
  editButton: {
    position: 'absolute',
    right: 0,
    bottom: 0,
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: Colors.white,
  },
});
