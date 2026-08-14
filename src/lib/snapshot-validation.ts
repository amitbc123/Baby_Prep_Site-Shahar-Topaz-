import type { AppSnapshot, ImportantDate, ShoppingItem, TaskItem } from '@/types/models'

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null
}

function isValidSettings(v: unknown): boolean {
  if (!isRecord(v)) return false
  return (
    typeof v.dueDate === 'string' &&
    (v.babyName === undefined || typeof v.babyName === 'string') &&
    Array.isArray(v.parents) &&
    v.parents.length === 2 &&
    v.parents.every((p) => typeof p === 'string') &&
    (v.theme === 'light' || v.theme === 'dark' || v.theme === 'system')
  )
}

function isValidShoppingItem(v: unknown): v is ShoppingItem {
  if (!isRecord(v)) return false
  return (
    typeof v.id === 'string' &&
    typeof v.name === 'string' &&
    typeof v.category === 'string' &&
    typeof v.priority === 'string' &&
    typeof v.status === 'string' &&
    Array.isArray(v.alternatives) &&
    typeof v.createdAt === 'number'
  )
}

function isValidTask(v: unknown): v is TaskItem {
  if (!isRecord(v)) return false
  return (
    typeof v.id === 'string' &&
    typeof v.title === 'string' &&
    typeof v.category === 'string' &&
    typeof v.priority === 'string' &&
    typeof v.done === 'boolean' &&
    typeof v.createdAt === 'number'
  )
}

function isValidImportantDate(v: unknown): v is ImportantDate {
  if (!isRecord(v)) return false
  return (
    typeof v.id === 'string' &&
    typeof v.date === 'string' &&
    typeof v.title === 'string' &&
    typeof v.createdAt === 'number'
  )
}

/** Structural check for a JSON file that claims to be an app export, before it's trusted into the store. */
export function isValidAppSnapshot(v: unknown): v is AppSnapshot {
  if (!isRecord(v)) return false
  return (
    v.version === 1 &&
    isValidSettings(v.settings) &&
    Array.isArray(v.shoppingItems) &&
    v.shoppingItems.every(isValidShoppingItem) &&
    Array.isArray(v.tasks) &&
    v.tasks.every(isValidTask) &&
    Array.isArray(v.importantDates) &&
    v.importantDates.every(isValidImportantDate)
  )
}
