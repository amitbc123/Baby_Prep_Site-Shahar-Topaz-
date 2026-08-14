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
import { Trash2, Plus } from 'lucide-react'
import {
  SHOPPING_CATEGORIES,
  SHOPPING_STATUS_LABEL,
  PRIORITY_LABEL,
  type ShoppingAlternative,
  type ShoppingItem,
  type ShoppingStatus,
  type Priority,
  type Assignee,
} from '@/types/models'

type FormValue = Omit<ShoppingItem, 'id' | 'createdAt' | 'alternatives'> & {
  alternatives: ShoppingAlternative[]
}

function emptyForm(): FormValue {
  return {
    name: '',
    category: 'תינוקייה',
    priority: 'normal',
    status: 'need',
    estimatedPrice: undefined,
    actualPrice: undefined,
    assignee: undefined,
    note: '',
    link: '',
    alternatives: [],
    chosenAlternativeId: undefined,
  }
}

export function ShoppingItemForm({
  open,
  onOpenChange,
  initial,
  onSubmit,
  onDelete,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  initial?: ShoppingItem
  onSubmit: (value: FormValue) => void
  onDelete?: () => void
}) {
  const [form, setForm] = useState<FormValue>(emptyForm())

  useEffect(() => {
    if (open) {
      setForm(initial ? { ...initial } : emptyForm())
    }
  }, [open, initial])

  function addAlternative() {
    setForm((f) => ({
      ...f,
      alternatives: [...f.alternatives, { id: crypto.randomUUID(), name: '', price: undefined }],
    }))
  }

  function updateAlternative(id: string, patch: Partial<ShoppingAlternative>) {
    setForm((f) => ({
      ...f,
      alternatives: f.alternatives.map((a) => (a.id === id ? { ...a, ...patch } : a)),
    }))
  }

  function removeAlternative(id: string) {
    setForm((f) => ({
      ...f,
      alternatives: f.alternatives.filter((a) => a.id !== id),
      chosenAlternativeId: f.chosenAlternativeId === id ? undefined : f.chosenAlternativeId,
    }))
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.name.trim()) return
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
            <SheetTitle className="font-heading">{initial ? 'עריכת פריט' : 'פריט חדש'}</SheetTitle>
          </SheetHeader>

          <div className="space-y-1.5">
            <Label htmlFor="item-name">שם הפריט</Label>
            <Input
              id="item-name"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              placeholder='למשל: עגלת תינוק'
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label>קטגוריה</Label>
              <Select
                value={form.category}
                onValueChange={(v) => setForm((f) => ({ ...f, category: v as ShoppingItem['category'] }))}
              >
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SHOPPING_CATEGORIES.map((c) => (
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
              <Label>סטטוס</Label>
              <Select
                value={form.status}
                onValueChange={(v) => setForm((f) => ({ ...f, status: v as ShoppingStatus }))}
              >
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {(Object.keys(SHOPPING_STATUS_LABEL) as ShoppingStatus[]).map((s) => (
                    <SelectItem key={s} value={s}>
                      {SHOPPING_STATUS_LABEL[s]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
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

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="est-price">מחיר משוער (₪)</Label>
              <Input
                id="est-price"
                type="number"
                inputMode="decimal"
                value={form.estimatedPrice ?? ''}
                onChange={(e) =>
                  setForm((f) => ({
                    ...f,
                    estimatedPrice: e.target.value === '' ? undefined : Number(e.target.value),
                  }))
                }
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="actual-price">מחיר בפועל (₪)</Label>
              <Input
                id="actual-price"
                type="number"
                inputMode="decimal"
                value={form.actualPrice ?? ''}
                onChange={(e) =>
                  setForm((f) => ({
                    ...f,
                    actualPrice: e.target.value === '' ? undefined : Number(e.target.value),
                  }))
                }
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="link">קישור (אופציונלי)</Label>
            <Input
              id="link"
              type="url"
              inputMode="url"
              placeholder="https://"
              value={form.link ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, link: e.target.value }))}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="note">הערה</Label>
            <Textarea
              id="note"
              value={form.note ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, note: e.target.value }))}
              rows={2}
            />
          </div>

          <div className="space-y-2 rounded-2xl border border-border p-3">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium text-foreground">מתלבטים בין כמה אפשרויות?</p>
              <Button type="button" variant="ghost" size="sm" onClick={addAlternative}>
                <Plus className="size-4" />
                הוספת אפשרות
              </Button>
            </div>
            {form.alternatives.length > 0 && (
              <div className="space-y-3">
                {form.alternatives.map((alt) => (
                  <div key={alt.id} className="flex items-start gap-2 rounded-xl bg-muted/50 p-2">
                    <input
                      type="radio"
                      name="chosenAlternative"
                      className="mt-3 size-4 accent-primary"
                      checked={form.chosenAlternativeId === alt.id}
                      onChange={() => setForm((f) => ({ ...f, chosenAlternativeId: alt.id }))}
                      aria-label={`בחירה ב-${alt.name || 'אפשרות'}`}
                    />
                    <div className="grid flex-1 grid-cols-2 gap-2">
                      <Input
                        placeholder="שם האפשרות"
                        className="col-span-2"
                        value={alt.name}
                        onChange={(e) => updateAlternative(alt.id, { name: e.target.value })}
                      />
                      <Input
                        placeholder="מחיר (₪)"
                        type="number"
                        inputMode="decimal"
                        value={alt.price ?? ''}
                        onChange={(e) =>
                          updateAlternative(alt.id, {
                            price: e.target.value === '' ? undefined : Number(e.target.value),
                          })
                        }
                      />
                      <Input
                        placeholder="קישור"
                        type="url"
                        value={alt.link ?? ''}
                        onChange={(e) => updateAlternative(alt.id, { link: e.target.value })}
                      />
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => removeAlternative(alt.id)}
                      aria-label="הסרת אפשרות"
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
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
