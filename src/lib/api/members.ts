import { apiClient } from '@/lib/api/client';
import type {
  CompleteProfileRequest,
  CreateInviteRequest,
  InviteResponse,
  MemberResponse,
  SharingGrantResponse,
  UpdateMemberRequest,
} from '@/types/members';

export function completeProfile(accessToken: string, request: CompleteProfileRequest): Promise<MemberResponse> {
  return apiClient.post<MemberResponse>('/api/v1/members/complete-profile', request, accessToken) as Promise<MemberResponse>;
}

export function getMembers(accessToken: string): Promise<MemberResponse[]> {
  return apiClient.get<MemberResponse[]>('/api/v1/members', accessToken) as Promise<MemberResponse[]>;
}

export function getMember(accessToken: string, memberId: string): Promise<MemberResponse> {
  return apiClient.get<MemberResponse>(`/api/v1/members/${memberId}`, accessToken) as Promise<MemberResponse>;
}

export function updateMember(
  accessToken: string,
  memberId: string,
  request: UpdateMemberRequest
): Promise<MemberResponse> {
  return apiClient.patch<MemberResponse>(`/api/v1/members/${memberId}`, request, accessToken) as Promise<MemberResponse>;
}

export function createInvite(accessToken: string, request: CreateInviteRequest): Promise<InviteResponse> {
  return apiClient.post<InviteResponse>('/api/v1/invites', request, accessToken) as Promise<InviteResponse>;
}

export function listInvites(accessToken: string): Promise<InviteResponse[]> {
  return apiClient.get<InviteResponse[]>('/api/v1/invites', accessToken) as Promise<InviteResponse[]>;
}

export function getSharingGrants(accessToken: string, accountId: string): Promise<SharingGrantResponse[]> {
  return apiClient.get<SharingGrantResponse[]>(
    `/api/v1/sharing-grants?accountId=${accountId}`,
    accessToken
  ) as Promise<SharingGrantResponse[]>;
}

export function updateSharingGrant(accessToken: string, grantId: string, visible: boolean): Promise<SharingGrantResponse> {
  return apiClient.patch<SharingGrantResponse>(
    `/api/v1/sharing-grants/${grantId}`,
    { visible },
    accessToken
  ) as Promise<SharingGrantResponse>;
}
