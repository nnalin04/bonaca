export type MemberRole = 'primary' | 'secondary';

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
  /** Short status line shown under the member's name on Member Details, e.g. "A few vitals show improvement today". */
  statusMessage?: string;
  /** Free-text gender as captured on Complete Profile — matches the Figma gender picker, not a fixed enum. */
  gender?: string;
  /** ISO 8601 date (YYYY-MM-DD), no time component. */
  dob?: string;
  heightCm?: number;
  weightKg?: number;
}

export type WearableProvider = 'fitbit' | 'garmin' | 'samsung-health' | 'oura';

export interface WearableConnection {
  id: string;
  memberId: string;
  provider: WearableProvider;
  status: 'PENDING' | 'CONNECTED' | 'DISCONNECTED';
  lastSyncedAt: string | null;
}

export type MetricType =
  | 'heart_rate'
  | 'steps'
  | 'sleep'
  | 'screen_time'
  | 'outdoor_time'
  | 'routine_adherence'
  | 'last_active_location'
  // Additional Vitals-tab metric cards present in the Member Details Figma design (node 196:4233)
  // that aren't yet broken out as PRD-level metrics (docs/PRD.md only formally lists the 7 above).
  | 'heart_rate_variability'
  | 'blood_oxygen'
  | 'respiration_rate'
  | 'stress_level'
  | 'body_temperature'
  | 'ecg'
  | 'blood_glucose'
  | 'vo2_max'
  // Additional Activity-tab metric cards.
  | 'calories'
  | 'workouts'
  | 'training_load';

/** Qualitative comparison of a reading against the member's personal baseline, shown as a caption under the value (e.g. "Higher than usual"). */
export type MetricTrendLabel =
  | 'higher_than_usual'
  | 'lower_than_usual'
  | 'same_as_usual';

export interface MetricReading {
  id: string;
  memberId: string;
  metricType: MetricType;
  value: number;
  unit: string;
  recordedAt: string;
  sourceDeviceId: string;
  /** Comparison-to-baseline caption shown on metric cards, e.g. "Higher than usual". */
  trendLabel?: MetricTrendLabel;
  /** Overrides the trendLabel caption with custom text when the generic vocabulary doesn't fit (e.g. "Within optimal range" for Training Load). */
  customCaption?: string;
  /** Lowest reading in the displayed range, for chart-card min/max captions (e.g. "Lowest: 78 bpm"). */
  rangeMin?: number;
  /** Highest reading in the displayed range, for chart-card min/max captions (e.g. "Highest: 148 bpm"). */
  rangeMax?: number;
}

export interface Insight {
  id: string;
  memberId: string;
  metricType: MetricType;
  generatedText: string;
  date: string;
  kind: 'trend' | 'anomaly';
}

export type SubscriptionStatus =
  | 'trial'
  | 'active'
  | 'expiring'
  | 'expired'
  | 'cancelled';

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
export type PaymentMethodType = 'upi' | 'card';

export interface PaymentMethod {
  id: string;
  type: PaymentMethodType;
  label: string;
  detail?: string;
}

export type SharingScope = 'vitals' | 'activity' | 'behaviour';

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
  /** Short notification headline, e.g. the member's display name shown in the list row. */
  title: string;
  /** Notification body copy. */
  body: string;
  /** Pre-formatted relative/absolute timestamp for display (e.g. "1 hr ago", "Yesterday, 3:30 PM"). */
  displayTime: string;
}

export type InviteStatus = 'pending' | 'accepted' | 'expired';

export interface Invite {
  id: string;
  inviterAccountId: string;
  phoneNumber: string;
  roleOffered: MemberRole;
  status: InviteStatus;
}
