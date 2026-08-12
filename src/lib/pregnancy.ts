const DAY_MS = 1000 * 60 * 60 * 24
const FULL_TERM_WEEKS = 40

function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

export function daysUntil(dueDateIso: string, from = new Date()): number {
  const due = startOfDay(new Date(dueDateIso))
  const today = startOfDay(from)
  return Math.round((due.getTime() - today.getTime()) / DAY_MS)
}

export interface PregnancyProgress {
  daysLeft: number
  week: number
  hasArrived: boolean
  /** 0 (new crescent) .. 1 (full moon at due date) */
  moonFraction: number
}

export function getPregnancyProgress(dueDateIso: string, from = new Date()): PregnancyProgress {
  const daysLeft = daysUntil(dueDateIso, from)
  const totalDays = FULL_TERM_WEEKS * 7
  const daysElapsed = totalDays - daysLeft
  const week = Math.min(FULL_TERM_WEEKS, Math.max(1, Math.floor(daysElapsed / 7) + 1))
  const moonFraction = Math.min(1, Math.max(0, daysElapsed / totalDays))
  return { daysLeft, week, hasArrived: daysLeft <= 0, moonFraction }
}

const HEBREW_MONTHS = [
  'בינואר',
  'בפברואר',
  'במרץ',
  'באפריל',
  'במאי',
  'ביוני',
  'ביולי',
  'באוגוסט',
  'בספטמבר',
  'באוקטובר',
  'בנובמבר',
  'בדצמבר',
]

export function formatHebrewDate(iso: string): string {
  const d = new Date(iso)
  return `${d.getDate()} ${HEBREW_MONTHS[d.getMonth()]} ${d.getFullYear()}`
}

export function isPastDate(iso: string, from = new Date()): boolean {
  return startOfDay(new Date(iso)).getTime() < startOfDay(from).getTime()
}

export const WEEKLY_INFO: Record<number, string> = {
  33: 'התינוקת מתחילה לתרגל נשימות. זה זמן טוב להתחיל לארוז את תיק ליולדת.',
  34: 'הריאות ממשיכות להתפתח והתינוקת כנראה כבר במנח הראש למטה. כדאי לוודא שמושב הבטיחות לרכב מוכן.',
  35: 'משקל הלידה כמעט נקבע מעכשיו. זמן טוב לגמור את הפריטים הדחופים ברשימת הקניות.',
  36: 'נחשבת כמעט "בטווח מלא". כדאי לסגור את פרטי הדרך לבית החולים ולוודא שהמסמכים החשובים מוכנים ונגישים.',
  37: 'התינוקת נחשבת רשמית "בטווח מלא" (Full Term)! היא יכולה להגיע בכל רגע — כדאי שתיק ליולדת יהיה ליד הדלת.',
  38: 'רוב האיברים בשלים לגמרי. זמן טוב לסיים סידורים אחרונים בבית ולנוח כמה שאפשר.',
  39: 'התינוקת ממשיכה לצבור משקל ומוכנה להגעה בכל יום. שווה לוודא שיש מספרי טלפון חשובים נגישים לשניכם.',
  40: 'תאריך הלידה המשוער הגיע. תזכרו: רוב התינוקות לא נולדים בדיוק בתאריך המשוער, וזה לגמרי תקין.',
}

export function getWeeklyInfo(week: number): string | undefined {
  return WEEKLY_INFO[week]
}

export interface WeeklyFruit {
  name: string
  emoji: string
}

export const WEEKLY_FRUIT: Record<number, WeeklyFruit> = {
  33: { name: 'אננס', emoji: '🍍' },
  34: { name: 'מלון חמד', emoji: '🍈' },
  35: { name: 'מלון דבש', emoji: '🍈' },
  36: { name: 'ראש חסה', emoji: '🥬' },
  37: { name: 'כרישה', emoji: '🥬' },
  38: { name: 'אבטיח מיני', emoji: '🍉' },
  39: { name: 'אבטיח קטן', emoji: '🍉' },
  40: { name: 'דלעת קטנה', emoji: '🎃' },
}

export function getWeeklyFruit(week: number): WeeklyFruit | undefined {
  return WEEKLY_FRUIT[week]
}
