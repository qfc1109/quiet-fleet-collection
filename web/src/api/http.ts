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
    if (error?.response?.data?.code === 'CSRF_TOKEN_INVALID') {
      csrfToken = ''
      const config = error.config as (InternalAxiosRequestConfig & { csrfRetried?: boolean }) | undefined
      if (config && !config.csrfRetried && isUnsafeMethod(config.method)) {
        config.csrfRetried = true
        setCsrfHeader(config, await loadCsrfToken())
        return http(config)
      }
    }
    const data = error?.response?.data
    if (data?.code && data.code !== 'SUCCESS') {
      return Promise.reject(new ApiError(String(data.code), data.message || String(data.code), error.response.status || 0))
    }
    return Promise.reject(error)
  },
)

function isUnsafeMethod(method?: string) {
  return ['post', 'put', 'patch', 'delete'].includes((method || 'get').toLowerCase())
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
