import { describe, it, expect, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useRef } from 'react'
import { useClickOutside } from './useClickOutside'
import { fireEvent } from '@testing-library/react'

describe('useClickOutside', () => {
  // WB分岐（contains=false）: ref外の要素をクリック → callback呼び出し
  it('clickOutside_callsCallback', () => {
    const callback = vi.fn()
    const outsideEl = document.createElement('div')
    document.body.appendChild(outsideEl)

    const { unmount } = renderHook(() => {
      const ref = useRef<HTMLDivElement>(document.createElement('div'))
      useClickOutside(ref, callback)
    })

    fireEvent.mouseDown(outsideEl)
    expect(callback).toHaveBeenCalledOnce()

    unmount()
    document.body.removeChild(outsideEl)
  })

  // WB分岐（contains=true）: ref内の子要素をクリック → callback未呼び出し
  it('clickInside_doesNotCallCallback', () => {
    const callback = vi.fn()
    const container = document.createElement('div')
    const child = document.createElement('span')
    container.appendChild(child)
    document.body.appendChild(container)

    renderHook(() => {
      const ref = { current: container }
      useClickOutside(ref as React.RefObject<HTMLElement>, callback)
    })

    fireEvent.mouseDown(child)
    expect(callback).not.toHaveBeenCalled()

    document.body.removeChild(container)
  })

  // WB条件網羅（ref要素自体）: ref要素自体をクリック → callback未呼び出し
  it('clickOnRef_doesNotCallCallback', () => {
    const callback = vi.fn()
    const container = document.createElement('div')
    document.body.appendChild(container)

    renderHook(() => {
      const ref = { current: container }
      useClickOutside(ref as React.RefObject<HTMLElement>, callback)
    })

    fireEvent.mouseDown(container)
    expect(callback).not.toHaveBeenCalled()

    document.body.removeChild(container)
  })

  // WB（cleanup確認）: アンマウント後に外部クリック → callback未呼び出し
  it('unmount_removesEventListener', () => {
    const callback = vi.fn()
    const outsideEl = document.createElement('div')
    document.body.appendChild(outsideEl)

    const { unmount } = renderHook(() => {
      const ref = useRef<HTMLDivElement>(document.createElement('div'))
      useClickOutside(ref, callback)
    })

    unmount()
    fireEvent.mouseDown(outsideEl)
    expect(callback).not.toHaveBeenCalled()

    document.body.removeChild(outsideEl)
  })
})

// React import for type usage
import type React from 'react'
