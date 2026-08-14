const DAY_MS = 1000 * 60 * 60 * 24
const FULL_TERM_WEEKS = 40

function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

/** Parses a `YYYY-MM-DD` string as a local-timezone date, not UTC midnight. */
function parseIsoDateLocal(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, (m ?? 1) - 1, d ?? 1)
}

export function daysUntil(dueDateIso: string, from = new Date()): number {
  const due = startOfDay(parseIsoDateLocal(dueDateIso))
  const today = startOfDay(from)
  return Math.round((due.getTime() - today.getTime()) / DAY_MS)
}

export interface PregnancyProgress {
  daysLeft: number
  week: number
  /** 1..7, the day within the current pregnancy week */
  dayOfWeek: number
  hasArrived: boolean
  /** 0 (new crescent) .. 1 (full moon at due date) */
  moonFraction: number
}

export function getPregnancyProgress(dueDateIso: string, from = new Date()): PregnancyProgress {
  const daysLeft = daysUntil(dueDateIso, from)
  const totalDays = FULL_TERM_WEEKS * 7
  const daysElapsed = totalDays - daysLeft
  const clampedElapsed = Math.min(totalDays - 1, Math.max(0, daysElapsed))
  const week = Math.min(FULL_TERM_WEEKS, Math.max(1, Math.floor(daysElapsed / 7) + 1))
  const dayOfWeek = (clampedElapsed % 7) + 1
  const moonFraction = Math.min(1, Math.max(0, daysElapsed / totalDays))
  return { daysLeft, week, dayOfWeek, hasArrived: daysLeft <= 0, moonFraction }
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
  const d = parseIsoDateLocal(iso)
  return `${d.getDate()} ${HEBREW_MONTHS[d.getMonth()]} ${d.getFullYear()}`
}

const HEBREW_WEEKDAYS = ['ראשון', 'שני', 'שלישי', 'רביעי', 'חמישי', 'שישי', 'שבת']

/** Full "יום ראשון, 4 בינואר 2026" style label for a given date (defaults to today). */
export function formatHebrewDateWithWeekday(d = new Date()): string {
  const weekday = HEBREW_WEEKDAYS[d.getDay()]
  return `יום ${weekday}, ${d.getDate()} ${HEBREW_MONTHS[d.getMonth()]} ${d.getFullYear()}`
}

export function isPastDate(iso: string, from = new Date()): boolean {
  return startOfDay(parseIsoDateLocal(iso)).getTime() < startOfDay(from).getTime()
}

export const WEEKLY_INFO: Record<number, string> = {
  1: 'ההריון נספר משבוע 1 מהיום הראשון של המחזור האחרון, לפני הביוץ וההפריה בפועל. עדיין מוקדם מדי בשביל תסמינים — זה בעיקר זמן להתחיל לעקוב.',
  2: 'הביוץ מתרחש בסביבות השבוע הזה, ובמידה ויש הפריה — היא קורית ממש עכשיו, ברוב המקרים בחצוצרה.',
  3: 'הביצית המופרית מתחלקת שוב ושוב בדרכה אל הרחם, ותוך ימים אחדים תשתרש ברירית הרחם. זו עדיין ההתחלה ממש.',
  4: 'ההשרשה ברירית הרחם הושלמה, וגוף האישה מתחיל להפריש את הורמון ה-hCG שעליו מתבססות בדיקות ההריון הביתיות.',
  5: 'הצינור העצבי, שממנו יתפתחו המוח וחוט השדרה, מתחיל להיווצר. הלב העוברי מתחיל לפעום בקצב לא סדיר.',
  6: 'הלב פועם בקצב סדיר יותר וניתן לעיתים לזהות אותו באולטרסאונד. מתחילים להסתמן ראשי הפרקים של הידיים והרגליים.',
  7: 'המוח מתפתח במהירות גבוהה, ומתחילות להיווצר תכונות פנים ראשוניות — כולל נקודות שיהפכו לעיניים ולנחיריים.',
  8: 'כל האיברים הפנימיים החיוניים כבר קיימים בצורתם הבסיסית וממשיכים להתפתח. האצבעות מתחילות להיפרד זו מזו.',
  9: 'העובר מתחיל לזוז, גם אם עדיין לא מורגש. השרירים מתחילים להתפתח והגוף מתיישר בהדרגה מתנוחה מכופפת.',
  10: 'תקופת העובר מתחילה רשמית מהשבוע הזה — האיברים הבסיסיים קיימים וממשיכים לגדול ולהבשיל. הציפורניים מתחילות להיווצר.',
  11: 'הגוף גדל מהר יותר מהראש, שעד כה היה גדול יחסית. אצבעות הידיים והרגליים כבר לא מחוברות זו לזו.',
  12: 'רפלקסים ראשונים מתפתחים — האצבעות יכולות להתכופף. הכליות מתחילות לייצר שתן שמתערבב במי השפיר.',
  13: 'סוף השליש הראשון. קול הזמזום של דופק העובר הופך לברור יותר בבדיקות. טביעות האצבע כבר מתחילות להיווצר.',
  14: 'תווי הפנים מתחדדים, והעובר מתחיל לבצע תנועות פנים כמו עיווצי גבות. אפשר לעיתים לזהות את מין העובר.',
  15: 'העצמות ממשיכות להתקשות, והעובר מתחיל לחוש אור דרך העפעפיים העדיין סגורים. חוש השמיעה מתחיל להתפתח.',
  16: 'ייתכן שתתחילו להרגיש תנועות עובריות ראשונות ("בעיטות") — עדיין עדינות ודומות לרפרוף. השלד ממשיך להתקשות.',
  17: 'רקמת שומן ראשונה מתחילה להצטבר מתחת לעור. השליה ממשיכה לגדול ולהתחזק כדי לתמוך בהתפתחות.',
  18: 'האוזניים במקומן הסופי ומתחילות לשמוע צלילים מבחוץ — כולל את הקול שלכם. ייתכן שתרגישו תנועות בבירור יותר.',
  19: 'שכבת הגנה שעווית (וֶרְניקְס) מתחילה לכסות את העור. חושי הטעם, הריח, השמיעה, הראייה והמישוש מתחילים להתמיין באזורי המוח.',
  20: 'מחצית הדרך! ייתכן שתעברו סקירת מערכות מפורטת סביב השבוע הזה, ולעיתים ניתן לזהות את מין העובר בבירור.',
  21: 'התנועות הופכות לחזקות ומורגשות יותר. מערכת העיכול מתחילה לתפקד באופן ראשוני, וריסים ועפעפיים כבר מפותחים.',
  22: 'העובר מתחיל לפתח חוש מגע — נוגע בפניו ובחבל הטבור. העור עדיין דק ומקומט, לפני שנוצר שומן מתחתיו.',
  23: 'הריאות מפתחות "עצים" של דרכי אוויר, בהכנה לנשימה עתידית. שינויים בלחץ אוויר או רעש חזק עלולים לגרום לתגובה של העובר.',
  24: 'נחשב לעיתים כ"גבול קיימות" מבחינה רפואית, אם כי המשך ההיריון קריטי להתפתחות הריאות והמוח. חוש שיווי המשקל מתחיל להתפתח.',
  25: 'שיער עדין (לָנוּגו) מכסה חלקים מהגוף ומסייע לוויסות חום. השליה ממשיכה לספק תזונה וחמצן בקצב הולך וגדל.',
  26: 'העיניים מתחילות להיפתח לראשונה. הריאות ממשיכות לייצר "סורפקטנט" — חומר שיאפשר בעתיד נשימה עצמאית.',
  27: 'סוף השליש השני. המוח פעיל מאוד ומייצר תבניות גלי מוח, ותקופות שינה וערות מתחילות להתגבש.',
  28: 'ריסי העיניים מלאים והעפעפיים יכולים להיפתח ולהיסגר. העובר מגיב לאור, לקול ולטעמים במי השפיר.',
  29: 'המוח והשלד ממשיכים להתפתח במהירות, ולכן הצורך בסידן, חלבון וברזל בתזונה גובר בתקופה הזו.',
  30: 'העיניים יכולות כעת לעקוב אחרי אור. מח העצם קלט לגמרי את ייצור תאי הדם האדומים מהכבד והטחול.',
  31: 'התנועות עשויות להרגיש חזקות אך פחות מרווחות — פשוט כי נהיה צפוף יותר בבטן. הריאות והמוח ממשיכים להבשיל בקצב מהיר.',
  32: 'ציפורניים כבר מגיעות לקצות האצבעות. משקל העובר עולה במהירות ככל שמצטברת רקמת שומן מתחת לעור.',
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
  4: { name: 'זרעון פרג', emoji: '🌱' },
  5: { name: 'גרגר שומשום', emoji: '🌰' },
  6: { name: 'גרגר עדשה', emoji: '🫘' },
  7: { name: 'אוכמנית', emoji: '🫐' },
  8: { name: 'פטל', emoji: '🍇' },
  9: { name: 'ענב', emoji: '🍒' },
  10: { name: 'תות שדה', emoji: '🍓' },
  11: { name: 'תאנה קטנה', emoji: '🍐' },
  12: { name: 'ליים', emoji: '🍋' },
  13: { name: 'אפרסק', emoji: '🍑' },
  14: { name: 'לימון', emoji: '🍋' },
  15: { name: 'תפוח', emoji: '🍎' },
  16: { name: 'אבוקדו', emoji: '🥑' },
  17: { name: 'אגס גדול', emoji: '🍐' },
  18: { name: 'פלפל', emoji: '🫑' },
  19: { name: 'מנגו', emoji: '🥭' },
  20: { name: 'בננה', emoji: '🍌' },
  21: { name: 'גזר גדול', emoji: '🥕' },
  22: { name: 'קישוא', emoji: '🥒' },
  23: { name: 'בטטה', emoji: '🍠' },
  24: { name: 'קלח תירס', emoji: '🌽' },
  25: { name: 'כרובית', emoji: '🥦' },
  26: { name: 'חציל', emoji: '🍆' },
  27: { name: 'קולורבי', emoji: '🥬' },
  28: { name: 'סלק', emoji: '🥬' },
  29: { name: 'דלעת עוגה', emoji: '🎃' },
  30: { name: 'כרוב סגול', emoji: '🥬' },
  31: { name: 'קוקוס', emoji: '🥥' },
  32: { name: 'דלעת קטנה', emoji: '🎃' },
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

export interface WeeklyAnimal {
  name: string
  emoji: string
}

export const WEEKLY_ANIMAL: Record<number, WeeklyAnimal> = {
  4: { name: 'נמלה', emoji: '🐜' },
  5: { name: 'דבורה', emoji: '🐝' },
  6: { name: 'חיפושית', emoji: '🐞' },
  7: { name: 'פרפר', emoji: '🦋' },
  8: { name: 'דג זהב', emoji: '🐠' },
  9: { name: 'אוגר', emoji: '🐹' },
  10: { name: 'עכבר קטן', emoji: '🐭' },
  11: { name: 'סנאי', emoji: '🐿️' },
  12: { name: 'צפרדע קטנה', emoji: '🐸' },
  13: { name: 'אפרוח', emoji: '🐤' },
  14: { name: 'חתלתול', emoji: '🐱' },
  15: { name: 'גוזל ינשוף', emoji: '🦉' },
  16: { name: 'קיפוד', emoji: '🦔' },
  17: { name: 'ארנבון', emoji: '🐰' },
  18: { name: 'לוטרה צעירה', emoji: '🦦' },
  19: { name: 'שרקן', emoji: '🦝' },
  20: { name: 'בונה', emoji: '🦫' },
  21: { name: 'שועל צעיר', emoji: '🦊' },
  22: { name: 'כלבלב', emoji: '🐶' },
  23: { name: 'חתול צעיר', emoji: '🐈' },
  24: { name: 'חזרזיר', emoji: '🐖' },
  25: { name: 'טלה', emoji: '🐑' },
  26: { name: 'גדי', emoji: '🐐' },
  27: { name: 'עופר צבי', emoji: '🦌' },
  28: { name: 'עצלן צעיר', emoji: '🦥' },
  29: { name: 'קנגורו צעיר', emoji: '🦘' },
  30: { name: 'גור נמר', emoji: '🐆' },
  31: { name: 'גור טיגריס', emoji: '🐅' },
  32: { name: 'גור דוב', emoji: '🐻' },
  33: { name: 'גור זאב', emoji: '🐺' },
  34: { name: 'גור אריה', emoji: '🦁' },
  35: { name: 'גור פיל', emoji: '🐘' },
  36: { name: 'גור תנין', emoji: '🐊' },
  37: { name: 'גור כלב ים', emoji: '🦭' },
  38: { name: 'אפרוח פינגווין', emoji: '🐧' },
  39: { name: 'גור קואלה', emoji: '🐨' },
  40: { name: 'תינוקת מוכנה!', emoji: '👶' },
}

export function getWeeklyAnimal(week: number): WeeklyAnimal | undefined {
  return WEEKLY_ANIMAL[week]
}
