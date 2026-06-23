import type { SubscriptionStatus } from '@/types';
import type { SubscriptionResponse } from '@/types/subscriptions';

export interface SubscriptionDisplayState {
  status: string;
  statusTone?: 'error';
  drawer?: {
    chip: string;
    title: string;
    body: string;
  };
}

const dayMs = 24 * 60 * 60 * 1000;

export function getSubscriptionDisplayState(
  subscription: SubscriptionResponse | null,
): SubscriptionDisplayState {
  if (!subscription) {
    return {
      status: 'Subscription status unavailable',
      statusTone: 'error',
      drawer: {
        chip: 'Unavailable',
        title: 'Subscription',
        body: 'We could not load your subscription. Please try again shortly.',
      },
    };
  }

  if (subscription.status === 'trial' && subscription.trialEndsAt) {
    const days = getDaysUntil(subscription.trialEndsAt);
    return {
      status:
        days <= 0
          ? 'Free trial ends today'
          : `Free trial ends in ${formatDays(days)}`,
      drawer: {
        chip:
          days <= 0 ? 'Trial ends today' : `Trial ends in ${formatDays(days)}`,
        title: 'Free Trial',
        body: 'Activate your plan to keep health insights running after the trial.',
      },
    };
  }

  if (subscription.status === 'active') {
    return {
      status: subscription.renewedAt
        ? `Renewed on ${formatShortDate(subscription.renewedAt)}`
        : 'Subscription active',
    };
  }

  if (subscription.status === 'expiring') {
    return {
      status: 'Subscription expires soon',
      drawer: {
        chip: 'Expiring soon',
        title: 'Subscription',
        body: 'Renew to continue uninterrupted health insights.',
      },
    };
  }

  if (
    subscription.status === 'expired' ||
    subscription.status === 'cancelled'
  ) {
    return {
      status: getInactiveStatusLabel(subscription.status),
      statusTone: 'error',
      drawer: {
        chip: getInactiveChipLabel(subscription.status),
        title: 'Subscription',
        body: 'Subscription has ended, so health tracking is paused.',
      },
    };
  }

  return { status: 'Subscription status unavailable', statusTone: 'error' };
}

function getDaysUntil(dateIso: string): number {
  const targetTime = new Date(dateIso).getTime();
  const now = Date.now();
  return Math.max(0, Math.ceil((targetTime - now) / dayMs));
}

function formatDays(days: number): string {
  return days === 1 ? '1 day' : `${days} days`;
}

function formatShortDate(dateIso: string): string {
  return new Intl.DateTimeFormat('en-IN', {
    day: 'numeric',
    month: 'short',
  }).format(new Date(dateIso));
}

function getInactiveStatusLabel(status: SubscriptionStatus): string {
  return status === 'cancelled'
    ? 'Subscription cancelled'
    : 'Subscription expired';
}

function getInactiveChipLabel(status: SubscriptionStatus): string {
  return status === 'cancelled' ? 'Cancelled' : 'Subscription expired';
}
