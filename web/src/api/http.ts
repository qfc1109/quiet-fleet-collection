import axios, { type InternalAxiosRequestConfig } from 'axios'
import { ApiError } from './types'

const CSRF_HEADER = 'X-CSRF-Token'
const csrfHttp = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
})

let csrfToken = ''
let csrfTokenRequest: Promise<string> | null = null
let sessionInvalidationHandler: (() => void) | null = null

export function registerSessionInvalidationHandler(handler: () => void) {
  sessionInvalidationHandler = handler
  return () => {
    if (sessionInvalidationHandler === handler) {
      sessionInvalidationHandler = null
    }
  }
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
})

http.interceptors.request.use(async (config) => {
  if (isUnsafeMethod(config.method)) {
    setCsrfHeader(config, await loadCsrfToken())
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const data = await readErrorResponseData(error)
    if (data?.code === 'CSRF_TOKEN_INVALID') {
      csrfToken = ''
      const config = error.config as (InternalAxiosRequestConfig & { csrfRetried?: boolean }) | undefined
      if (config && !config.csrfRetried && isUnsafeMethod(config.method)) {
        config.csrfRetried = true
        setCsrfHeader(config, await loadCsrfToken())
        return http(config)
      }
    }
    if (data?.code && data.code !== 'SUCCESS') {
      const apiError = new ApiError(String(data.code), data.message || String(data.code), error.response.status || 0)
      if (apiError.code === 'ACCOUNT_LOGGED_IN_ELSEWHERE') {
        sessionInvalidationHandler?.()
      }
      return Promise.reject(apiError)
    }
    return Promise.reject(error)
  },
)

function isUnsafeMethod(method?: string) {
  return ['post', 'put', 'patch', 'delete'].includes((method || 'get').toLowerCase())
}

async function readErrorResponseData(error: any) {
  const data = error?.response?.data
  if (isJsonBlob(data, error?.response?.headers)) {
    return readJsonBlob(data)
  }
  return data
}

function isJsonBlob(data: any, headers: any) {
  return typeof Blob !== 'undefined' && data instanceof Blob && responseContentType(headers).includes('application/json')
}

async function readJsonBlob(blob: Blob) {
  const text = await blob.text()
  if (!text) {
    return null
  }
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

function responseContentType(headers: any) {
  const value = typeof headers?.get === 'function'
    ? headers.get('content-type')
    : headers?.['content-type'] || headers?.['Content-Type']
  return String(value || '').toLowerCase()
}

async function loadCsrfToken() {
  if (csrfToken) {
    return csrfToken
  }
  if (!csrfTokenRequest) {
    csrfTokenRequest = csrfHttp.get('/auth/csrf')
      .then((response) => {
        const token = response.data?.data?.token
        if (!token) {
          throw new Error('CSRF token missing')
        }
        csrfToken = token
        return token
      })
      .finally(() => {
        csrfTokenRequest = null
      })
  }
  return csrfTokenRequest
}

function setCsrfHeader(config: InternalAxiosRequestConfig, token: string) {
  const headers = config.headers as any
  if (headers && typeof headers.set === 'function') {
    headers.set(CSRF_HEADER, token)
    return
  }
  config.headers = {
    ...(headers || {}),
    [CSRF_HEADER]: token,
  } as any
}
