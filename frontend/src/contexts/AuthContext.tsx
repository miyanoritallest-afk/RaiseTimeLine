import { createContext, useContext, useState, useCallback, type ReactNode } from 'react'
import client from '../api/client'

interface User {
  id: number
  username: string
  email: string
  bio: string | null
  avatarUrl: string | null
}

interface AuthContextValue {
  user: User | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (username: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  updateUser: (partial: Partial<User>) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function loadUser(): User | null {
  try {
    const raw = localStorage.getItem('user')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(loadUser)

  const saveTokens = (accessToken: string, refreshToken: string, userData: User) => {
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    localStorage.setItem('user', JSON.stringify(userData))
    setUser(userData)
  }

  const login = useCallback(async (email: string, password: string) => {
    const { data } = await client.post('/auth/login', { email, password })
    saveTokens(data.accessToken, data.refreshToken, data.user)
  }, [])

  const register = useCallback(async (username: string, email: string, password: string) => {
    const { data } = await client.post('/auth/register', { username, email, password })
    saveTokens(data.accessToken, data.refreshToken, data.user)
  }, [])

  const updateUser = useCallback((partial: Partial<User>) => {
    setUser((prev) => {
      if (!prev) return prev
      const updated = { ...prev, ...partial }
      localStorage.setItem('user', JSON.stringify(updated))
      return updated
    })
  }, [])

  const logout = useCallback(async () => {
    const refreshToken = localStorage.getItem('refreshToken')
    if (refreshToken) {
      try {
        await client.post('/auth/logout', { refreshToken })
      } catch {
        // サーバーエラーでもクライアント側はクリアする
      }
    }
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, register, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
