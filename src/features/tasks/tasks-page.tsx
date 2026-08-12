import { useMemo, useState } from 'react'
import { Plus, Sparkles } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useAppStore } from '@/stores/appStore'
import { TaskCard } from '@/features/tasks/task-card'
import { TaskForm } from '@/features/tasks/task-form'
import { HOSPITAL_BAG_PRESET } from '@/lib/hospital-bag-preset'
import { TASK_CATEGORIES, type TaskCategory, type TaskItem } from '@/types/models'

type FilterValue = 'all' | TaskCategory

export default function TasksPage() {
  const tasks = useAppStore((s) => s.tasks)
  const addTask = useAppStore((s) => s.addTask)
  const updateTask = useAppStore((s) => s.updateTask)
  const removeTask = useAppStore((s) => s.removeTask)

  const [filter, setFilter] = useState<FilterValue>('all')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<TaskItem | undefined>(undefined)

  const filtered = useMemo(
    () => (filter === 'all' ? tasks : tasks.filter((t) => t.category === filter)),
    [tasks, filter],
  )

  const sorted = useMemo(
    () => [...filtered].sort((a, b) => Number(a.done) - Number(b.done) || a.createdAt - b.createdAt),
    [filtered],
  )

  const hospitalBagAlreadyAdded = tasks
    .filter((t) => t.category === 'תיק ליולדת')
    .map((t) => t.title)

  function openNew() {
    setEditing(undefined)
    setFormOpen(true)
  }

  function openEdit(task: TaskItem) {
    setEditing(task)
    setFormOpen(true)
  }

  function addHospitalBagPreset() {
    for (const title of HOSPITAL_BAG_PRESET) {
      if (!hospitalBagAlreadyAdded.includes(title)) {
        addTask({
          title,
          category: 'תיק ליולדת',
          priority: 'normal',
          done: false,
        })
      }
    }
    setFilter('תיק ליולדת')
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-2xl text-foreground">משימות</h1>
        <Button onClick={openNew} size="sm">
          <Plus className="size-4" />
          משימה חדשה
        </Button>
      </div>

      <Tabs value={filter} onValueChange={(v) => setFilter(v as FilterValue)}>
        <TabsList className="flex h-auto w-full flex-wrap justify-start gap-1 bg-transparent p-0">
          <TabsTrigger value="all" className="rounded-full border border-border data-[state=active]:border-primary">
            הכל
          </TabsTrigger>
          {TASK_CATEGORIES.map((c) => (
            <TabsTrigger
              key={c}
              value={c}
              className="rounded-full border border-border data-[state=active]:border-primary"
            >
              {c}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>

      {filter === 'תיק ליולדת' && (
        <button
          type="button"
          onClick={addHospitalBagPreset}
          className="flex w-full items-center justify-center gap-2 rounded-2xl border border-dashed border-primary/50 bg-primary/5 py-3 text-sm text-primary"
        >
          <Sparkles className="size-4" />
          הוספת רשימת פריטים מומלצת לתיק ליולדת
        </button>
      )}

      {sorted.length === 0 ? (
        <p className="py-10 text-center text-sm text-muted-foreground">
          {tasks.length === 0 ? 'עוד לא הוספתם משימות. אפשר להתחיל!' : 'אין משימות בסינון הזה.'}
        </p>
      ) : (
        <div className="space-y-2">
          {sorted.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onToggleDone={(done) => updateTask(task.id, { done })}
              onClick={() => openEdit(task)}
            />
          ))}
        </div>
      )}

      <Button
        onClick={openNew}
        size="icon"
        className="fixed end-4 z-40 size-14 rounded-full shadow-lg"
        style={{ bottom: 'calc(5.5rem + env(safe-area-inset-bottom))' }}
        aria-label="הוספת משימה"
      >
        <Plus className="size-6" />
      </Button>

      <TaskForm
        open={formOpen}
        onOpenChange={setFormOpen}
        initial={editing}
        defaultCategory={filter !== 'all' ? filter : undefined}
        onSubmit={(value) => {
          if (editing) {
            updateTask(editing.id, value)
          } else {
            addTask(value)
          }
        }}
        onDelete={
          editing
            ? () => {
                removeTask(editing.id)
                setFormOpen(false)
              }
            : undefined
        }
      />
    </div>
  )
}
