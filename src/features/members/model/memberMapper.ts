import type { Member } from '@/types';
import type { MemberResponse } from '@/types/members';

export function toMember(response: MemberResponse): Member {
  return {
    id: response.id,
    accountId: response.accountId,
    role: response.role,
    name: response.name,
    nickname: response.nickname ?? undefined,
    pinned: response.pinned,
    hidden: response.hidden,
    statusMessage: response.statusMessage ?? undefined,
    gender: response.gender ?? undefined,
    dob: response.dob ?? undefined,
    heightCm: response.heightCm ?? undefined,
    weightKg: response.weightKg ?? undefined,
  };
}
