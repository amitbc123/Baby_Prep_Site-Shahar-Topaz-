import { useId, useMemo } from 'react'
import type { WeeklyFruit } from '@/lib/pregnancy'

interface MoonCountdownProps {
  moonFraction: number
  week: number
  daysLeft: number
  hasArrived: boolean
  babyName?: string
  fruit?: WeeklyFruit
}

const STARS = [
  { x: 20, y: 24, r: 1.4 },
  { x: 168, y: 40, r: 1.1 },
  { x: 34, y: 150, r: 1 },
  { x: 180, y: 130, r: 1.3 },
  { x: 150, y: 20, r: 0.9 },
  { x: 14, y: 90, r: 0.8 },
  { x: 186, y: 90, r: 1 },
]

export function MoonCountdown({
  moonFraction,
  week,
  daysLeft,
  hasArrived,
  babyName,
  fruit,
}: MoonCountdownProps) {
  const clipId = useId()
  const glowId = useId()
  const fillY = useMemo(() => 200 - moonFraction * 200, [moonFraction])

  return (
    <div className="overflow-hidden rounded-3xl bg-[#232A3D] px-6 py-8 text-center shadow-lg">
      <svg viewBox="0 0 200 200" className="mx-auto h-40 w-40" role="img" aria-label={`${moonFraction * 100} אחוז מהדרך אל הירח המלא`}>
        <defs>
          <clipPath id={clipId}>
            <circle cx="100" cy="100" r="76" />
          </clipPath>
          <radialGradient id={glowId} cx="50%" cy="42%" r="65%">
            <stop offset="0%" stopColor="#FBD8AC" />
            <stop offset="55%" stopColor="#E8A268" />
            <stop offset="100%" stopColor="#C9793F" />
          </radialGradient>
        </defs>

        {STARS.map((s, i) => (
          <circle key={i} cx={s.x} cy={s.y} r={s.r} fill="#F7F1E6" opacity={0.55} />
        ))}

        <circle cx="100" cy="100" r="76" fill="#2B3348" stroke="#3A4360" strokeWidth="1" />

        <g clipPath={`url(#${clipId})`}>
          <rect x="0" y={fillY} width="200" height="200" fill={`url(#${glowId})`} className="transition-[y] duration-700 ease-out" />
        </g>

        <circle cx="100" cy="100" r="76" fill="none" stroke="#4A5372" strokeWidth="1" />
      </svg>

      <p className="mt-4 font-heading text-lg text-[#F7F1E6]">
        {hasArrived
          ? babyName
            ? `${babyName} כאן!`
            : 'היא כאן!'
          : `שבוע ${week} מתוך 40`}
      </p>
      <p className="mt-1 text-sm text-[#C9C2AE]">
        {hasArrived
          ? 'הירח מלא. מזל טוב!'
          : daysLeft === 1
            ? 'עוד יום אחד'
            : `עוד ${daysLeft} ימים עד התאריך המשוער`}
      </p>

      {!hasArrived && fruit && (
        <p className="mt-3 inline-flex items-center gap-1.5 rounded-full bg-white/5 px-3 py-1 text-xs text-[#F7F1E6]">
          <span className="text-base leading-none">{fruit.emoji}</span>
          היא בערך בגודל של {fruit.name}
        </p>
      )}
    </div>
  )
}
