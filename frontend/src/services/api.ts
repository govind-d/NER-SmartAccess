import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse, AuthResponse } from '../types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

const ACCESS_TOKEN_KEY = 'ner.accessToken'
const REFRESH_TOKEN_KEY = 'ner.refreshToken'

export const tokenStore = {
  access: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  refresh: () => localStorage.getItem(REFRESH_TOKEN_KEY),
  save(access: string, refresh: string) {
    localStorage.setItem(ACCESS_TOKEN_KEY, access)
    localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
  },
  clear() {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  },
}

export const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Attach the access token to every outgoing request.
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.access()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * Silent token refresh.
 *
 * The access token lives only fifteen minutes. Rather than logging the user out every
 * quarter of an hour, the first 401 triggers a refresh and the original request is
 * retried once. Concurrent requests that all fail at the same moment wait on the single
 * in-flight refresh instead of firing five of them - which would fail anyway, because the
 * backend rotates refresh tokens and only the first would be valid.
 */
let refreshInFlight: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStore.refresh()
  if (!refreshToken) {
    throw new Error('No refresh token')
  }
  const response = await axios.post<ApiResponse<AuthResponse>>(
    `${BASE_URL}/auth/refresh`,
    { refreshToken },
    { headers: { 'Content-Type': 'application/json' } },
  )
  const { accessToken, refreshToken: rotated } = response.data.data
  tokenStore.save(accessToken, rotated)
  return accessToken
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retried?: boolean }

    const isAuthCall = original?.url?.includes('/auth/')
    if (error.response?.status !== 401 || original?._retried || isAuthCall) {
      return Promise.reject(error)
    }

    original._retried = true
    try {
      refreshInFlight = refreshInFlight ?? refreshAccessToken()
      const token = await refreshInFlight
      refreshInFlight = null
      original.headers.Authorization = `Bearer ${token}`
      return api(original)
    } catch (refreshError) {
      refreshInFlight = null
      tokenStore.clear()
      // A dead session must land on the login page, not on a broken screen.
      window.location.href = '/login'
      return Promise.reject(refreshError)
    }
  },
)

/** Unwraps the ApiResponse envelope so callers work with plain data. */
export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise
  return response.data.data
}

/** Turns any Axios failure into the message the backend actually sent. */
export function errorMessage(error: unknown): string {
  const axiosError = error as AxiosError<{ message?: string; errors?: { message: string }[] }>
  const body = axiosError?.response?.data
  if (body?.errors?.length) {
    return body.errors.map((e) => e.message).join(', ')
  }
  return body?.message ?? 'Something went wrong. Please try again.'
}
