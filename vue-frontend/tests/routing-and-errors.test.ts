import { describe, expect, it } from 'vitest'
import { ApiError, errorText } from '../src/api/client'
import { decideAccess, safeRedirect } from '../src/router/access'

describe('route access decisions', () => {
  it('sends an anonymous visitor to login and retains the destination', () => {
    expect(decideAccess(false, false, '/articles/7/edit', undefined)).toEqual({
      kind: 'login',
      redirect: '/articles/7/edit',
    })
  })

  it('returns a signed-in administrator from login to a safe local path', () => {
    expect(decideAccess(true, true, '/login', '/comments')).toEqual({ kind: 'redirect', path: '/comments' })
    expect(safeRedirect('https://example.com')).toBe('/dashboard')
    expect(safeRedirect('//example.com')).toBe('/dashboard')
  })
})

describe('API error presentation', () => {
  it('keeps rate limit timing and trace id visible', () => {
    const error = new ApiError('too many attempts', 429, 42900, 'trace-123', {}, 45)
    expect(errorText(error)).toBe('尝试过于频繁，请在 45 秒后重试（TraceId: trace-123）')
  })

  it('maps last-admin protection', () => {
    expect(errorText(new ApiError('forbidden', 403, 40302))).toBe('不能修改最后一位管理员')
  })
})
