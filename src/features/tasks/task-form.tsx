import { useEffect, useState } from 'react'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetFooter } from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { TASK_CATEGORIES, PRIORITY_LABEL, type Priority, type TaskItem, type Assignee } from '@/types/models'

type FormValue = Omit<TaskItem, 'id' | 'createdAt'>

function emptyForm(): FormValue {
  return {
    title: '',
    category: 'הכנת הבית',
    dueDate: undefined,
    priority: 'normal',
    assignee: undefined,
    done: false,
    note: '',
  }
}

export function TaskForm({
  open,
  onOpenChange,
  initial,
  defaultCategory,
  onSubmit,
  onDelete,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  initial?: TaskItem
  defaultCategory?: FormValue['category']
  onSubmit: (value: FormValue) => void
  onDelete?: () => void
}) {
  const [form, setForm] = useState<FormValue>(emptyForm())

  useEffect(() => {
    if (open) {
      setForm(initial ? { ...initial } : { ...emptyForm(), category: defaultCategory ?? 'הכנת הבית' })
    }
  }, [open, initial, defaultCategory])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.title.trim()) return
    onSubmit(form)
    onOpenChange(false)
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="bottom"
        className="max-h-[min(92dvh,calc(var(--visual-vh,100dvh)*0.92))] overflow-y-auto rounded-t-3xl"
      >
        <form onSubmit={handleSubmit} className="mx-auto flex w-full max-w-lg flex-col gap-4 pb-4">
          <SheetHeader className="px-0">
            <SheetTitle className="font-heading">{initial ? 'עריכת משימה' : 'משימה חדשה'}</SheetTitle>
          </SheetHeader>

          <div className="space-y-1.5">
            <Label htmlFor="task-title">כותרת</Label>
            <Input
              id="task-title"
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              placeholder="למשל: להירשם לקורס הכנה ללידה"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label>קטגוריה</Label>
              <Select
                value={form.category}
                onValueChange={(v) => setForm((f) => ({ ...f, category: v as FormValue['category'] }))}
              >
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {TASK_CATEGORIES.map((c) => (
                    <SelectItem key={c} value={c}>
                      {c}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>עדיפות</Label>
              <Select
                value={form.priority}
                onValueChange={(v) => setForm((f) => ({ ...f, priority: v as Priority }))}
              >
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {(Object.keys(PRIORITY_LABEL) as Priority[]).map((p) => (
                    <SelectItem key={p} value={p}>
                      {PRIORITY_LABEL[p]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="due-date">תאריך יעד</Label>
              <Input
                id="due-date"
                type="date"
                value={form.dueDate ?? ''}
                onChange={(e) => setForm((f) => ({ ...f, dueDate: e.target.value || undefined }))}
              />
            </div>
            <div className="space-y-1.5">
              <Label>אחראי/ת</Label>
              <Select
                value={form.assignee ?? 'none'}
                onValueChange={(v) =>
                  setForm((f) => ({ ...f, assignee: v === 'none' ? undefined : (v as Assignee) }))
                }
              >
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">לא נבחר</SelectItem>
                  <SelectItem value="שחר">שחר</SelectItem>
                  <SelectItem value="טופז">טופז</SelectItem>
                  <SelectItem value="שניהם">שניהם</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="task-note">הערה</Label>
            <Textarea
              id="task-note"
              value={form.note ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, note: e.target.value }))}
              rows={2}
            />
          </div>

          <SheetFooter className="flex-row gap-2 px-0">
            {onDelete && (
              <Button type="button" variant="outline" className="flex-1" onClick={onDelete}>
                מחיקה
              </Button>
            )}
            <Button type="submit" className="flex-1">
              שמירה
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  )
}
