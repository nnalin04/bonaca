import type { Member } from '@/types';

export function getProfileDisplayName(member: Member | undefined): string {
  return member?.nickname ?? member?.name ?? 'Profile';
}

export function getAgeLabel(dob: string | undefined): string {
  if (!dob) return 'Age not set';

  const birthDate = new Date(dob);
  if (Number.isNaN(birthDate.getTime())) return 'Age not set';

  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const hasBirthdayPassed =
    today.getMonth() > birthDate.getMonth() ||
    (today.getMonth() === birthDate.getMonth() &&
      today.getDate() >= birthDate.getDate());

  if (!hasBirthdayPassed) age -= 1;
  return `${age} yrs`;
}

export function getHeightPrimaryValue(heightCm: number | undefined): string {
  if (!heightCm) return '—';

  const totalInches = Math.round(heightCm / 2.54);
  const feet = Math.floor(totalInches / 12);
  const inches = totalInches % 12;
  return `${feet}’${inches}”`;
}

export function getHeightUnitValue(
  heightCm: number | undefined,
): string | undefined {
  return heightCm ? `(${heightCm} cm)` : undefined;
}

export function getWeightValue(weightKg: number | undefined): string {
  return weightKg ? String(weightKg) : '—';
}

export function getBmiValue(member: Member | undefined): string {
  if (!member?.heightCm || !member.weightKg) return '—';

  const heightMeters = member.heightCm / 100;
  return (member.weightKg / (heightMeters * heightMeters)).toFixed(1);
}

export function getBmiUnit(member: Member | undefined): string | undefined {
  if (!member?.heightCm || !member.weightKg) return undefined;

  const bmi = Number(getBmiValue(member));
  if (bmi < 18.5) return '(Low)';
  if (bmi < 25) return '(Normal)';
  if (bmi < 30) return '(High)';
  return '(Very high)';
}
