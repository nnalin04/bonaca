// Members/Sharing API request/response DTOs — shaped to match the backend exactly
// (backend/...members/MembersController, InvitesController, SharingGrantsController).
// Kept separate from the hand-written domain types in src/types/index.ts, same separation
// the project already uses between database types and domain types (see types/auth.ts).

import type { MemberRole, SharingScope } from '@/types';

export interface MemberResponse {
  id: string;
  accountId: string;
  role: MemberRole;
  name: string;
  nickname: string | null;
  pinned: boolean;
  hidden: boolean;
  statusMessage: string | null;
  gender: string | null;
  dob: string | null;
  heightCm: number | null;
  weightKg: number | null;
  self: boolean;
}

export interface CompleteProfileRequest {
  name: string;
  gender?: string;
  dob?: string;
  heightCm?: number;
  weightKg?: number;
}

export interface UpdateMemberRequest {
  nickname?: string;
  pinned?: boolean;
  hidden?: boolean;
}

// Secondary is the only assignable role — Primary is taken at account creation, no other role exists.
export interface CreateInviteRequest {
  phoneNumber: string;
}

export interface InviteResponse {
  id: string;
  phoneNumber: string;
  roleOffered: MemberRole;
  status: 'pending' | 'accepted' | 'expired';
}

export interface SharingGrantResponse {
  id: string;
  granterMemberId: string;
  granteeMemberId: string;
  scope: SharingScope;
  visible: boolean;
}
