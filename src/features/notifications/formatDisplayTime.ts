export function formatDisplayTime(createdAtIso: string, now: Date = new Date()): string {
  const createdAt = new Date(createdAtIso);
  const diffMs = now.getTime() - createdAt.getTime();
  const diffMinutes = Math.floor(diffMs / 60000);

  if (diffMinutes < 1) return 'Just now';
  if (diffMinutes < 60) return `${diffMinutes} min${diffMinutes === 1 ? '' : 's'} ago`;

  const diffHours = Math.floor(diffMinutes / 60);
  const isSameCalendarDay = createdAt.toDateString() === now.toDateString();
  if (diffHours < 24 && isSameCalendarDay) {
    return `${diffHours} hr${diffHours === 1 ? '' : 's'} ago`;
  }

  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  if (createdAt.toDateString() === yesterday.toDateString()) {
    return `Yesterday, ${createdAt.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}`;
  }

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays} day${diffDays === 1 ? '' : 's'} ago`;

  const diffWeeks = Math.floor(diffDays / 7);
  return `${diffWeeks} week${diffWeeks === 1 ? '' : 's'} ago`;
}
