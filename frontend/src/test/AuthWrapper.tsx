import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../contexts/AuthContext'
import type { ReactNode } from 'react'

interface Props {
  children: ReactNode
  initialUser?: {
    id: number
    username: string
    email: string
    bio: string | null
    avatarUrl: string | null
  } | null
}

export function AuthWrapper({ children, initialUser }: Props) {
  if (initialUser !== undefined && initialUser !== null) {
    localStorage.setItem('user', JSON.stringify(initialUser))
    localStorage.setItem('accessToken', 'test-access-token')
    localStorage.setItem('refreshToken', 'test-refresh-token')
  }
  return (
    <MemoryRouter>
      <AuthProvider>{children}</AuthProvider>
    </MemoryRouter>
  )
}
