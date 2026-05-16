import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { formatDate } from './formatDate'

const FIXED_NOW = new Date('2025-06-01T12:00:00Z')

describe('formatDate', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(FIXED_NOW)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  function isoSecondsAgo(secs: number): string {
    return new Date(FIXED_NOW.getTime() - secs * 1000).toISOString()
  }

  // BB同値分割（区間1代表値）: 30秒前 → "30秒"
  it('lessThan60sec_showsSeconds', () => {
    expect(formatDate(isoSecondsAgo(30))).toBe('30秒')
  })

  // BB境界値（分岐1上限）: 59秒前 → "59秒"
  it('exactly59sec_showsSeconds', () => {
    expect(formatDate(isoSecondsAgo(59))).toBe('59秒')
  })

  // BB境界値（分岐2下限）: 60秒前 → "1分"
  it('exactly60sec_showsMinutes', () => {
    expect(formatDate(isoSecondsAgo(60))).toBe('1分')
  })

  // BB同値分割（区間2代表値）: 45分前 → "45分"
  it('between60secAnd3600sec_showsMinutes', () => {
    expect(formatDate(isoSecondsAgo(45 * 60))).toBe('45分')
  })

  // BB境界値（分岐2上限）: 3599秒前 → "59分"
  it('exactly3599sec_showsMinutes', () => {
    expect(formatDate(isoSecondsAgo(3599))).toBe('59分')
  })

  // BB境界値（分岐3下限）: 3600秒前 → "1時間"
  it('exactly3600sec_showsHours', () => {
    expect(formatDate(isoSecondsAgo(3600))).toBe('1時間')
  })

  // BB同値分割（区間3代表値）: 3時間前 → "3時間"
  it('between3600And86400_showsHours', () => {
    expect(formatDate(isoSecondsAgo(3 * 3600))).toBe('3時間')
  })

  // BB境界値（分岐3上限）: 86399秒前 → "23時間"
  it('exactly86399sec_showsHours', () => {
    expect(formatDate(isoSecondsAgo(86399))).toBe('23時間')
  })

  // BB境界値（分岐4下限）: 86400秒前 → 日付文字列
  it('exactly86400sec_showsDate', () => {
    const result = formatDate(isoSecondsAgo(86400))
    // 月日 HH:mm 形式であることを確認
    expect(result).toMatch(/\d+月\d+日 \d{2}:\d{2}/)
  })

  // BB同値分割（区間4代表値）: 2日前 → 日付文字列
  it('moreThan1Day_showsDate', () => {
    const result = formatDate(isoSecondsAgo(2 * 86400))
    expect(result).toMatch(/\d+月\d+日 \d{2}:\d{2}/)
  })
})
