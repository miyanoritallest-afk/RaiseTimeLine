import { describe, it, expect } from 'vitest'
import { getCharCountClass, POST_MAX_LENGTH } from './charCount'

describe('getCharCountClass', () => {
  // BB同値分割（区間3代表値）: 0 → "char-count"
  it('length0_returnsNormal', () => {
    expect(getCharCountClass(0)).toBe('char-count')
  })

  // BB境界値（分岐2下限-1）: 259 → "char-count"
  it('length259_returnsNormal', () => {
    expect(getCharCountClass(259)).toBe('char-count')
  })

  // BB境界値（分岐2下限）: 260 → "char-count warning"
  it('length260_returnsWarning', () => {
    expect(getCharCountClass(260)).toBe('char-count warning')
  })

  // BB境界値（分岐1下限-1）: 269 → "char-count warning"
  it('length269_returnsWarning', () => {
    expect(getCharCountClass(269)).toBe('char-count warning')
  })

  // BB境界値（分岐1下限）: 270 → "char-count danger"
  it('length270_returnsDanger', () => {
    expect(getCharCountClass(270)).toBe('char-count danger')
  })

  // BB境界値（最大有効）: 280 → "char-count danger"
  it('length280_returnsDanger', () => {
    expect(getCharCountClass(280)).toBe('char-count danger')
  })

  // BB境界値（超過）: 281 → "char-count danger"
  it('length281_returnsDanger', () => {
    expect(getCharCountClass(281)).toBe('char-count danger')
  })

  // BB（定数確認）: POST_MAX_LENGTH は 280
  it('POST_MAX_LENGTH_is280', () => {
    expect(POST_MAX_LENGTH).toBe(280)
  })
})
