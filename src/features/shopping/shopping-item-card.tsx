import { Card, CardContent } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { PriorityBadge } from '@/components/shared/priority-badge'
import { formatIls, itemEffectivePrice } from '@/features/shopping/budget'
import { SHOPPING_STATUS_LABEL, type ShoppingItem } from '@/types/models'
import { cn } from '@/lib/utils'

export function ShoppingItemCard({
  item,
  onToggleBought,
  onClick,
}: {
  item: ShoppingItem
  onToggleBought: (bought: boolean) => void
  onClick: () => void
}) {
  const price = itemEffectivePrice(item)
  const bought = item.status === 'bought'

  return (
    <Card
      className={cn(
        'cursor-pointer border-s-4 transition-colors',
        item.priority === 'high' && 'border-s-blush',
        item.priority === 'normal' && 'border-s-moss',
        item.priority === 'low' && 'border-s-border',
        bought && 'bg-muted/40',
      )}
      onClick={onClick}
    >
      <CardContent className="flex items-start gap-3 py-3">
        <Checkbox
          checked={bought}
          onCheckedChange={(v) => onToggleBought(v === true)}
          onClick={(e) => e.stopPropagation()}
          className="mt-1"
          aria-label={`סימון ${item.name} כנקנה`}
        />
        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between gap-2">
            <p className={cn('truncate font-medium text-foreground', bought && 'line-through opacity-60')}>
              {item.name}
            </p>
            {price != null && (
              <span className="shrink-0 tabular-nums text-sm text-foreground">{formatIls(price)}</span>
            )}
          </div>
          <div className="mt-1.5 flex flex-wrap items-center gap-1.5">
            <span className="text-xs text-muted-foreground">{item.category}</span>
            <PriorityBadge priority={item.priority} />
            {!bought && (
              <span className="text-xs text-muted-foreground">· {SHOPPING_STATUS_LABEL[item.status]}</span>
            )}
            {item.assignee && <span className="text-xs text-muted-foreground">· {item.assignee}</span>}
            {item.alternatives.length > 0 && (
              <span className="text-xs text-primary">
                · {item.alternatives.length} אפשרויות בבדיקה
              </span>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
