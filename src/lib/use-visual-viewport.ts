import { useEffect } from 'react'

/**
 * iOS Safari keeps `position: fixed` elements anchored to the layout viewport,
 * not the visual one — so a bottom sheet stays pinned behind the keyboard
 * instead of shrinking above it. This tracks `visualViewport.height` into a
 * CSS var that fixed bottom sheets can clamp their max-height against.
 * (Chromium honors `interactive-widget=resizes-content` in index.html instead,
 * so this is mainly the iOS fallback.)
 */
export function useVisualViewportHeight() {
  useEffect(() => {
    const vv = window.visualViewport
    if (!vv) return

    function update() {
      document.documentElement.style.setProperty('--visual-vh', `${vv!.height}px`)
    }

    update()
    vv.addEventListener('resize', update)
    vv.addEventListener('scroll', update)
    return () => {
      vv.removeEventListener('resize', update)
      vv.removeEventListener('scroll', update)
    }
  }, [])
}
