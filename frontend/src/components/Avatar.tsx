interface AvatarProps {
  avatarUrl: string | null
  username: string
  size?: 'sm' | 'md' | 'lg'
  onClick?: () => void
}

const SIZE_CLASS: Record<string, string> = { sm: 'avatar avatar-sm', md: 'avatar', lg: 'avatar avatar-lg' }

export default function Avatar({ avatarUrl, username, size = 'md', onClick }: AvatarProps) {
  return (
    <div className={SIZE_CLASS[size]} onClick={onClick} style={onClick ? { cursor: 'pointer' } : undefined}>
      {avatarUrl ? (
        <img src={avatarUrl} alt={username} />
      ) : (
        <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect width="40" height="40" fill="#cfd9de" />
          <circle cx="20" cy="16" r="7" fill="#8899a6" />
          <ellipse cx="20" cy="34" rx="12" ry="8" fill="#8899a6" />
        </svg>
      )}
    </div>
  )
}
