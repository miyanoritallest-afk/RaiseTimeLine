import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useDebounce } from './useDebounce'

describe('useDebounce', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  // BB同値分割（初期状態）: render直後は初期値と同じ
  it('returnsInitialValueImmediately', () => {
    const { result } = renderHook(() => useDebounce('hello', 400))
    expect(result.current).toBe('hello')
  })

  // WB分岐（タイマー未発火）: 399ms経過 → まだ旧値
  it('doesNotUpdateBeforeDelay', () => {
    const { result, rerender } = renderHook(
      ({ value }: { value: string }) => useDebounce(value, 400),
      { initialProps: { value: 'old' } }
    )
    rerender({ value: 'new' })
    act(() => { vi.advanceTimersByTime(399) })
    expect(result.current).toBe('old')
  })

  // WB分岐（タイマー発火）: 400ms経過 → 新値に更新
  it('updatesAfterDelay', () => {
    const { result, rerender } = renderHook(
      ({ value }: { value: string }) => useDebounce(value, 400),
      { initialProps: { value: 'old' } }
    )
    rerender({ value: 'new' })
    act(() => { vi.advanceTimersByTime(400) })
    expect(result.current).toBe('new')
  })

  // WB（タイマーリセット）: 200msごとに値変更×3回後400ms経過 → 最後の値のみ反映
  it('resetsTimerOnRapidChange', () => {
    const { result, rerender } = renderHook(
      ({ value }: { value: string }) => useDebounce(value, 400),
      { initialProps: { value: 'a' } }
    )
    act(() => { vi.advanceTimersByTime(200) })
    rerender({ value: 'b' })
    act(() => { vi.advanceTimersByTime(200) })
    rerender({ value: 'c' })
    act(() => { vi.advanceTimersByTime(200) })
    rerender({ value: 'final' })
    // まだ更新されていない
    expect(result.current).toBe('a')
    act(() => { vi.advanceTimersByTime(400) })
    expect(result.current).toBe('final')
  })

  // BB同値分割（カスタム遅延）: delay=1000、1000ms経過 → 新値に更新
  it('customDelay1000ms_updatesAfter1000ms', () => {
    const { result, rerender } = renderHook(
      ({ value }: { value: string }) => useDebounce(value, 1000),
      { initialProps: { value: 'old' } }
    )
    rerender({ value: 'new' })
    act(() => { vi.advanceTimersByTime(1000) })
    expect(result.current).toBe('new')
  })

  // BB境界値: delay=1000、999ms経過 → まだ旧値
  it('customDelay1000ms_noUpdateAt999ms', () => {
    const { result, rerender } = renderHook(
      ({ value }: { value: string }) => useDebounce(value, 1000),
      { initialProps: { value: 'old' } }
    )
    rerender({ value: 'new' })
    act(() => { vi.advanceTimersByTime(999) })
    expect(result.current).toBe('old')
  })
})
