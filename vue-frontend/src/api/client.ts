import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import type { ApiResponse, FieldErrors } from '../types/api'

type RetryConfig = InternalAxiosRequestConfig & { _retriedAfterRefresh?: boolean }

const transport = axios.create({
  baseURL: '/',
  withCredentials: true,
  withXSRFToken: false,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: { Accept: 'application/json' },
})

const api = axios.create({
  baseURL: '/',
  withCredentials: true,
  withXSRFToken: false,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: { Accept: 'application/json' },
})

let accessToken: string | null = null
let refreshHandler: (() => Promise<boolean>) | null = null
let csrfPromise: Promise<void> | null = null
let csrfToken: string | null = null

export class ApiError extends Error {
  readonly status: number
  readonly code: number
  readonly traceId: string
  readonly fields: FieldErrors
  readonly retryAfter: number

  constructor(
    message: string,
    status = 0,
    code = 0,
    traceId = '',
    fields: FieldErrors = {},
    retryAfter = 0,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.traceId = traceId
    this.fields = fields
    this.retryAfter = retryAfter
  }
}

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function configureRefreshHandler(handler: () => Promise<boolean>) {
  refreshHandler = handler
}

export function resetCsrfState() {
  csrfPromise = null
  csrfToken = null
}

export function ensureCsrfToken(): Promise<void> {
  if (!csrfPromise) {
    csrfPromise = transport
      .get<ApiResponse<string>>('/api/v1/auth/csrf')
      .then((response) => {
        if (response.data.code !== 0 || !response.data.data) {
          throw fromEnvelope(response.data, response.status)
        }
        csrfToken = response.data.data
      })
      .catch((error) => {
        csrfPromise = null
        throw normalizeApiError(error)
      })
  }
  return csrfPromise
}

api.interceptors.request.use(async (config) => {
  const method = config.method?.toUpperCase() || 'GET'
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    await ensureCsrfToken()
    if (csrfToken) config.headers['X-XSRF-TOKEN'] = csrfToken
  }
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

api.interceptors.response.use(undefined, async (error: AxiosError) => {
  const config = error.config as RetryConfig | undefined
  const url = config?.url || ''
  const canRefresh =
    error.response?.status === 401 &&
    config &&
    !config._retriedAfterRefresh &&
    refreshHandler &&
    !url.includes('/api/v1/auth/login') &&
    !url.includes('/api/v1/auth/refresh')

  if (!canRefresh) return Promise.reject(error)

  config._retriedAfterRefresh = true
  const handler = refreshHandler
  if (!handler) return Promise.reject(error)
  const refreshed = await handler()
  if (!refreshed) return Promise.reject(error)
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return api.request(config)
})

function fromEnvelope(envelope: Partial<ApiResponse<unknown>>, status: number, retryAfter = 0): ApiError {
  const fields =
    envelope.data && typeof envelope.data === 'object' && !Array.isArray(envelope.data)
      ? (envelope.data as FieldErrors)
      : {}
  return new ApiError(
    envelope.message || '请求失败',
    status,
    envelope.code || 0,
    envelope.traceId || '',
    fields,
    retryAfter,
  )
}

export function normalizeApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  if (axios.isAxiosError(error)) {
    const envelope = (error.response?.data || {}) as Partial<ApiResponse<unknown>>
    const retryAfter = Number(error.response?.headers?.['retry-after'] || 0)
    return fromEnvelope(envelope, error.response?.status || 0, retryAfter)
  }
  return new ApiError(error instanceof Error ? error.message : '请求失败')
}

export function errorText(error: unknown, fallback = '操作失败'): string {
  const normalized = normalizeApiError(error)
  const translations: Record<number, string> = {
    40100: '账号或密码错误',
    40101: '登录状态已过期',
    40102: '登录凭证已失效，请重新登录',
    40300: '没有执行此操作的权限',
    40301: '账号已被禁用',
    40302: '不能修改最后一位管理员',
    40900: '名称或 Slug 已存在',
    42900: normalized.retryAfter
      ? `尝试过于频繁，请在 ${normalized.retryAfter} 秒后重试`
      : '尝试过于频繁，请稍后重试',
  }
  const message = translations[normalized.code] || normalized.message || fallback
  return normalized.traceId ? `${message}（TraceId: ${normalized.traceId}）` : message
}

export async function requestData<T>(config: AxiosRequestConfig): Promise<T> {
  try {
    const response = await api.request<ApiResponse<T>>(config)
    if (response.data.code !== 0) throw fromEnvelope(response.data, response.status)
    return response.data.data
  } catch (error) {
    throw normalizeApiError(error)
  }
}

export async function directRequestData<T>(config: AxiosRequestConfig): Promise<T> {
  try {
    const method = config.method?.toUpperCase() || 'GET'
    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) await ensureCsrfToken()
    const response = await transport.request<ApiResponse<T>>({
      ...config,
      headers: {
        ...config.headers,
        ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {}),
      },
    })
    if (response.data.code !== 0) throw fromEnvelope(response.data, response.status)
    return response.data.data
  } catch (error) {
    throw normalizeApiError(error)
  }
}
