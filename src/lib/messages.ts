const MESSAGES = [
  'עוד קצת, ותהיו שלושה.',
  'כל יום שעובר הוא יום קרוב יותר אליה.',
  'תנשמו עמוק. אתם מוכנים יותר משאתם חושבים.',
  'היא כבר מחכה לפגוש אתכם.',
  'הבית כמעט מוכן. הלבבות שלכם כבר היו.',
  'עוד ירח אחד, ותתחיל ההרפתקה הכי גדולה שלכם.',
]

export function getDailyMessage(from = new Date()): string {
  const dayOfYear = Math.floor(
    (Date.UTC(from.getFullYear(), from.getMonth(), from.getDate()) -
      Date.UTC(from.getFullYear(), 0, 0)) /
      86400000,
  )
  return MESSAGES[dayOfYear % MESSAGES.length]
}
