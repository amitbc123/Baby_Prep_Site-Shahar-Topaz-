import { CalendarHeart, Home, ListChecks, Settings, ShoppingBag } from 'lucide-react'

export const NAV_ITEMS = [
  { to: '/', label: 'בית', icon: Home },
  { to: '/shopping', label: 'קניות', icon: ShoppingBag },
  { to: '/tasks', label: 'משימות', icon: ListChecks },
  { to: '/dates', label: 'תאריכים', icon: CalendarHeart },
  { to: '/settings', label: 'הגדרות', icon: Settings },
] as const
