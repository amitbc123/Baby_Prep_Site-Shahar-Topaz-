import { PRIORITY_LABEL, type Priority } from '@/types/models'
import { cn } from '@/lib/utils'

const BADGE_CLASS: Record<Priority, string> = {
  low: 'border-border text-muted-foreground',
  normal: 'border-moss/40 bg-moss/15 text-foreground',
  high: 'border-blush/50 bg-blush/20 text-foreground',
}

const DOT_CLASS: Record<Priority, string> = {
  low: 'bg-muted-foreground/50',
  normal: 'bg-moss',
  high: 'bg-blush',
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full border px-2 py-0.5 text-xs',
        BADGE_CLASS[priority],
      )}
    >
      <span className={cn('size-1.5 rounded-full', DOT_CLASS[priority])} aria-hidden />
      {PRIORITY_LABEL[priority]}
    </span>
  )
}
