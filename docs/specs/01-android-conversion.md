# Android App Conversion + Private Couple Task, Document & Menstrual Cycle Manager

## 1. Goal

Take existing website/app in repo, convert to **native Android application**. Preserve existing functionality, extend into private, secure app for two people.

Use these Android skills as baseline:

https://github.com/rcosteira79/android-skills

Use relevant skills from this repo for:

* Android architecture
* Kotlin
* Jetpack Compose
* Material 3
* Coroutines / Flow
* Room
* DataStore
* Koin
* Networking
* Testing
* Gradle
* Security
* Debugging
* Modularization
* Android UX

Don't ignore these skills, don't reinvent architecture.

App should combine:

```text
Tasks
+
Folders
+
Documents
+
Document Scanning
+
Menstrual Cycle Tracking
+
Private Couple Sharing
+
Offline-First Synchronization
```

App meant for **two private users: me and wife**.

Privacy first-class requirement.

---

# 2. Important Clarification — "Period"

Term **Period** here means:

> **Menstrual period / menstrual cycle tracking**

NOT task time tracking, work periods, timers, time-management.

Don't implement task timers as "period" functionality.

Tasks can still have:

* due dates
* start dates
* reminders
* recurring schedules

but menstrual cycle tracking completely separate feature/module.

---

# 3. First Step — Analyze Existing Website

Before changing code:

1. Inspect entire existing project.
2. Identify:

   * frontend framework
   * backend/API
   * database
   * authentication
   * existing routes/pages
   * existing components
   * existing data models
   * existing task functionality
   * existing document functionality
   * APIs
   * authentication/session mechanism
3. Run existing website locally.
4. Understand current UX.
5. Understand current data model.
6. Identify what reusable.
7. Create migration/implementation plan.

Don't blindly rewrite existing app.

Preserve existing functionality unless good reason to change.

---

# 4. Android Application

Create **real native Android application**.

Don't just wrap website in WebView.

Preferred stack:

* Kotlin
* Jetpack Compose
* Material 3
* MVVM
* Kotlin Coroutines
* Kotlin Flow
* Room
* DataStore
* Koin
* Retrofit or Ktor
* Kotlin Serialization
* WorkManager
* CameraX
* Android Storage Access Framework
* Android Keystore
* BiometricPrompt where appropriate

Follow architecture recommended by Android skills.

Preferred architecture:

```text
UI
 ↓
ViewModel
 ↓
Use Case / Domain
 ↓
Repository
 ↓
Local / Remote Data Sources
```

Keep business logic out of Composables.

---

# 5. Application Modules

Structure app around clear feature modules.

Suggested:

```text
app/

core/
    common/
    model/
    database/
    network/
    security/
    storage/
    sync/
    ui/

feature/
    home/
    tasks/
    folders/
    documents/
    scanner/
    calendar/
    cycle/
    sharing/
    settings/
```

Don't over-engineer tiny features into unnecessary modules.

---

# 6. Main Application Concept

App becomes private personal/couple organizer.

High-level model:

```text
Private Couple Workspace
│
├── Tasks
│   ├── Personal
│   └── Shared
│
├── Folders
│   ├── Tasks
│   └── Documents
│
├── Documents
│   ├── Personal
│   └── Shared
│
├── Menstrual Cycle
│   └── Wife's cycle data
│
└── Shared Activity
```

Menstrual cycle module needs own privacy model.

---

# 7. Privacy Classification

Every data piece needs explicit visibility level.

At minimum:

```text
PRIVATE
SHARED
```

Potential model:

```text
visibility = PRIVATE
visibility = SHARED_WITH_PARTNER
```

Default:

```text
Tasks       → Private
Documents   → Private
Cycle data  → Private
Notes       → Private
```

User can explicitly choose to share supported items.

Never assume same couple workspace membership means every item auto-visible.

---

# 8. Two-User Couple Workspace

App must support exactly two users in initial implementation.

Example:

```text
User A
Me

User B
Wife
```

Create private couple workspace:

```text
Couple Workspace
├── User A
└── User B
```

Only these two accounts access it.

Backend must enforce membership.

Don't use predictable workspace ID as security mechanism.

Every request must verify:

```text
Authenticated User
        ↓
Workspace Membership
        ↓
Resource Ownership
        ↓
Resource Visibility
        ↓
Permission
```

---

# 9. Authentication

Implement secure authentication.

If existing website already has auth, evaluate if safely reusable.

Otherwise support secure auth system such as:

* Email/password
* Magic link
* OAuth/OIDC

Users need unique IDs.

Example:

```text
User
├── id: UUID
├── email
├── createdAt
└── updatedAt
```

Never store auth tokens in plain SharedPreferences.

Use secure storage / Android Keystore as appropriate.

---

# 10. Couple Pairing

Provide easy way to connect two accounts.

Example:

```text
Settings
   ↓
Couple
   ↓
Invite Partner
   ↓
Generate secure invitation
   ↓
Wife accepts
   ↓
Couple workspace created
```

Don't use simple predictable numeric pairing code as sole security mechanism.

Invitation tokens should:

* expire
* be single-use
* be random
* be revocable
* be tied to initiating account

---

# 11. Tasks

Tasks remain important part of app.

Each task should support:

* Title
* Description
* Notes
* Status
* Priority
* Tags
* Folder
* Created date
* Updated date
* Due date
* Start date
* Recurrence
* Reminder
* Attachments
* Checklist/subtasks
* Created by
* Assigned to
* Visibility
* Sync status

Example:

```text
Task
├── Buy baby equipment
├── Folder: Baby
├── Due: 20/08/2026
├── Assigned to: Wife
├── Visibility: Shared
└── Attachments
    ├── shopping-list.pdf
    └── product.jpg
```

---

# 12. Task Ownership and Sharing

Each task should contain:

```text
createdBy
assignedTo
visibility
```

Example:

```text
Task:
Buy groceries

Created by:
Me

Assigned to:
Wife

Visibility:
SHARED
```

Private example:

```text
Task:
Buy surprise gift

Created by:
Me

Visibility:
PRIVATE
```

Wife must not retrieve private tasks through API.

---

# 13. Folders

Add folder system.

Users should be able to:

* Create folder
* Rename folder
* Delete folder
* Create subfolder
* Move folder
* Move task
* Move document
* Move task between folders
* Move documents between folders

Example:

```text
📁 Home
├── 📁 Repairs
│   ├── Fix kitchen
│   └── invoice.pdf
│
├── 📁 Shopping
│   └── Furniture
│
└── 📁 Documents
    ├── contract.pdf
    └── receipt.jpg
```

Support nested folders.

At minimum support 5 nesting levels.

Unlimited nesting OK if no UX/performance problems.

---

# 14. Documents

Add document management.

Users must be able to:

* Upload files
* Import files
* Scan documents
* Take photos
* Attach documents to tasks
* Attach documents to folders
* Rename files
* Move files
* Delete files
* Search files
* Preview files
* Download/open files
* Share files with partner
* See file metadata

Supported formats should include:

```text
PDF
DOC
DOCX
XLS
XLSX
CSV
TXT
MD
JSON
JPG
JPEG
PNG
WEBP
HEIC
```

Design system so more formats addable later.

---

# 15. Android File Picker

Use Android Storage Access Framework.

User should see:

```text
+ Add

├── Upload File
├── Scan Document
├── Take Photo
└── Create Folder
```

Avoid broad filesystem permissions when scoped storage APIs suffice.

Use modern Android APIs.

---

# 16. Document Scanner

Implement document scanning.

Use CameraX and appropriate Android document-scanning functionality.

Flow:

```text
Scan Document
      ↓
Camera
      ↓
Detect document
      ↓
Crop / perspective correction
      ↓
Preview
      ↓
Retake / Add page
      ↓
Save
      ↓
Choose Folder
      ↓
Optional Task Attachment
```

Support multi-page documents.

Example:

```text
Scan

Page 1
Page 2
Page 3

↓
Save as

invoice.pdf
```

Allow:

* PDF
* Image

where practical.

---

# 17. OCR

If practical, implement optional OCR.

Example:

```text
Scan document
      ↓
OCR
      ↓
Extract text
      ↓
Store searchable text
```

Prefer offline OCR when practical.

OCR optional.

Don't send private documents to third-party OCR services without explicit user consent.

OCR text is private data, follows same visibility rules as document.

---

# 18. Document Preview

Implement previews for common formats.

```text
PDF      → PDF viewer
Images   → Image viewer
TXT      → Text viewer
JSON     → Formatted text
CSV      → Table
Office   → System/external viewer where appropriate
```

Don't implement full Office editing unless required.

---

# 19. Search

Implement global search.

Search:

* Tasks
* Folders
* Documents
* Notes
* Tags
* File names
* OCR text
* Menstrual cycle notes where user has permission

Example:

```text
Search: insurance

Results:

📁 Insurance
📄 insurance.pdf
✅ Renew insurance
📝 Insurance notes
```

Search results must respect visibility.

If User A searches, private User B data must never appear.

---

# 20. Menstrual Cycle Tracking

Create dedicated **Cycle Tracking** feature.

One of primary app modules.

Suggested navigation:

```text
Home
Tasks
Calendar
Cycle
Documents
Shared
Settings
```

Cycle screen should clearly communicate this is menstrual-cycle tracking.

---

# 21. Cycle Data Model

Menstrual cycle should be modeled separately from tasks.

Potential model:

```text
MenstrualCycle
├── id
├── userId
├── startDate
├── endDate
├── cycleLength
├── periodLength
├── predictedStartDate
├── predictedEndDate
├── predictedOvulationDate
├── fertileWindowStart
├── fertileWindowEnd
├── notes
├── visibility
├── createdAt
├── updatedAt
└── version
```

Don't store all calculated values as authoritative if derivable.

Prefer storing actual user-entered events, calculating predictions from historical data.

---

# 22. Period Logging

Allow wife to easily record:

```text
Period started
Period ended
```

Example UI:

```text
Did your period start today?

[ Yes, started today ]

Period started:
15 Aug 2026
```

Allow manual correction.

Example:

```text
Period started:
12 Aug 2026

Period ended:
17 Aug 2026
```

User must be able to edit historical records.

---

# 23. Cycle Calendar

Create dedicated menstrual-cycle calendar.

Display:

```text
August 2026

Sun Mon Tue Wed Thu Fri Sat

             1   2   3
 4   5   6   7   8   9  10
11  12  13  14  15  16  17
18  19  20  21  22  23  24
25  26  27  28  29  30  31
```

Clearly distinguish:

* Actual period days
* Predicted period days
* Fertile window
* Estimated ovulation
* Current day

Don't make predictions look like confirmed events.

Use labels such as:

```text
Actual
Predicted
Estimated
```

---

# 24. Cycle Predictions

Calculate:

* Average cycle length
* Average period duration
* Predicted next period
* Estimated fertile window
* Estimated ovulation

Use historical cycle data.

Important:

Predictions are estimates, not medical facts.

Don't claim prediction guarantees:

* ovulation
* fertility
* pregnancy prevention
* pregnancy detection

UI should make distinction clear.

Example:

```text
Estimated next period

Around 20 Aug

Based on your previous cycles.
```

---

# 25. Irregular Cycles

Don't assume every cycle exactly same length.

Support:

```text
Cycle 1 → 27 days
Cycle 2 → 31 days
Cycle 3 → 25 days
Cycle 4 → 29 days
```

Calculate predictions using historical data.

If insufficient historical data:

```text
Not enough cycle history yet.

Continue tracking to improve estimates.
```

Don't fabricate precise predictions from insufficient data.

---

# 26. Symptoms

Allow wife to record optional symptoms.

Examples:

```text
Cramps
Headache
Bloating
Fatigue
Back pain
Breast tenderness
Nausea
Acne
Spotting
Other
```

Allow custom symptoms.

Symptoms should support:

```text
date
intensity
note
```

Example:

```text
Cramps
Intensity: Moderate
Date: 15 Aug
```

---

# 27. Flow Tracking

Allow optional flow tracking.

Example:

```text
Flow

○ None
○ Spotting
○ Light
○ Medium
○ Heavy
```

Should be optional.

Don't force user to record medical details.

---

# 28. Mood Tracking

Optional mood tracking:

```text
Mood

😊 Good
🙂 Okay
😐 Neutral
😔 Low
😡 Irritated
😴 Tired
```

Allow multiple moods if appropriate.

Keep interface simple.

---

# 29. Pain Tracking

Allow optional pain tracking.

Example:

```text
Pain
0 ───────── 10
```

Optionally specify:

```text
Location:
- Lower abdomen
- Back
- Head
- Other
```

This tracking data, not diagnosis.

Don't provide medical diagnoses based on this info.

---

# 30. Cycle Notes

Allow free-form notes.

Example:

```text
Cycle notes

"Felt more tired than usual."

"Traveling this week."

"Period started later than expected."
```

Notes sensitive/private by default.

---

# 31. Cycle History

Provide historical cycles.

Example:

```text
Cycle History

Aug 2026
29 days

Jul 2026
28 days

Jun 2026
31 days

May 2026
27 days
```

Allow opening cycle for details.

---

# 32. Cycle Statistics

Show useful statistics:

```text
Average cycle:
28.7 days

Average period:
5.1 days

Shortest cycle:
26 days

Longest cycle:
32 days
```

Don't overstate statistical accuracy.

Clearly indicate these based only on recorded history.

---

# 33. Cycle Sharing

Menstrual cycle data must be:

**PRIVATE BY DEFAULT.**

Wife decides whether to share with partner.

Example:

```text
Cycle Privacy

● Private
○ Share with Partner
```

If shared:

```text
Shared with:
Husband

Shared data:
☑ Period dates
☑ Predicted next period
☑ Cycle calendar
☐ Symptoms
☐ Mood
☐ Notes
```

Provide granular sharing where practical.

At minimum support:

```text
Share cycle
Don't share cycle
```

Prefer more granular controls.

---

# 34. Important Privacy Rule

Husband must NEVER automatically receive:

* menstrual dates
* symptoms
* mood
* pain
* notes
* cycle history

just because connected as partner.

Wife must explicitly opt into sharing.

Example:

```text
Wife:
Cycle data → PRIVATE

Husband:
Cannot query it
Cannot search it
Cannot download it
Cannot infer it through API responses
```

---

# 35. Shared Cycle View

If wife explicitly enables sharing, husband may see only info she selected.

Example:

```text
Partner Shared Cycle

Current cycle:
Day 18

Estimated next period:
~20 Aug

Shared information:
Period dates
Predicted period
```

If symptoms not shared:

```text
Symptoms:
Private
```

Don't expose hidden data through statistics, notifications, search, or sync metadata.

---

# 36. Cycle Notifications

Optional reminders:

```text
Period prediction
Cycle logging reminder
Missed logging reminder
```

Don't create alarming notifications.

Notifications must respect privacy.

Avoid sensitive text on lock screen by default.

Instead of:

```text
Your period is expected tomorrow.
```

prefer:

```text
Cycle reminder
```

unless user explicitly enables sensitive notification content.

---

# 37. Pregnancy Mode — Optional

If appropriate, design architecture so future pregnancy mode addable.

Don't automatically implement pregnancy calculations unless explicitly required.

Current system should allow future extension:

```text
Cycle Tracking
     ↓
Future:
Pregnancy Tracking
```

Keep data model extensible.

---

# 38. Medical Safety

This app is tracking tool.

NOT medical diagnostic system.

Don't implement claims such as:

```text
"You are definitely ovulating."
"You cannot become pregnant today."
"Your symptoms mean X disease."
```

Use language such as:

```text
Estimated
Predicted
Based on previous cycles
```

If medical warnings eventually added, must be conservative and appropriately sourced.

---

# 39. Offline-First Architecture

App must work without Internet.

Architecture:

```text
                 Android Phone
                       │
              ┌────────▼────────┐
              │ Local Database  │
              │      Room       │
              └────────┬────────┘
                       │
                  Sync Queue
                       │
                    Network
                       │
              ┌────────▼────────┐
              │    Backend      │
              │ PostgreSQL      │
              │ File Storage    │
              └─────────────────┘
```

UI should primarily read from local storage.

Network sync updates local data.

Don't make UI dependent on network.

---

# 40. Sensitive Data Sync

Menstrual cycle info needs additional privacy layer.

For every cycle-related record:

```text
ownerId
visibility
sharedWith
workspaceId
```

Before syncing:

```text
Is this data allowed to sync to partner?
```

If private:

```text
Sync to owner's devices only
```

If shared:

```text
Sync to authorized partner
```

Never use:

```text
workspace membership = permission to see all data
```

---

# 41. Sync Engine

Use reliable synchronization.

Entities should include fields similar to:

```text
id
createdAt
updatedAt
deletedAt
version
ownerId
workspaceId
visibility
syncStatus
```

Potential sync states:

```text
LOCAL_ONLY
PENDING_UPLOAD
SYNCED
PENDING_UPDATE
PENDING_DELETE
CONFLICT
```

Use WorkManager.

Sync should retry safely.

Operations should be idempotent where practical.

---

# 42. Sync Privacy Example

Example:

```text
Wife phone:

Cycle:
PRIVATE

Task:
SHARED

Document:
PRIVATE
```

Sync should produce:

```text
Backend

Wife's private cycle
        ↓
Available to Wife devices only

Shared task
        ↓
Available to both users

Private document
        ↓
Available to Wife devices only
```

Husband's device should never download private cycle then hide it in UI.

Backend must prevent access in first place.

---

# 43. Conflict Handling

Both devices may modify shared records.

Example:

```text
Task:
User A → "Buy milk"
User B → "Buy milk and bread"
```

Don't silently lose changes.

Use:

```text
version
updatedAt
clientMutationId
```

For important shared content, consider:

```text
Conflict detected

Your version:
Buy milk

Partner version:
Buy milk and bread

[Keep Mine]
[Keep Partner's]
[Merge]
```

For cycle data, avoid blindly merging contradictory events.

Example:

```text
Period start:
Wife device → Aug 14
Partner device → Aug 15
```

Owner of cycle data should have authority to resolve conflict.

Partner edits to cycle data should generally not be allowed unless explicitly supported.

---

# 44. File Synchronization

Large files shouldn't embed directly in normal JSON API requests.

Use:

```text
Create document metadata
        ↓
Request upload
        ↓
Upload file
        ↓
Checksum verification
        ↓
Mark upload complete
        ↓
Sync metadata
```

Use:

* SHA-256 or equivalent checksum
* resumable uploads where practical
* background upload
* retry
* progress reporting

---

# 45. Encryption

Treat menstrual cycle info and personal documents as sensitive.

Use:

```text
Device
 ↓
Encrypted local storage
 ↓
TLS
 ↓
Backend
 ↓
Protected database/storage
```

Use Android Keystore where appropriate.

For highly sensitive data, consider application-level encryption or end-to-end encryption.

If E2EE implemented:

* use established cryptographic libraries
* don't invent cryptographic algorithms
* document key management
* document device recovery
* document account recovery

Don't claim "end-to-end encrypted" unless implementation actually provides end-to-end encryption.

---

# 46. Backend Database

Potential schema:

```text
users

couple_workspaces

workspace_members

folders

tasks

task_checklists

tags

task_tags

documents

document_versions

task_documents

folder_documents

menstrual_cycles

cycle_symptoms

cycle_flow_entries

cycle_mood_entries

cycle_notes

cycle_sharing_permissions

sync_operations

devices

notifications
```

Adjust according to existing app.

Use UUIDs.

Use migrations.

Never destroy existing data during migration.

---

# 47. Menstrual Cycle Database Design

Prefer normalized records.

Example:

```text
menstrual_cycles
----------------
id
owner_id
start_date
end_date
created_at
updated_at
deleted_at
visibility
version
```

Symptoms:

```text
cycle_symptoms
---------------
id
cycle_id
owner_id
date
type
intensity
note
created_at
updated_at
```

Mood:

```text
cycle_moods
-----------
id
cycle_id
owner_id
date
mood
note
```

Sharing:

```text
cycle_sharing_permissions
-------------------------
id
cycle_owner_id
partner_id
share_cycle_dates
share_predictions
share_symptoms
share_mood
share_notes
updated_at
```

Don't duplicate cycle data for partner.

Partner should receive authorized views of owner's data.

---

# 48. API Authorization

Every API endpoint must enforce authorization.

Example:

```text
GET /cycles/{cycleId}
```

Server:

```text
Authenticate user

Load cycle

IF cycle.ownerId == currentUser:
    allow

ELSE IF cycle is shared with currentUser:
    return only permitted fields

ELSE:
    HTTP 403 / appropriate not-found behavior
```

Never rely on Android UI restrictions.

---

# 49. Prevent Privacy Leaks

Test for indirect leaks.

Husband's account must not learn private cycle info through:

* Search
* Statistics
* Calendar
* Notifications
* Sync metadata
* Task attachments
* Document names
* API counts
* API errors
* Predictable IDs
* Cache
* Local database
* Background sync
* Logs

For example, don't return:

```json
{
  "cycleCount": 12
}
```

to husband if even count itself reveals private info.

---

# 50. Calendar

Main app calendar may contain:

```text
Tasks
Shared events
Cycle events
```

But cycle events must respect privacy.

If wife hasn't shared cycle data:

```text
Husband calendar:
No cycle information
```

If shared:

```text
Husband calendar:
Shared cycle information only
```

Avoid visually exposing private cycle info through colors, dots, badges, or empty-space patterns.

---

# 51. Dashboard

Create clean dashboard.

Example for wife:

```text
Good morning 👋

Today
────────────────────

Tasks
☐ Buy groceries
☐ Upload document

Cycle
────────────────────
Cycle day 18

Estimated next period:
~20 Aug

Folders
────────────────────
🏠 Home
💼 Work
📄 Documents

Recent Documents
────────────────────
invoice.pdf
receipt.jpg
```

Example for husband when cycle data private:

```text
Good morning 👋

Today
────────────────────

Tasks
☐ Buy groceries

Folders
────────────────────
🏠 Home
💼 Work
📄 Documents
```

No hidden cycle info should appear.

---

# 52. Home Widgets / Quick Actions

Consider Android shortcuts/widgets.

Examples:

```text
+ Add Task
+ Upload Document
+ Scan Document
Log Period
```

**Log Period** action should only appear for user who owns cycle data.

Don't expose sensitive cycle info through widgets by default.

---

# 53. Recurring Tasks

Support:

```text
Every day
Every week
Every month
Every year
Custom
```

Model recurring tasks efficiently.

Don't generate thousands of unnecessary task records.

---

# 54. Tags

Support tags:

```text
#home
#work
#shopping
#documents
#baby
#finance
#medical
```

Cycle-related tags should be treated as sensitive if revealing private health info.

Don't expose private tags to partner.

---

# 55. Settings

Include:

```text
Account
Couple
Sharing
Cycle Tracking
Notifications
Appearance
Sync
Storage
Security
Privacy
Biometric Lock
About
```

Cycle settings:

```text
Cycle Tracking
----------------
Cycle tracking enabled

Default cycle visibility:
● Private
○ Shared

Share with partner:
[Configure]

Notifications:
[Configure]
```

---

# 56. Security Settings

Include:

```text
Require biometric authentication
Auto-lock
Sensitive notification content
Manage connected devices
Clear local cache
Logout
```

For sensitive data:

```text
Show private information in app previews
```

Allow user to disable it.

---

# 57. Notifications

Notifications must respect privacy.

Good:

```text
Cycle reminder
Task due soon
Document upload complete
Sync completed
```

Potentially sensitive:

```text
Your period starts tomorrow
```

Default to generic notification.

Allow user to explicitly enable sensitive notification content.

---

# 58. Document Privacy

Documents have same visibility model:

```text
PRIVATE
SHARED
```

Private document:

* syncs to owner's devices
* stored securely
* cannot be accessed by partner

Shared document:

* syncs to authorized devices
* visible to both users
* remains protected by backend authorization

---

# 59. Document Attachments to Cycle

Allow wife to optionally attach documents to cycle records.

Examples:

```text
Cycle
 ├── Notes
 ├── Symptoms
 └── Documents
     ├── medical-document.pdf
     └── image.jpg
```

Attachments must inherit cycle's privacy rules by default.

If cycle private:

```text
Document → Private
```

If cycle shared:

```text
Document → Shared
```

unless explicitly overridden.

---

# 60. Accessibility

Support:

* TalkBack
* content descriptions
* semantic labels
* sufficient contrast
* scalable fonts
* large touch targets
* keyboard navigation where appropriate

Don't rely solely on color to communicate:

* period days
* fertile window
* predicted dates
* private/shared status

Use labels and patterns in addition to colors.

---

# 61. UI / UX

Use modern Material 3.

App should feel like polished native Android app.

Use:

* Material 3
* light theme
* dark theme
* dynamic color where appropriate
* responsive layouts
* empty states
* loading states
* error states
* accessibility
* smooth but restrained animations

Avoid:

* WebView-like UX
* excessive cards
* giant buttons everywhere
* unnecessary animations
* cluttered dashboards

---

# 62. Navigation

Suggested:

```text
Home
Tasks
Calendar
Cycle
Documents
Shared
Settings
```

For husband:

If no access to wife's cycle data, Cycle section should either:

* not appear, or
* show only explicitly shared cycle information

Don't show empty/private state that indirectly reveals private data exists unless appropriate.

---

# 63. Local Room Database

Use Room.

Potential entities:

```text
TaskEntity
FolderEntity
DocumentEntity
MenstrualCycleEntity
CycleSymptomEntity
CycleMoodEntity
CycleFlowEntity
CycleSharingPermissionEntity
WorkspaceEntity
SyncOperationEntity
```

Use indexes for:

```text
ownerId
workspaceId
folderId
updatedAt
dueDate
startDate
visibility
syncStatus
```

For cycle data also index:

```text
cycle owner
cycle start date
cycle updated date
```

---

# 64. Local Sensitive Data Protection

Don't assume Room alone sufficient for highly sensitive data.

Evaluate:

* encrypted database
* encrypted file storage
* Android Keystore
* secure key storage
* biometric unlock
* automatic locking

Sensitive data includes:

```text
cycle data
symptoms
mood
pain
cycle notes
private documents
private tasks
authentication tokens
```

---

# 65. Offline Behavior

While offline:

User must be able to:

* create tasks
* edit tasks
* create folders
* import documents
* scan documents
* record menstrual period
* edit cycle data
* add symptoms
* add notes

Changes saved locally.

When Internet returns:

```text
Local Changes
      ↓
Sync Queue
      ↓
Server
      ↓
Partner devices where permitted
```

---

# 66. Cycle Offline Behavior

If wife records:

```text
Period started:
15 Aug
```

while offline:

```text
Room
 ↓
Local cycle record
 ↓
Sync queue
```

When online:

```text
Server receives record
 ↓
Authorization
 ↓
Store
 ↓
Sync to wife's devices
```

If cycle shared:

```text
 ↓
Sync permitted information to husband
```

If private:

```text
 ↓
Do NOT sync to husband
```

---

# 67. Testing — General

Testing mandatory.

Unit tests:

* task calculations
* folder hierarchy
* recurring tasks
* search
* document metadata
* sync
* permissions
* conflict resolution

---

# 68. Testing — Menstrual Cycle

Create extensive tests for:

### Cycle length

```text
Start:
2026-08-01

Next start:
2026-08-29

Cycle:
28 days
```

### Period duration

```text
Start:
Aug 1

End:
Aug 5

Duration:
5 days
```

### Irregular cycles

```text
27
31
25
29
```

Ensure predictions don't assume fixed cycle.

### Insufficient history

Ensure app doesn't fabricate precise predictions.

### Manual correction

Test editing previous cycle dates.

---

# 69. Testing — Cycle Privacy

These tests critical.

### Test 1

Wife creates:

```text
Cycle:
PRIVATE
```

Husband attempts:

```text
GET /cycles/{id}
```

Expected:

```text
DENIED
```

### Test 2

Wife changes:

```text
PRIVATE → SHARED
```

Expected:

```text
Husband can access permitted cycle information.
```

### Test 3

Wife shares:

```text
Cycle dates
```

but not:

```text
Symptoms
Mood
Notes
```

Expected:

```text
Husband:
Cycle dates → visible

Symptoms → hidden
Mood → hidden
Notes → hidden
```

### Test 4

Husband searches:

```text
cramps
```

Expected:

```text
No results from wife's private symptoms.
```

### Test 5

Husband requests:

```text
statistics
```

Expected:

```text
No statistics derived from private cycle data.
```

### Test 6

Private cycle must not appear in:

* calendar
* notifications
* widgets
* search
* sync
* analytics
* logs

---

# 70. Testing — Two Users

Test:

```text
User A creates shared task
        ↓
Server
        ↓
User B receives task
```

Test:

```text
User A creates private task
        ↓
Server
        ↓
User B cannot access
```

Test:

```text
User A uploads private document
        ↓
User B cannot access
```

Test:

```text
User A records private cycle
        ↓
User B cannot access
```

Test:

```text
User A shares cycle
        ↓
User B can access only permitted fields
```

---

# 71. Testing — Security

Test:

* unauthorized API access
* cross-user access
* cross-workspace access
* private cycle access
* private document access
* private task access
* expired authentication
* invalid IDs
* malicious filenames
* oversized files
* unsupported file types
* path traversal
* upload authorization
* search authorization
* calendar authorization

Don't only test Android UI.

Test backend authorization directly.

---

# 72. Testing — Synchronization

Test:

```text
Offline
 ↓
Create task
 ↓
Create folder
 ↓
Record period
 ↓
Upload document
 ↓
Reconnect
 ↓
Synchronize
```

Verify all records.

Also test:

```text
Offline on Device A
Offline on Device B
Both modify shared task
Reconnect
 ↓
Conflict handling
```

---

# 73. Performance

App should remain responsive with:

```text
10,000+ tasks
10,000+ documents
large folder trees
large cycle history
large document libraries
```

Use:

* LazyColumn
* Paging where appropriate
* database indexes
* background processing
* thumbnails
* cached previews
* efficient Flow usage

Never load all documents into memory.

---

# 74. Backend File Storage

Store files outside relational database where practical.

Use:

```text
PostgreSQL
+
Private Object Storage
```

Potential:

```text
S3-compatible storage
```

Files must not be public.

Use authenticated access or short-lived signed URLs.

---

# 75. Backend Deployment

If backend required, provide Docker support.

Example:

```text
docker-compose.yml

services:

  backend
  postgres
  object-storage
```

Don't commit secrets.

Use:

```text
.env.example
```

instead of:

```text
.env
```

---

# 76. Data Migration

If existing website already has data:

create import/migration system.

Don't delete existing data.

Potential:

```text
Existing Database
       ↓
Migration
       ↓
New Schema
       ↓
Validation
       ↓
Android
```

Validate:

* task counts
* document counts
* folder relationships
* user relationships

---

# 77. Progress Tracking

Create:

```text
PROGRESS.md
```

Keep updated throughout implementation.

Example:

```markdown
# Progress

## Phase 1 — Existing Application
- [ ] Analyze frontend
- [ ] Analyze backend
- [ ] Analyze database
- [ ] Analyze authentication

## Phase 2 — Android
- [ ] Native Android project
- [ ] Compose
- [ ] Navigation
- [ ] Theme

## Phase 3 — Tasks
- [ ] Task model
- [ ] Task UI
- [ ] Recurring tasks
- [ ] Notifications

## Phase 4 — Folders
- [ ] Folder hierarchy
- [ ] Move tasks
- [ ] Move documents

## Phase 5 — Documents
- [ ] File picker
- [ ] Upload
- [ ] Scanner
- [ ] Preview
- [ ] OCR

## Phase 6 — Menstrual Cycle
- [ ] Cycle model
- [ ] Period logging
- [ ] Cycle calendar
- [ ] Predictions
- [ ] Symptoms
- [ ] Flow
- [ ] Mood
- [ ] Notes
- [ ] History
- [ ] Statistics
- [ ] Reminders
- [ ] Privacy controls

## Phase 7 — Couple
- [ ] Authentication
- [ ] Couple pairing
- [ ] Shared workspace
- [ ] Permissions

## Phase 8 — Sync
- [ ] Offline storage
- [ ] Sync queue
- [ ] Background sync
- [ ] Conflict handling
- [ ] File sync

## Phase 9 — Security
- [ ] Encryption
- [ ] Secure storage
- [ ] Backend authorization
- [ ] Privacy testing

## Phase 10 — Testing
- [ ] Unit tests
- [ ] UI tests
- [ ] Integration tests
- [ ] Security tests
- [ ] Sync tests

## Phase 11 — Release
- [ ] Debug APK
- [ ] Release build
- [ ] Documentation
```

Update after every meaningful development phase.

---

# 78. Architecture Decision Records

Create:

```text
docs/architecture/
```

Include:

```text
001-android-architecture.md
002-local-first-sync.md
003-authentication.md
004-couple-workspace.md
005-data-privacy.md
006-document-storage.md
007-encryption.md
008-cycle-tracking.md
009-cycle-sharing.md
010-conflict-resolution.md
```

Document why each decision made.

---

# 79. Do Not Overengineer

Build in phases.

## P0 — Must Have

* Native Android app
* Existing website functionality
* Tasks
* Folders
* Documents
* File upload
* Document scanner
* Menstrual period tracking
* Cycle calendar
* Cycle history
* Basic cycle predictions
* Authentication
* Two-user private workspace
* Offline-first storage
* Synchronization
* Privacy controls
* Backend authorization
* Basic encryption/security

## P1

* OCR
* Advanced cycle statistics
* Symptoms
* Mood
* Flow tracking
* Granular cycle sharing
* Notifications
* Search
* Conflict UI
* Shared activity

## P2

* Advanced analytics
* Widgets
* Advanced document processing
* Pregnancy tracking
* Additional integrations
* Automation

---

# 80. Git Workflow

Before modifying project:

```bash
git status
git branch
git log --oneline -10
```

Never overwrite uncommitted user changes.

Create feature branch:

```text
feature/android-app
```

Use small logical commits.

Example:

```text
feat(android): initialize native compose application
feat(tasks): add task management
feat(folders): add folder hierarchy
feat(documents): add document management
feat(scanner): add document scanner
feat(cycle): add menstrual cycle tracking
feat(cycle): add cycle predictions
feat(cycle): add symptom tracking
feat(sharing): add private couple workspace
feat(sync): add offline synchronization
feat(security): protect private user data
test(cycle): add menstrual cycle tests
test(privacy): add cross-user authorization tests
```

Never commit:

```text
.env
API keys
passwords
tokens
private certificates
production credentials
```

---

# 81. Claude Code Execution Instructions

You lead Android engineer.

Don't merely describe what should be done.

**Actually implement project.**

Start by inspecting repo.

Then:

1. Read/use relevant Android skills from:
   https://github.com/rcosteira79/android-skills

2. Analyze existing website.

3. Analyze its backend.

4. Analyze its database.

5. Analyze authentication.

6. Create architecture plan.

7. Create `PROGRESS.md`.

8. Create native Android application.

9. Implement existing functionality.

10. Implement tasks.

11. Implement folders.

12. Implement documents.

13. Implement file upload.

14. Implement document scanning.

15. Implement search.

16. Implement menstrual cycle tracking.

17. Implement cycle calendar.

18. Implement cycle predictions.

19. Implement symptoms/mood/flow where practical.

20. Implement cycle privacy.

21. Implement two-user authentication.

22. Implement couple pairing.

23. Implement private/shared resources.

24. Implement offline-first storage.

25. Implement synchronization.

26. Implement conflict handling.

27. Implement encryption/security.

28. Implement notifications.

29. Implement tests.

30. Build app.

31. Run tests.

32. Fix errors.

33. Update documentation.

34. Update `PROGRESS.md`.

Work incrementally.

After every major phase:

```text
Build
↓
Test
↓
Run
↓
Fix
↓
Update PROGRESS.md
↓
Commit
```

Don't make one giant change.

---

# 82. Important Product Principle

Menstrual cycle functionality **not secondary task timer feature**.

Dedicated health-data tracking module.

Architecture should clearly separate:

```text
Task Management
        │
        ├── Tasks
        ├── Folders
        └── Documents

Cycle Tracking
        │
        ├── Periods
        ├── Cycles
        ├── Symptoms
        ├── Mood
        ├── Flow
        ├── Predictions
        └── Cycle Sharing
```

May integrate in UI, but data models, permissions, business logic stay separate.

---

# 83. Critical Privacy Principle

Wife owns her menstrual-cycle data.

Therefore:

```text
Wife
 │
 ├── Can view her cycle
 ├── Can edit her cycle
 ├── Can delete her cycle
 └── Controls sharing
        │
        ├── Private
        │
        └── Shared with Partner
```

Husband:

```text
Can view only what the wife explicitly shares.
```

Shouldn't automatically gain access just because accounts paired.

---

# 84. Definition of Done

Project NOT complete merely because Android APK builds.

Complete when:

* Existing website functionality works in Android
* Tasks work
* Folders work
* Documents work
* File uploads work
* Document scanning works
* Search works
* Menstrual periods can be recorded
* Cycle history works
* Cycle calendar works
* Predictions work
* Predictions clearly labeled as estimates
* Symptoms can be tracked
* Mood can be tracked
* Flow can be tracked
* Cycle notes work
* Cycle privacy works
* Cycle sharing works
* Authentication works
* Couple pairing works
* Two users can share authorized data
* Private data remains private
* Offline mode works
* Synchronization works
* Conflict handling exists
* Backend authorization tested
* Local sensitive data protected
* Documents protected
* Notifications respect privacy
* Tests pass
* Lint passes
* Debug build works
* Release build can be generated
* Documentation exists
* `PROGRESS.md` current

---

# 85. Final Report

When implementation complete, provide:

```text
## Implementation Summary

### Existing Website
...

### Android Architecture
...

### New Features
...

### Task System
...

### Document System
...

### Menstrual Cycle System
...

### Couple / Sharing Architecture
...

### Privacy & Security
...

### Offline / Sync Architecture
...

### Database
...

### Files Added
...

### Tests
...

### Build Result
...

### Known Limitations
...

### How to Run
...

### How to Build APK
...
```

Also explicitly explain:

```text
How User A authenticates
How User B authenticates
How the couple accounts are paired
How private data is stored
How shared data is synchronized
How menstrual cycle data is protected
How cycle sharing works
How private documents are protected
How offline changes synchronize
How conflicts are resolved
How account/device recovery works
```

Don't claim app secure without verifying through tests.

---

# 86. Final Architecture Target

Desired architecture should look approximately like:

```text
                         ┌─────────────────────────┐
                         │       YOUR PHONE        │
                         │                         │
                         │ Native Android          │
                         │ Jetpack Compose         │
                         │                         │
                         │ Room                    │
                         │ ├── Tasks               │
                         │ ├── Folders             │
                         │ ├── Documents           │
                         │ └── Your private data   │
                         │                         │
                         │ Encrypted Storage       │
                         └────────────┬────────────┘
                                      │
                              Encrypted Sync
                                      │
                                      ▼
                         ┌─────────────────────────┐
                         │        BACKEND          │
                         │                         │
                         │ Authentication          │
                         │ Authorization            │
                         │ Couple Workspace       │
                         │                         │
                         │ PostgreSQL              │
                         │ Private Object Storage  │
                         │                         │
                         │ Privacy Enforcement     │
                         └────────────┬────────────┘
                                      │
                              Encrypted Sync
                                      │
                                      ▼
                         ┌─────────────────────────┐
                         │      WIFE'S PHONE       │
                         │                         │
                         │ Native Android          │
                         │ Jetpack Compose         │
                         │                         │
                         │ Room                    │
                         │ ├── Tasks               │
                         │ ├── Folders             │
                         │ ├── Documents           │
                         │ └── Her cycle data      │
                         │                         │
                         │ Encrypted Storage       │
                         └─────────────────────────┘
```

Fundamental security rule:

```text
PRIVATE DATA
     ↓
Owner's authorized devices only

SHARED DATA
     ↓
Owner + explicitly authorized partner

NEVER
     ↓
"Same workspace = access to everything"
```

Menstrual cycle system must follow this rule strictly.

App should be **offline-first, privacy-first, native Android, designed specifically for two trusted users**, while remaining extensible for future features.