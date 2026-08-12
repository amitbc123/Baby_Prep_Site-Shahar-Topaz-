import { PRIORITY_LABEL, type Priority } from '@/types/models'
import { cn } from '@/lib/utils'

const DOT_CLASS: Record<Priority, string> = {
  low: 'bg-muted-foreground/50',
  normal: 'bg-moss',
  high: 'bg-destructive',
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-border px-2 py-0.5 text-xs text-muted-foreground">
      <span className={cn('size-1.5 rounded-full', DOT_CLASS[priority])} aria-hidden />
      {PRIORITY_LABEL[priority]}
    </span>
  )
}
