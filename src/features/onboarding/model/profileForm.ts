export interface ProfileFormState {
  name: string;
  gender: string;
  dob: string;
  height: string;
  weight: string;
}

export type ProfileModalKey = 'gender' | 'dob' | 'height' | 'weight';

export const emptyProfileForm: ProfileFormState = {
  name: '',
  gender: '',
  dob: '',
  height: '',
  weight: '',
};

export const genderOptions = ['Male', 'Female', 'Other', 'Prefer not to say'];
export const heightOptions = Array.from({ length: 5 }, (_, ft) =>
  Array.from({ length: 12 }, (_, inch) => `${ft + 4}ft ${inch}in`),
).flat();
export const weightOptions = Array.from(
  { length: 121 },
  (_, i) => `${i + 30} Kg`,
);

const monthNames = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
];

export function parseHeightCm(label: string): number | undefined {
  const match = /^(\d+)ft (\d+)in$/.exec(label);
  if (!match) return undefined;
  const totalInches = Number(match[1]) * 12 + Number(match[2]);
  return Math.round(totalInches * 2.54);
}

export function parseWeightKg(label: string): number | undefined {
  const match = /^(\d+) Kg$/.exec(label);
  return match ? Number(match[1]) : undefined;
}

export function toDobIso(day: number, month: number, year: number): string {
  return `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
}

export function formatDob(day: number, month: number, year: number): string {
  const today = new Date();
  let age = today.getFullYear() - year;
  const hasHadBirthdayThisYear =
    today.getMonth() + 1 > month ||
    (today.getMonth() + 1 === month && today.getDate() >= day);
  if (!hasHadBirthdayThisYear) age -= 1;

  const dayLabel = day.toString().padStart(2, '0');
  return `${dayLabel} ${monthNames[month - 1]} ${year} (${age} yrs)`;
}
