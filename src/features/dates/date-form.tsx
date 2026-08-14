import { useEffect, useState } from 'react'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetFooter } from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type { ImportantDate } from '@/types/models'

type FormValue = Omit<ImportantDate, 'id' | 'createdAt'>

function emptyForm(): FormValue {
  return { date: new Date().toISOString().slice(0, 10), title: '', wish: '' }
}

export function DateForm({
  open,
  onOpenChange,
  initial,
  onSubmit,
  onDelete,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  initial?: ImportantDate
  onSubmit: (value: FormValue) => void
  onDelete?: () => void
}) {
  const [form, setForm] = useState<FormValue>(emptyForm())

  useEffect(() => {
    if (open) setForm(initial ? { ...initial } : emptyForm())
  }, [open, initial])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.title.trim() || !form.date) return
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
            <SheetTitle className="font-heading">{initial ? 'עריכת תאריך' : 'תאריך חדש'}</SheetTitle>
          </SheetHeader>

          <div className="space-y-1.5">
            <Label htmlFor="date-title">כותרת</Label>
            <Input
              id="date-title"
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              placeholder="למשל: מסיבת קבלת פנים"
              required
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="date-value">תאריך</Label>
            <Input
              id="date-value"
              type="date"
              value={form.date}
              onChange={(e) => setForm((f) => ({ ...f, date: e.target.value }))}
              required
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="date-wish">משאלה או הערה אישית</Label>
            <Textarea
              id="date-wish"
              value={form.wish ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, wish: e.target.value }))}
              rows={3}
              placeholder="משהו שתרצו לזכור או לאחל לעצמכם / לה"
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
