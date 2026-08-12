import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { calculateBudget, formatIls } from '@/features/shopping/budget'
import type { ShoppingItem } from '@/types/models'

export function BudgetSummaryCard({ items, compact }: { items: ShoppingItem[]; compact?: boolean }) {
  const totals = calculateBudget(items)
  const pct = totals.estimatedTotal > 0 ? Math.min(100, (totals.spentTotal / totals.estimatedTotal) * 100) : 0

  if (items.length === 0) {
    return null
  }

  return (
    <Card>
      <CardHeader className={compact ? 'pb-2' : undefined}>
        <CardTitle className="font-heading text-base">תקציב הקניות</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex items-baseline justify-between">
          <span className="text-2xl font-semibold tabular-nums text-foreground">
            {formatIls(totals.spentTotal)}
          </span>
          <span className="text-sm text-muted-foreground">
            מתוך {formatIls(totals.estimatedTotal)} משוער
          </span>
        </div>
        <Progress value={pct} className="h-2" />
        <p className="text-xs text-muted-foreground">
          {totals.boughtCount} מתוך {totals.totalCount} פריטים נקנו
        </p>

        {!compact && totals.byCategory.length > 0 && (
          <div className="space-y-2 pt-2">
            {totals.byCategory
              .sort((a, b) => b.estimated - a.estimated)
              .map((c) => (
                <div key={c.category} className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">{c.category}</span>
                  <span className="tabular-nums text-foreground">
                    {formatIls(c.spent)} / {formatIls(c.estimated)}
                  </span>
                </div>
              ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
