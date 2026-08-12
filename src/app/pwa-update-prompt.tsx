import { useRegisterSW } from 'virtual:pwa-register/react'
import { Button } from '@/components/ui/button'

export function PwaUpdatePrompt() {
  const {
    offlineReady: [offlineReady, setOfflineReady],
    needRefresh: [needRefresh, setNeedRefresh],
    updateServiceWorker,
  } = useRegisterSW()

  if (!offlineReady && !needRefresh) return null

  const dismiss = () => {
    setOfflineReady(false)
    setNeedRefresh(false)
  }

  return (
    <div
      role="status"
      className="fixed inset-x-4 z-50 mx-auto max-w-md rounded-2xl border border-border bg-card p-4 text-card-foreground shadow-lg sm:inset-x-auto sm:end-4"
      style={{ bottom: 'calc(6rem + env(safe-area-inset-bottom))' }}
    >
      <p className="text-sm">
        {needRefresh ? 'גרסה חדשה של האתר מוכנה.' : 'האתר מוכן לעבודה גם ללא אינטרנט.'}
      </p>
      <div className="mt-3 flex gap-2">
        {needRefresh && (
          <Button size="sm" onClick={() => updateServiceWorker(true)}>
            רענון עכשיו
          </Button>
        )}
        <Button size="sm" variant="ghost" onClick={dismiss}>
          סגירה
        </Button>
      </div>
    </div>
  )
}
