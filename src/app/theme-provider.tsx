import { createContext, use, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { ThemeMode } from '@/types/models'

interface ThemeContextValue {
  theme: ThemeMode
  resolvedTheme: 'light' | 'dark'
  setTheme: (theme: ThemeMode) => void
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

function resolveTheme(theme: ThemeMode): 'light' | 'dark' {
  if (theme === 'system') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  return theme
}

export function ThemeProvider({
  theme,
  onThemeChange,
  children,
}: {
  theme: ThemeMode
  onThemeChange: (theme: ThemeMode) => void
  children: ReactNode
}) {
  const [resolvedTheme, setResolvedTheme] = useState(() => resolveTheme(theme))

  useEffect(() => {
    setResolvedTheme(resolveTheme(theme))
    if (theme !== 'system') return
    const mql = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = () => setResolvedTheme(resolveTheme('system'))
    mql.addEventListener('change', onChange)
    return () => mql.removeEventListener('change', onChange)
  }, [theme])

  useEffect(() => {
    document.documentElement.classList.toggle('dark', resolvedTheme === 'dark')
  }, [resolvedTheme])

  const value = useMemo(
    () => ({ theme, resolvedTheme, setTheme: onThemeChange }),
    [theme, resolvedTheme, onThemeChange],
  )

  return <ThemeContext value={value}>{children}</ThemeContext>
}

export function useTheme() {
  const ctx = use(ThemeContext)
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider')
  return ctx
}
