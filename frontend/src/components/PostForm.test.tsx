import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import PostForm from './PostForm'
import { AuthWrapper } from '../test/AuthWrapper'

const ALICE = { id: 1, username: 'alice', email: 'alice@test.com', bio: null, avatarUrl: null }

function renderPostForm(onPosted = vi.fn()) {
  return render(
    <AuthWrapper initialUser={ALICE}>
      <PostForm onPosted={onPosted} />
    </AuthWrapper>
  )
}

function getSubmitButton() {
  return screen.getByRole('button', { name: /投稿する/ })
}

describe('PostForm', () => {
  // WB分岐（auth=null）: user がない → コンポーネント非表示
  it('noUser_rendersNothing', () => {
    const { container } = render(
      <AuthWrapper initialUser={null}>
        <PostForm onPosted={vi.fn()} />
      </AuthWrapper>
    )
    expect(container.querySelector('.post-form-wrapper')).not.toBeInTheDocument()
  })

  // WB分岐1（T）: コンテンツ空・画像なし → submit disabled
  it('emptyContent_noImages_submitDisabled', () => {
    renderPostForm()
    expect(getSubmitButton()).toBeDisabled()
  })

  // BB同値分割（テキストのみ）: content="a" → submit enabled
  it('contentPresent_noImages_submitEnabled', () => {
    renderPostForm()
    fireEvent.change(screen.getByPlaceholderText('いまどうしてる？'), { target: { value: 'a' } })
    expect(getSubmitButton()).not.toBeDisabled()
  })

  // BB境界値（最大有効=280文字）: 280文字 → enabled
  it('content280chars_submitEnabled', () => {
    renderPostForm()
    fireEvent.change(screen.getByPlaceholderText('いまどうしてる？'), { target: { value: 'a'.repeat(280) } })
    expect(getSubmitButton()).not.toBeDisabled()
  })

  // BB境界値（max+1=281文字）/ WB分岐2（T）: 281文字 → disabled
  it('content281chars_submitDisabled', () => {
    renderPostForm()
    fireEvent.change(screen.getByPlaceholderText('いまどうしてる？'), { target: { value: 'a'.repeat(281) } })
    expect(getSubmitButton()).toBeDisabled()
  })

  // BB境界値（警告開始）: 260文字 → charCount に warning クラス
  it('charCount_at260_showsWarningClass', () => {
    const { container } = renderPostForm()
    fireEvent.change(screen.getByPlaceholderText('いまどうしてる？'), { target: { value: 'a'.repeat(260) } })
    expect(container.querySelector('.char-count.warning')).toBeInTheDocument()
  })

  // BB境界値（危険開始）: 270文字 → charCount に danger クラス
  it('charCount_at270_showsDangerClass', () => {
    const { container } = renderPostForm()
    fireEvent.change(screen.getByPlaceholderText('いまどうしてる？'), { target: { value: 'a'.repeat(270) } })
    expect(container.querySelector('.char-count.danger')).toBeInTheDocument()
  })

  // BB（ハッピーパス）: 投稿成功 → onPosted 呼び出し、textarea 空
  it('successfulSubmit_callsOnPostedAndClearsTextarea', async () => {
    const onPosted = vi.fn()
    renderPostForm(onPosted)
    const textarea = screen.getByPlaceholderText('いまどうしてる？')
    fireEvent.change(textarea, { target: { value: 'test post' } })
    fireEvent.click(getSubmitButton())
    await waitFor(() => expect(onPosted).toHaveBeenCalled())
    expect(textarea).toHaveValue('')
  })

  // WB分岐5（T）: Ctrl+Enter → API呼び出し
  it('ctrlEnter_submitsForm', async () => {
    const onPosted = vi.fn()
    renderPostForm(onPosted)
    const textarea = screen.getByPlaceholderText('いまどうしてる？')
    await userEvent.type(textarea, 'ctrl enter test')
    await userEvent.keyboard('{Control>}{Enter}{/Control}')
    await waitFor(() => expect(onPosted).toHaveBeenCalled())
  })
})
