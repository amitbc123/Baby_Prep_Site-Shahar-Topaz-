import type { PersistStorage, StorageValue } from 'zustand/middleware'

/**
 * Everything the app persists goes through this one adapter. Today it reads/writes
 * localStorage, so data lives only on the device that opens the site. To move to a
 * shared backend later (e.g. a small REST/WebSocket API), replace the three methods
 * below with calls to that API — no other file needs to change, since every store
 * only ever talks to `appStorage`, never to `localStorage` directly.
 */
export function createAppStorage<T>(): PersistStorage<T> {
  return {
    getItem: (name) => {
      const raw = localStorage.getItem(name)
      if (!raw) return null
      try {
        return JSON.parse(raw) as StorageValue<T>
      } catch {
        return null
      }
    },
    setItem: (name, value) => {
      localStorage.setItem(name, JSON.stringify(value))
    },
    removeItem: (name) => {
      localStorage.removeItem(name)
    },
  }
}
