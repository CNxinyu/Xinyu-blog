export type AccessDecision =
  | true
  | { kind: 'redirect'; path: string }
  | { kind: 'login'; redirect: string }

export function safeRedirect(value: unknown): string {
  const candidate = typeof value === 'string' ? value : '/dashboard'
  return candidate.startsWith('/') && !candidate.startsWith('//') ? candidate : '/dashboard'
}

export function decideAccess(
  publicRoute: boolean,
  isAdmin: boolean,
  fullPath: string,
  redirectQuery: unknown,
): AccessDecision {
  if (publicRoute) return isAdmin ? { kind: 'redirect', path: safeRedirect(redirectQuery) } : true
  if (!isAdmin) return { kind: 'login', redirect: fullPath }
  return true
}
