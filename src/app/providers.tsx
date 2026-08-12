import { RouterProvider } from 'react-router'
import { Toaster } from '@/components/ui/sonner'
import { ErrorBoundary } from '@/app/error-boundary'
import { ThemeProvider } from '@/app/theme-provider'
import { PwaUpdatePrompt } from '@/app/pwa-update-prompt'
import { router } from '@/app/router'
import { useAppStore } from '@/stores/appStore'

export function Providers() {
  const theme = useAppStore((s) => s.settings.theme)
  const updateSettings = useAppStore((s) => s.updateSettings)

  return (
    <ErrorBoundary>
      <ThemeProvider theme={theme} onThemeChange={(next) => updateSettings({ theme: next })}>
        <RouterProvider router={router} />
        <Toaster position="top-center" />
        <PwaUpdatePrompt />
      </ThemeProvider>
    </ErrorBoundary>
  )
}
