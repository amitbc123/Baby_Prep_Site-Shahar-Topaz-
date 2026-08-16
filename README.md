# אור ירח

ארגונית פרטית זוג — שחר וטופז — לקראת הגעת בת. משימות (כולל תיק ליולדת), רשימת קניות
עם תקציב ועדיפויות, תאריכים ומשאלות, מעקב מחזור, תיקיות ומסמכים (כולל סריקה), ספירה
לאחור עד ירח מלא.

**מוצר פעיל: אפליקציית Android** (`android/`) — לא ה-PWA. הכל מוצפן קצה-לקצה: שני
בני זוג חולקים סביבת עבודה אחת, Supabase (שרת) לעולם לא רואה תוכן פשוט, רק ציפרטקסט.
פירוט מלא בהחלטות ארכיטקטורה תחת `docs/architecture/`, מצב התקדמות תחת `PROGRESS.md`.

## Android — פיתוח מקומי

```bash
cd android
./gradlew :app:assembleDebug   # APK לבדיקה
./gradlew test                 # כל הבדיקות (כולל :core:crypto, :core:domain)
./gradlew lint                 # oxlint המקביל של Android
```

הרצה מ-Android Studio (AGP 9, JDK 21) או מכשיר/אמולטור עם `./gradlew installDebug`.
בנייה חתומה (`assembleRelease`) דורשת סודות keystore
(`ANDROID_KEYSTORE_BASE64` וכו') — בלעדיהם build עובר אך יוצא **לא חתום**, במכוון (ר'
`docs/architecture/011-release-signing-and-updates.md`).

### מבנה מודולים

`:core:*` — תשתית משותפת בלי תלות במסך ספציפי: `model`/`common`/`domain` — Kotlin טהור
(בדיקים בלי אמולטור), `crypto` (Bouncy Castle, ChaCha20-Poly1305 + HPKE),
`database` (Room+SQLCipher), `network` (Supabase), `security` (Keystore, ביומטריה),
`sync`, `scanner` (ML Kit), `settings`, `update` (עדכון אוטומטי מ-GitHub Releases).

`:feature:*` — מסך אחד לכל מודול (`auth`, `pairing`, `tasks`, `shopping`, `dates`, `home`,
`folders`, `cycle`, `settings`, `update`). מודולי feature **לא** תלויים זה בזה — ניווט
ביניהם ב-`:app` בלבד.

Compose + Material 3, MVVM (state/effect), Koin ל-DI. פירוט ונימוקים מלאים:
`docs/architecture/001-android-architecture.md`.

### מודל שיתוף

זוג = סביבת עבודה אחת, מפתח הצפנה אחד. אין הרשאות פר-פריט, אין "פרטי מול השני" — שני
בני זוג רואים הכל. גבול פרטיות: זוג מול עולם חיצון, לא בן זוג מול בן זוג
(`docs/architecture/005-data-privacy.md`). מכשיר חדש מצטרף בקוד הזמנה; מפתח עובר בין
מכשירים מוצפן, לעולם לא דרך שרת בפשוט. משפט שחזור 24 מילים (BIP-39) — דרך יחידה
חזרה פנימה אם שני מכשירים אבדו.

## אתר אינטרנט (legacy)

`src/` מכיל PWA מקורי (React + Vite, `localStorage` בלבד, בלי שרת) — **פריסה
הופסקה**, נשמר להתייחסות בלבד. אפליקציית Android — ממשיכו, כולל ייבוא JSON
חד-פעמי מייצוא ישן (`:core:domain`'s `WebSnapshot`/`toImportedSnapshot`). רוצים
להריץ מקומית עדיין:

```bash
npm install
npm run dev
```

פריסה אוטומטית ל-GitHub Pages (`.github/workflows/deploy.yml`) עדיין בריפו אך לא
מוצר פעיל יותר.