interface AvatarProps {
  avatarUrl: string | null
  username: string
  size?: 'sm' | 'md' | 'lg'
}

const SIZE_MAP = { sm: 32, md: 40, lg: 72 }

export default function Avatar({ avatarUrl, username, size = 'md' }: AvatarProps) {
  const px = SIZE_MAP[size]
  const className = size === 'lg' ? 'avatar avatar-lg' : 'avatar'

  return (
    <div className={className} style={{ width: px, height: px }}>
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
