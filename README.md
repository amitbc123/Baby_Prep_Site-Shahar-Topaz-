# אור ירח

ארגונית פרטית לזוג — שחר וטופז — לקראת הגעת הבת. משימות (כולל תיק ליולדת), רשימת קניות
עם תקציב ועדיפויות, תאריכים ומשאלות, מעקב מחזור, תיקיות ומסמכים (כולל סריקה), וספירה
לאחור עד הירח המלא.

**המוצר הפעיל הוא אפליקציית Android** (`android/`) — לא ה-PWA. הכל מוצפן מקצה-לקצה: שני
בני הזוג חולקים סביבת עבודה אחת, ו-Supabase (השרת) לעולם לא רואה תוכן פשוט, רק ציפרטקסט.
פירוט מלא בהחלטות הארכיטקטורה תחת `docs/architecture/` ובמצב ההתקדמות תחת `PROGRESS.md`.

## Android — פיתוח מקומי

```bash
cd android
./gradlew :app:assembleDebug   # APK לבדיקה
./gradlew test                 # כל הבדיקות (כולל :core:crypto, :core:domain)
./gradlew lint                 # oxlint המקביל של Android
```

מריצים מ-Android Studio (AGP 9, JDK 21) או מכשיר/אמולטור עם `./gradlew installDebug`.
בנייה חתומה (`assembleRelease`) דורשת את סודות ה-keystore
(`ANDROID_KEYSTORE_BASE64` וכו') — ללא הם ה-build עובר אך יוצא **לא חתום**, במכוון (ר'
`docs/architecture/011-release-signing-and-updates.md`).

### מבנה המודולים

`:core:*` — תשתית משותפת ללא תלות במסך ספציפי: `model`/`common`/`domain` הם Kotlin טהור
(ניתנים לבדיקה בלי אמולטור), `crypto` (Bouncy Castle, ChaCha20-Poly1305 + HPKE),
`database` (Room+SQLCipher), `network` (Supabase), `security` (Keystore, ביומטריה),
`sync`, `scanner` (ML Kit), `settings`, `update` (עדכון אוטומטי מ-GitHub Releases).

`:feature:*` — מסך אחד לכל מודול (`auth`, `pairing`, `tasks`, `shopping`, `dates`, `home`,
`folders`, `cycle`, `settings`, `update`). מודולי feature **לא** תלויים זה בזה — ניווט
ביניהם קורה ב-`:app` בלבד.

Compose + Material 3, MVVM (state/effect), Koin ל-DI. פירוט מלא ונימוקים:
`docs/architecture/001-android-architecture.md`.

### מודל השיתוף

זוג = סביבת עבודה אחת עם מפתח הצפנה אחד. אין הרשאות פר-פריט, אין "פרטי מול השני" — שני
בני הזוג רואים הכל. גבול הפרטיות הוא הזוג מול העולם החיצון, לא בן זוג מול בן זוג
(`docs/architecture/005-data-privacy.md`). מכשיר חדש מצטרף בקוד הזמנה; המפתח מועבר בין
המכשירים מוצפן, לעולם לא דרך השרת בפשוט. משפט שחזור בן 24 מילים (BIP-39) הוא הדרך היחידה
חזרה פנימה אם שני המכשירים אבדו.

## אתר האינטרנט (legacy)

`src/` מכיל את ה-PWA המקורי (React + Vite, `localStorage` בלבד, ללא שרת) — **פרישה
הופסקה**, נשמר להתייחסות בלבד. אפליקציית ה-Android היא הממשיך שלו, כולל ייבוא JSON
חד-פעמי מהייצוא הישן (`:core:domain`'s `WebSnapshot`/`toImportedSnapshot`). אם עדיין רוצים
להריץ אותו מקומית:

```bash
npm install
npm run dev
```

הפריסה האוטומטית ל-GitHub Pages (`.github/workflows/deploy.yml`) עדיין קיימת בריפו אך אינה
המוצר הפעיל יותר.
