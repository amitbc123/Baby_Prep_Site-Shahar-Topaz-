import { Link } from 'react-router'
import { MoonCountdown } from '@/components/countdown/moon-countdown'
import { Card, CardContent } from '@/components/ui/card'
import { BudgetSummaryCard } from '@/features/shopping/budget-summary-card'
import { useAppStore } from '@/stores/appStore'
import {
  getPregnancyProgress,
  getWeeklyInfo,
  getWeeklyFruit,
  getWeeklyAnimal,
  formatHebrewDateWithWeekday,
} from '@/lib/pregnancy'
import { getDailyMessage } from '@/lib/messages'

export default function HomePage() {
  const settings = useAppStore((s) => s.settings)
  const shoppingItems = useAppStore((s) => s.shoppingItems)
  const tasks = useAppStore((s) => s.tasks)

  const progress = getPregnancyProgress(settings.dueDate)
  const weeklyInfo = getWeeklyInfo(progress.week)
  const weeklyFruit = getWeeklyFruit(progress.week)
  const weeklyAnimal = getWeeklyAnimal(progress.week)
  const openTasks = tasks.filter((t) => !t.done).length

  return (
    <div className="space-y-4">
      <MoonCountdown
        moonFraction={progress.moonFraction}
        week={progress.week}
        dayOfWeek={progress.dayOfWeek}
        daysLeft={progress.daysLeft}
        hasArrived={progress.hasArrived}
        babyName={settings.babyName}
        dateLabel={formatHebrewDateWithWeekday()}
        fruit={weeklyFruit}
        animal={weeklyAnimal}
      />

      <p className="text-center text-sm text-muted-foreground">{getDailyMessage()}</p>

      {weeklyInfo && (
        <Card>
          <CardContent className="pt-6">
            <p className="text-xs font-medium text-primary">מה קורה השבוע</p>
            <p className="mt-1 text-sm leading-relaxed text-foreground">{weeklyInfo}</p>
          </CardContent>
        </Card>
      )}

      <BudgetSummaryCard items={shoppingItems} compact />

      {openTasks > 0 && (
        <Link to="/tasks">
          <Card className="transition-colors hover:bg-muted/60">
            <CardContent className="flex items-center justify-between pt-6">
              <span className="text-sm text-foreground">
                {openTasks} משימות פתוחות
              </span>
              <span className="text-sm text-primary">למעבר לרשימה ←</span>
            </CardContent>
          </Card>
        </Link>
      )}

      <p className="pb-2 text-center text-xs text-muted-foreground">
        <Link to="/settings" className="underline underline-offset-2">
          לעריכת התאריך המשוער והשם
        </Link>
      </p>
    </div>
  )
}
