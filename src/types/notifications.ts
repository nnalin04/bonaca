// Notifications API response DTOs — shaped to match the backend exactly
// (backend/...notifications/NotificationsController).
// Kept separate from the hand-written domain types in src/types/index.ts, same separation
// the project already uses between database types and domain types (see types/auth.ts).

export interface NotificationResponse {
  id: string;
  /** The notification's subject (who it's about) — not the recipient, see backend plan doc §2. */
  memberId: string;
  type: string;
  title: string;
  body: string;
  deepLinkTarget: string;
  read: boolean;
  createdAt: string;
}
