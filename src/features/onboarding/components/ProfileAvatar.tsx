import { IconPlus } from '@tabler/icons-react-native';
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
          <View style={styles.placeholder}>
            <View style={styles.placeholderHead} />
            <View style={styles.placeholderBody} />
          </View>
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
    borderWidth: 1,
    borderColor: Colors.avatarBorder,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  photo: {
    width: 104,
    height: 104,
  },
  placeholder: {
    width: 104,
    height: 104,
    borderRadius: 52,
    backgroundColor: Colors.avatarPlaceholderInnerBg,
    overflow: 'hidden',
  },
  placeholderHead: {
    position: 'absolute',
    top: 25,
    left: 33,
    width: 37,
    height: 37,
    borderRadius: 18.5,
    backgroundColor: Colors.avatarPlaceholderFg,
  },
  placeholderBody: {
    position: 'absolute',
    top: 69,
    left: 12,
    width: 79,
    height: 46,
    borderRadius: 40,
    backgroundColor: Colors.avatarPlaceholderFg,
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
