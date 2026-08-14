import { describe, expect, it } from 'vitest'
import { calculateBudget, itemEffectivePrice } from '@/features/shopping/budget'
import type { ShoppingItem } from '@/types/models'

function makeItem(overrides: Partial<ShoppingItem>): ShoppingItem {
  return {
    id: crypto.randomUUID(),
    name: 'פריט',
    category: 'תינוקייה',
    priority: 'normal',
    status: 'need',
    alternatives: [],
    createdAt: 0,
    ...overrides,
  }
}

describe('itemEffectivePrice', () => {
  it('prefers actualPrice over everything else', () => {
    const item = makeItem({ actualPrice: 100, estimatedPrice: 50 })
    expect(itemEffectivePrice(item)).toBe(100)
  })

  it('falls back to the chosen alternative price when no actualPrice is set', () => {
    const item = makeItem({
      estimatedPrice: 50,
      chosenAlternativeId: 'alt-1',
      alternatives: [{ id: 'alt-1', name: 'חלופה', price: 80 }],
    })
    expect(itemEffectivePrice(item)).toBe(80)
  })

  it('falls back to estimatedPrice when nothing else is set', () => {
    const item = makeItem({ estimatedPrice: 50 })
    expect(itemEffectivePrice(item)).toBe(50)
  })

  it('is undefined when no price is known', () => {
    expect(itemEffectivePrice(makeItem({}))).toBeUndefined()
  })
})

describe('calculateBudget', () => {
  it('only counts spent for bought items', () => {
    const items = [
      makeItem({ status: 'bought', actualPrice: 100 }),
      makeItem({ status: 'need', estimatedPrice: 40 }),
    ]
    const totals = calculateBudget(items)
    expect(totals.spentTotal).toBe(100)
    expect(totals.estimatedTotal).toBe(140)
    expect(totals.boughtCount).toBe(1)
    expect(totals.totalCount).toBe(2)
  })

  it('groups totals by category', () => {
    const items = [
      makeItem({ category: 'בגדים', estimatedPrice: 20 }),
      makeItem({ category: 'בגדים', estimatedPrice: 30 }),
      makeItem({ category: 'האכלה', estimatedPrice: 10 }),
    ]
    const totals = calculateBudget(items)
    const clothes = totals.byCategory.find((c) => c.category === 'בגדים')
    expect(clothes?.estimated).toBe(50)
  })

  it('handles an empty list', () => {
    const totals = calculateBudget([])
    expect(totals).toEqual({
      estimatedTotal: 0,
      spentTotal: 0,
      boughtCount: 0,
      totalCount: 0,
      byCategory: [],
    })
  })
})
