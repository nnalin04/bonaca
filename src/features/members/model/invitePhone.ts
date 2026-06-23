export function toInvitePhoneNumber(phone: string): string {
  const trimmed = phone.trim();
  if (trimmed.startsWith('+')) {
    return `+${trimmed.replace(/\D/g, '')}`;
  }

  const digits = trimmed.replace(/\D/g, '');
  if (digits.length === 10) {
    return `+91${digits}`;
  }

  return `+${digits}`;
}
