import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import PostCard from './PostCard'
import { makePost } from '../test/factories'
import type { PostResponse } from '../types/post'

const noop = () => {}

function renderPostCard(post: PostResponse, currentUserId = 99) {
  return render(
    <MemoryRouter>
      <PostCard
        post={post}
        currentUserId={currentUserId}
        onLike={noop}
        onEdit={noop}
        onDelete={noop}
        onComment={noop}
      />
    </MemoryRouter>
  )
}

describe('PostCard', () => {
  // BB（仕様確認）: コンテンツが DOM に存在
  it('rendersContent', () => {
    renderPostCard(makePost({ content: 'hello world' }))
    expect(screen.getByText('hello world')).toBeInTheDocument()
  })

  // BB（仕様確認）: 著者名が存在
  it('rendersAuthorUsername', () => {
    renderPostCard(makePost({ author: { id: 1, username: 'alice', avatarUrl: null } }))
    expect(screen.getByText('alice')).toBeInTheDocument()
  })

  // WB分岐1（likeCount=0）: いいね数バッジ非表示
  it('likeCount0_hidesBadge', () => {
    renderPostCard(makePost({ likeCount: 0 }))
    expect(screen.queryByText('0')).not.toBeInTheDocument()
  })

  // WB分岐1（likeCount>0）: "5" バッジ表示
  it('likeCount5_showsBadge', () => {
    renderPostCard(makePost({ likeCount: 5 }))
    expect(screen.getByText('5')).toBeInTheDocument()
  })

  // WB分岐2（likedByMe=false）: ♡ アイコン
  it('likedByMe_false_showsHollowHeart', () => {
    renderPostCard(makePost({ likedByMe: false }))
    expect(screen.getByText('♡')).toBeInTheDocument()
  })

  // WB分岐2（likedByMe=true）: ♥ アイコン、liked クラス
  it('likedByMe_true_showsFilledHeart', () => {
    renderPostCard(makePost({ likedByMe: true }))
    expect(screen.getByText('♥')).toBeInTheDocument()
  })

  // WB分岐3（imageUrls=[]）: 画像セクション非表示
  it('noImages_hidesImageSection', () => {
    const { container } = renderPostCard(makePost({ imageUrls: [] }))
    expect(container.querySelector('.post-images')).not.toBeInTheDocument()
  })

  // WB分岐3（imageUrls=1件以上）: <img> 存在
  it('withImages_showsImages', () => {
    const { container } = renderPostCard(makePost({ imageUrls: ['https://example.com/img.jpg'] }))
    const postImg = container.querySelector('.post-img')
    expect(postImg).toBeInTheDocument()
    expect(postImg).toHaveAttribute('src', 'https://example.com/img.jpg')
  })

  // WB分岐4（自分の投稿）: メニューボタン存在
  it('ownPost_showsMenuButton', () => {
    renderPostCard(makePost({ author: { id: 1, username: 'alice', avatarUrl: null } }), 1)
    expect(screen.getByRole('button', { name: /···/ })).toBeInTheDocument()
  })

  // WB分岐4（他人の投稿）: メニューボタン非表示
  it('otherPost_hidesMenuButton', () => {
    renderPostCard(makePost({ author: { id: 1, username: 'alice', avatarUrl: null } }), 99)
    expect(screen.queryByRole('button', { name: /···/ })).not.toBeInTheDocument()
  })

  // BB（インタラクション）: メニューボタンクリック → 編集・削除ドロップダウン表示
  it('menuButton_click_showsDropdown', () => {
    renderPostCard(makePost({ author: { id: 1, username: 'alice', avatarUrl: null } }), 1)
    fireEvent.click(screen.getByRole('button', { name: /···/ }))
    expect(screen.getByText('編集')).toBeInTheDocument()
    expect(screen.getByText('削除')).toBeInTheDocument()
  })

  // WB（confirm=true）: confirm承認 → onDelete 呼び出し
  it('delete_confirmTrue_callsOnDelete', () => {
    const onDelete = vi.fn()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(
      <MemoryRouter>
        <PostCard
          post={makePost({ id: 42, author: { id: 1, username: 'alice', avatarUrl: null } })}
          currentUserId={1}
          onLike={noop}
          onEdit={noop}
          onDelete={onDelete}
          onComment={noop}
        />
      </MemoryRouter>
    )
    fireEvent.click(screen.getByRole('button', { name: /···/ }))
    fireEvent.click(screen.getByText('削除'))
    expect(onDelete).toHaveBeenCalledWith(42)
    vi.restoreAllMocks()
  })

  // WB（confirm=false）: confirm拒否 → onDelete 未呼び出し
  it('delete_confirmFalse_doesNotCallOnDelete', () => {
    const onDelete = vi.fn()
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderPostCard(makePost({ author: { id: 1, username: 'alice', avatarUrl: null } }), 1)
    fireEvent.click(screen.getByRole('button', { name: /···/ }))
    fireEvent.click(screen.getByText('削除'))
    expect(onDelete).not.toHaveBeenCalled()
    vi.restoreAllMocks()
  })

  // BB（インタラクション）: いいねボタンクリック → onLike(post.id, post.likedByMe)
  it('like_click_callsOnLikeWithCorrectArgs', () => {
    const onLike = vi.fn()
    render(
      <MemoryRouter>
        <PostCard
          post={makePost({ id: 7, likedByMe: false })}
          currentUserId={99}
          onLike={onLike}
          onEdit={noop}
          onDelete={noop}
          onComment={noop}
        />
      </MemoryRouter>
    )
    fireEvent.click(screen.getByText('♡'))
    expect(onLike).toHaveBeenCalledWith(7, false)
  })
})
