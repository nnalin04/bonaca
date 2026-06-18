export type MemberRole = 'primary' | 'secondary' | 'tertiary';

export interface Account {
  id: string;
  ownerMemberId: string;
  subscription: Subscription;
}

export interface Member {
  id: string;
  accountId: string;
  role: MemberRole;
  name: string;
  nickname?: string;
  pinned: boolean;
  hidden: boolean;
}

export type WearableProvider = 'apple-health' | 'health-connect' | 'fitbit' | 'garmin';
export type WearableConnectionStatus = 'connected' | 'disconnected' | 'needs-reauth';

export interface WearableConnection {
  id: string;
  memberId: string;
  provider: WearableProvider;
  status: WearableConnectionStatus;
  lastSyncedAt: string | null;
}

export type MetricType =
  | 'heart_rate'
  | 'steps'
  | 'sleep'
  | 'screen_time'
  | 'outdoor_time'
  | 'routine_adherence'
  | 'last_active_location';

export interface MetricReading {
  id: string;
  memberId: string;
  metricType: MetricType;
  value: number;
  unit: string;
  recordedAt: string;
  sourceDeviceId: string;
}

export interface Insight {
  id: string;
  memberId: string;
  metricType: MetricType;
  generatedText: string;
  date: string;
  kind: 'trend' | 'anomaly';
}

export type SubscriptionStatus = 'trial' | 'active' | 'expiring' | 'expired' | 'cancelled';

export interface Subscription {
  id: string;
  accountId: string;
  status: SubscriptionStatus;
  trialEndsAt: string | null;
  renewedAt: string | null;
}

// Payment methods shown on the Payment Gateway screen (Figma section "Connecting a Wearable").
// UI-only for now — no processor is wired up (RevenueCat/StoreKit/Razorpay per
// docs/TECHNICAL_REQUIREMENTS.md are decided but not yet implemented).
export type PaymentMethodType = 'upi' | 'paypal' | 'amex' | 'mastercard' | 'apple-pay';

export interface PaymentMethod {
  id: string;
  type: PaymentMethodType;
  label: string;
  detail?: string;
}

export type SharingScope = 'vitals' | 'activity' | 'behaviour' | 'location';

export interface SharingGrant {
  id: string;
  granterMemberId: string;
  granteeMemberId: string;
  scope: SharingScope;
  visible: boolean;
}

export interface Notification {
  id: string;
  memberId: string;
  type: string;
  read: boolean;
  deepLinkTarget: string;
  createdAt: string;
}

export type InviteStatus = 'pending' | 'accepted' | 'expired';

export interface Invite {
  id: string;
  inviterAccountId: string;
  phoneNumber: string;
  roleOffered: MemberRole;
  status: InviteStatus;
}
