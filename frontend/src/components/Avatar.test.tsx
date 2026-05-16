import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import Avatar from './Avatar'

describe('Avatar', () => {
  // BB同値分割（有効URL）: avatarUrl あり → <img> が存在
  it('withAvatarUrl_rendersImg', () => {
    render(<Avatar avatarUrl="https://example.com/avatar.jpg" username="alice" />)
    const img = screen.getByRole('img')
    expect(img).toBeInTheDocument()
    expect(img).toHaveAttribute('src', 'https://example.com/avatar.jpg')
    expect(img).toHaveAttribute('alt', 'alice')
  })

  // BB同値分割（null）: avatarUrl=null → <img> なし、SVG 存在
  it('withoutAvatarUrl_rendersFallbackSvg', () => {
    const { container } = render(<Avatar avatarUrl={null} username="alice" />)
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(container.querySelector('svg')).toBeInTheDocument()
  })

  // BB同値分割（sm）: size="sm" → avatar-sm クラス
  it('sizeSm_appliesSmClass', () => {
    const { container } = render(<Avatar avatarUrl={null} username="alice" size="sm" />)
    expect(container.firstChild).toHaveClass('avatar-sm')
  })

  // BB同値分割（md）: size="md"（デフォルト）→ avatar-sm・avatar-lg なし
  it('sizeMd_defaultClass', () => {
    const { container } = render(<Avatar avatarUrl={null} username="alice" size="md" />)
    expect(container.firstChild).not.toHaveClass('avatar-sm')
    expect(container.firstChild).not.toHaveClass('avatar-lg')
  })

  // BB同値分割（lg）: size="lg" → avatar-lg クラス
  it('sizeLg_appliesLgClass', () => {
    const { container } = render(<Avatar avatarUrl={null} username="alice" size="lg" />)
    expect(container.firstChild).toHaveClass('avatar-lg')
  })

  // WB分岐（onClick あり）: cursor:pointer スタイル
  it('withOnClick_hasPointerCursor', () => {
    const { container } = render(<Avatar avatarUrl={null} username="alice" onClick={() => {}} />)
    expect(container.firstChild).toHaveStyle({ cursor: 'pointer' })
  })

  // WB分岐（onClick なし）: pointer cursor なし
  it('withoutOnClick_noPointerCursor', () => {
    const { container } = render(<Avatar avatarUrl={null} username="alice" />)
    const el = container.firstChild as HTMLElement
    expect(el.style.cursor).toBe('')
  })

  // BB（インタラクション）: クリックで callback 呼び出し
  it('withOnClick_clickTriggersCallback', () => {
    const onClick = vi.fn()
    const { container } = render(<Avatar avatarUrl={null} username="alice" onClick={onClick} />)
    fireEvent.click(container.firstChild as HTMLElement)
    expect(onClick).toHaveBeenCalledOnce()
  })
})
