import { useId, useMemo } from 'react'
import type { WeeklyAnimal, WeeklyFruit } from '@/lib/pregnancy'

interface MoonCountdownProps {
  moonFraction: number
  week: number
  dayOfWeek: number
  daysLeft: number
  hasArrived: boolean
  babyName?: string
  dateLabel: string
  fruit?: WeeklyFruit
  animal?: WeeklyAnimal
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
  dayOfWeek,
  daysLeft,
  hasArrived,
  babyName,
  dateLabel,
  fruit,
  animal,
}: MoonCountdownProps) {
  const clipId = useId()
  const glowId = useId()
  const fillY = useMemo(() => 200 - moonFraction * 200, [moonFraction])

  return (
    <div className="overflow-hidden rounded-3xl bg-[#292540] px-6 py-8 text-center shadow-lg">
      <p className="text-xs text-[#C9C2AE]">{dateLabel}</p>

      <svg viewBox="0 0 200 200" className="mx-auto mt-3 h-40 w-40" role="img" aria-label={`${moonFraction * 100} אחוז מהדרך אל הירח המלא`}>
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
          <circle key={i} cx={s.x} cy={s.y} r={s.r} fill="#F3ECDD" opacity={0.55} />
        ))}

        <circle cx="100" cy="100" r="76" fill="#332E4A" stroke="#463F5E" strokeWidth="1" />

        <g clipPath={`url(#${clipId})`}>
          <rect x="0" y={fillY} width="200" height="200" fill={`url(#${glowId})`} className="transition-[y] duration-700 ease-out" />
        </g>

        <circle cx="100" cy="100" r="76" fill="none" stroke="#544C6E" strokeWidth="1" />
      </svg>

      <p className="mt-4 font-heading text-lg text-[#F3ECDD]">
        {hasArrived
          ? babyName
            ? `${babyName} כאן!`
            : 'היא כאן!'
          : `שבוע ${week}, יום ${dayOfWeek} מתוך 40`}
      </p>
      <p className="mt-1 text-sm text-[#C9C2AE]">
        {hasArrived
          ? 'הירח מלא. מזל טוב!'
          : daysLeft === 1
            ? 'עוד יום אחד'
            : `עוד ${daysLeft} ימים עד התאריך המשוער`}
      </p>

      {!hasArrived && (fruit || animal) && (
        <p className="mt-3 inline-flex flex-wrap items-center justify-center gap-x-1.5 gap-y-1 rounded-full bg-white/5 px-3 py-1.5 text-xs text-[#F3ECDD]">
          {fruit && (
            <span className="inline-flex items-center gap-1">
              <span className="text-base leading-none">{fruit.emoji}</span>
              בגודל {fruit.name}
            </span>
          )}
          {fruit && animal && <span className="text-[#C9C2AE]">•</span>}
          {animal && (
            <span className="inline-flex items-center gap-1">
              <span className="text-base leading-none">{animal.emoji}</span>
              כמו {animal.name}
            </span>
          )}
        </p>
      )}
    </div>
  )
}
