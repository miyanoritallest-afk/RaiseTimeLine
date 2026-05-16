import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import FollowButton from './FollowButton'
import { server } from '../test/mswServer'

describe('FollowButton', () => {
  const onToggle = vi.fn()

  // BB同値分割（isFollowing=false）: "フォローする" テキスト
  it('notFollowing_showsFollowButton', () => {
    render(<FollowButton targetUserId={2} isFollowing={false} onToggle={onToggle} />)
    expect(screen.getByRole('button')).toHaveTextContent('フォローする')
  })

  // BB同値分割（isFollowing=true）: "フォロー中" テキスト
  it('following_showsFollowingButton', () => {
    render(<FollowButton targetUserId={2} isFollowing={true} onToggle={onToggle} />)
    expect(screen.getByRole('button')).toHaveTextContent('フォロー中')
  })

  // BB（インタラクション）: フォローボタンクリック → POST /api/users/2/follow
  it('click_followButton_callsFollowApi', async () => {
    let called = false
    server.use(
      http.post('/api/users/2/follow', () => {
        called = true
        return HttpResponse.json({ userId: 2, followersCount: 1, followingCount: 0, isFollowing: true }, { status: 201 })
      })
    )
    render(<FollowButton targetUserId={2} isFollowing={false} onToggle={onToggle} />)
    fireEvent.click(screen.getByRole('button'))
    await waitFor(() => expect(called).toBe(true))
  })

  // BB（インタラクション）: フォロー中ボタンクリック → DELETE /api/users/2/follow
  it('click_followingButton_callsUnfollowApi', async () => {
    let called = false
    server.use(
      http.delete('/api/users/2/follow', () => {
        called = true
        return HttpResponse.json({ userId: 2, followersCount: 0, followingCount: 0, isFollowing: false })
      })
    )
    render(<FollowButton targetUserId={2} isFollowing={true} onToggle={onToggle} />)
    fireEvent.click(screen.getByRole('button'))
    await waitFor(() => expect(called).toBe(true))
  })

  // WB（楽観的更新）: クリック直後（API応答前）に onToggle が !isFollowing で呼ばれる
  it('click_optimisticallyUpdatesUI', async () => {
    const toggle = vi.fn()
    server.use(
      http.post('/api/users/2/follow', async () => {
        await new Promise(r => setTimeout(r, 100))
        return HttpResponse.json({ userId: 2, followersCount: 1, followingCount: 0, isFollowing: true }, { status: 201 })
      })
    )
    render(<FollowButton targetUserId={2} isFollowing={false} onToggle={toggle} />)
    fireEvent.click(screen.getByRole('button'))
    // 楽観的更新: クリック直後に呼ばれている
    expect(toggle).toHaveBeenCalledWith(true)
  })

  // WB分岐（catch: APIエラー）: 500返却 → onToggle が旧値で呼び直される
  it('apiFailure_revertsUI', async () => {
    const toggle = vi.fn()
    server.use(
      http.post('/api/users/2/follow', () =>
        HttpResponse.json({ message: 'error' }, { status: 500 })
      )
    )
    render(<FollowButton targetUserId={2} isFollowing={false} onToggle={toggle} />)
    fireEvent.click(screen.getByRole('button'))
    // 楽観的更新 (true) → エラー後にリセット (false)
    await waitFor(() => {
      const calls = toggle.mock.calls
      expect(calls.some(([v]) => v === true)).toBe(true)
      expect(calls.some(([v]) => v === false)).toBe(true)
    })
  })

  // WB分岐（loading中）: API保留中にボタンがdisabledになる
  it('loading_disablesButton', async () => {
    server.use(
      http.post('/api/users/2/follow', async () => {
        await new Promise(r => setTimeout(r, 200))
        return HttpResponse.json({ userId: 2, followersCount: 1, followingCount: 0, isFollowing: true }, { status: 201 })
      })
    )
    render(<FollowButton targetUserId={2} isFollowing={false} onToggle={onToggle} />)
    fireEvent.click(screen.getByRole('button'))
    expect(screen.getByRole('button')).toBeDisabled()
  })
})
