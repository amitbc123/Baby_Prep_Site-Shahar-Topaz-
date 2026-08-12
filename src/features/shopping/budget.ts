import type { ShoppingCategory, ShoppingItem } from '@/types/models'

export interface BudgetTotals {
  estimatedTotal: number
  spentTotal: number
  boughtCount: number
  totalCount: number
  byCategory: { category: ShoppingCategory; estimated: number; spent: number }[]
}

export function itemEffectivePrice(item: ShoppingItem): number | undefined {
  if (item.actualPrice != null) return item.actualPrice
  if (item.chosenAlternativeId) {
    const chosen = item.alternatives.find((a) => a.id === item.chosenAlternativeId)
    if (chosen?.price != null) return chosen.price
  }
  return item.estimatedPrice
}

export function calculateBudget(items: ShoppingItem[]): BudgetTotals {
  const byCategoryMap = new Map<ShoppingCategory, { estimated: number; spent: number }>()
  let estimatedTotal = 0
  let spentTotal = 0
  let boughtCount = 0

  for (const item of items) {
    const est = item.estimatedPrice ?? itemEffectivePrice(item) ?? 0
    const spent = item.status === 'bought' ? (itemEffectivePrice(item) ?? 0) : 0
    estimatedTotal += est
    spentTotal += spent
    if (item.status === 'bought') boughtCount += 1

    const bucket = byCategoryMap.get(item.category) ?? { estimated: 0, spent: 0 }
    bucket.estimated += est
    bucket.spent += spent
    byCategoryMap.set(item.category, bucket)
  }

  return {
    estimatedTotal,
    spentTotal,
    boughtCount,
    totalCount: items.length,
    byCategory: [...byCategoryMap.entries()].map(([category, v]) => ({ category, ...v })),
  }
}

export function formatIls(amount: number): string {
  return new Intl.NumberFormat('he-IL', { style: 'currency', currency: 'ILS', maximumFractionDigits: 0 }).format(
    amount,
  )
}
