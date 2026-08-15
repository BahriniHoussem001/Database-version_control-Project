const TOKEN_KEY = 'dbvc_auth_token'
const USER_KEY = 'dbvc_auth_user'

export function saveAuthSession(loginResponse) {
  localStorage.setItem(TOKEN_KEY, loginResponse.token)
  localStorage.setItem(
    USER_KEY,
    JSON.stringify({
      username: loginResponse.username,
      role: loginResponse.role,
      expiresAt: loginResponse.expiresAt,
    })
  )
}

export function getAuthToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getAuthUser() {
  const rawUser = localStorage.getItem(USER_KEY)
  if (!rawUser) return null

  try {
    return JSON.parse(rawUser)
  } catch {
    return null
  }
}

export function isAuthenticated() {
  return Boolean(getAuthToken() && getAuthUser())
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}