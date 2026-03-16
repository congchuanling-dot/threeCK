/**
 * 统一 HTTP 客户端：封装 fetch、错误处理、JSON 解析。
 */
const DEFAULT_HEADERS = {
  'Content-Type': 'application/json',
}

/**
 * @param {string} url
 * @param {RequestInit} init
 * @returns {Promise<Object>} 解析后的 JSON
 */
export async function request(url, init = {}) {
  const res = await fetch(url, {
    ...init,
    headers: { ...DEFAULT_HEADERS, ...init.headers },
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw new Error(data?.message ?? `HTTP ${res.status}`)
  }
  return data
}

/**
 * GET 请求
 */
export function get(url) {
  return request(url, { method: 'GET' })
}

/**
 * POST 请求
 */
export function post(url, body) {
  return request(url, {
    method: 'POST',
    body: body != null ? JSON.stringify(body) : undefined,
  })
}
