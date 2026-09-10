export function formatDate(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

export function displayName(user?: { nickname?: string | null; username: string } | null): string {
  return user?.nickname?.trim() || user?.username || '未知用户'
}

export function truncate(value: string, max = 72): string {
  return value.length > max ? `${value.slice(0, max)}…` : value
}
