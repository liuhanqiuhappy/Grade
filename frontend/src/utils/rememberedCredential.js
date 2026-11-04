/**
 * 「记住密码」本地凭据缓存（契约 §3.8）。
 *
 * 纪律：
 * 1. 只写 localStorage，不写 Cookie，不随请求发送；
 * 2. 只有用户勾选且登录成功时才写入；登录失败不写；
 * 3. Base64 只是编码不是加密，本机可还原明文 —— 仅课程演示可接受，
 *    生产应改用服务端签发的 HttpOnly 长效令牌。
 */

const STORAGE_KEY = 'gm.rememberedCredential'
const VERSION = 1
// 30 天，过期自动丢弃
const TTL_MS = 30 * 24 * 60 * 60 * 1000

function toBase64(text) {
  const bytes = new TextEncoder().encode(text)
  let binary = ''
  bytes.forEach((b) => {
    binary += String.fromCharCode(b)
  })
  return btoa(binary)
}

function fromBase64(base64) {
  const binary = atob(base64)
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

/**
 * 保存凭据。调用前必须已确认「勾选 + 登录成功」。
 * @returns {boolean} 是否写入成功（隐私模式下 localStorage 可能不可用）
 */
export function saveCredential(username, password) {
  const payload = {
    v: VERSION,
    username,
    password,
    savedAt: Date.now()
  }
  try {
    localStorage.setItem(STORAGE_KEY, toBase64(JSON.stringify(payload)))
    return true
  } catch (e) {
    // 无痕模式 / 配额满：功能降级为不记住，不影响登录
    console.warn('记住密码写入失败，已跳过', e)
    return false
  }
}

/**
 * 读取凭据。记录损坏、版本不符或超过 30 天时直接删除并返回 null。
 * @returns {{username: string, password: string} | null}
 */
export function loadCredential() {
  let raw = null
  try {
    raw = localStorage.getItem(STORAGE_KEY)
  } catch (e) {
    return null
  }
  if (!raw) {
    return null
  }

  try {
    const data = JSON.parse(fromBase64(raw))
    const valid =
      data &&
      data.v === VERSION &&
      typeof data.username === 'string' &&
      typeof data.password === 'string' &&
      typeof data.savedAt === 'number' &&
      data.username.length > 0 &&
      data.password.length > 0
    if (!valid || Date.now() - data.savedAt > TTL_MS) {
      clearCredential()
      return null
    }
    return { username: data.username, password: data.password }
  } catch (e) {
    clearCredential()
    return null
  }
}

/** 清除凭据（取消勾选后登录成功、记录非法、用户主动清理时调用） */
export function clearCredential() {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch (e) {
    console.warn('清除记住密码记录失败', e)
  }
}
