import { describe, expect, it } from 'vitest'
import { daysUntil, getPregnancyProgress, isPastDate } from '@/lib/pregnancy'

function toLocalIso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

describe('daysUntil', () => {
  it('counts whole days between two local dates regardless of time zone parsing', () => {
    expect(daysUntil('2026-09-01', new Date(2026, 7, 25))).toBe(7)
  })

  it('is negative once the due date has passed', () => {
    expect(daysUntil('2026-01-01', new Date(2026, 0, 5))).toBe(-4)
  })

  it('is zero on the due date itself', () => {
    expect(daysUntil('2026-01-01', new Date(2026, 0, 1))).toBe(0)
  })
})

describe('getPregnancyProgress', () => {
  it('reports week 1, day 1 far before the due date', () => {
    const progress = getPregnancyProgress('2026-12-31', new Date(2026, 0, 1))
    expect(progress.week).toBe(1)
    expect(progress.dayOfWeek).toBe(1)
    expect(progress.hasArrived).toBe(false)
  })

  it('reports week 40 and hasArrived on the due date', () => {
    const progress = getPregnancyProgress('2026-06-01', new Date(2026, 5, 1))
    expect(progress.week).toBe(40)
    expect(progress.hasArrived).toBe(true)
    expect(progress.moonFraction).toBe(1)
  })

  it('advances day-of-week within a pregnancy week', () => {
    const from = new Date(2026, 0, 1)
    const due = new Date(from)
    due.setDate(due.getDate() + 280 - 10) // 10 days elapsed -> week 2, day 4
    const dueIso = toLocalIso(due)
    const progress = getPregnancyProgress(dueIso, from)
    expect(progress.week).toBe(2)
    expect(progress.dayOfWeek).toBe(4)
  })

  it('clamps moonFraction and week at term even past the due date', () => {
    const progress = getPregnancyProgress('2026-01-01', new Date(2026, 1, 1))
    expect(progress.week).toBe(40)
    expect(progress.moonFraction).toBe(1)
  })
})

describe('isPastDate', () => {
  it('treats the same day as not past', () => {
    expect(isPastDate('2026-03-10', new Date(2026, 2, 10))).toBe(false)
  })

  it('treats an earlier date as past', () => {
    expect(isPastDate('2026-03-10', new Date(2026, 2, 11))).toBe(true)
  })
})
