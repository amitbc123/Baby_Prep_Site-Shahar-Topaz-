# Android App Conversion + Private Couple Task, Document & Menstrual Cycle Manager

## 1. Goal

Take the existing website/application in this repository and convert it into a **native Android application**, while preserving the existing functionality and extending it into a private, secure application for two people.

Use the following Android skills as the baseline:

https://github.com/rcosteira79/android-skills

Use the relevant skills from this repository for:

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

Do not ignore these skills and reinvent the architecture.

The application should ultimately combine:

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

The application is intended for **two private users: me and my wife**.

Privacy is a first-class requirement.

---

# 2. Important Clarification — "Period"

The term **Period** in this project means:

> **Menstrual period / menstrual cycle tracking**

It does NOT mean task time tracking, work periods, timers, or time-management periods.

Do not implement task timers as the "period" functionality.

Tasks can still have:

* due dates
* start dates
* reminders
* recurring schedules

but menstrual cycle tracking is a completely separate feature/module.

---

# 3. First Step — Analyze Existing Website

Before changing code:

1. Inspect the entire existing project.
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
3. Run the existing website locally.
4. Understand the current UX.
5. Understand the current data model.
6. Identify what can be reused.
7. Create a migration/implementation plan.

Do NOT blindly rewrite the existing application.

Preserve existing functionality unless there is a good reason to change it.

---

# 4. Android Application

Create a **real native Android application**.

Do NOT simply wrap the website in a WebView.

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

Follow the architecture recommended by the Android skills.

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

Structure the application around clear feature modules.

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

Do not over-engineer tiny features into unnecessary modules.

---

# 6. Main Application Concept

The application should become a private personal/couple organizer.

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

The menstrual cycle module must have its own privacy model.

---

# 7. Privacy Classification

Every piece of data should have an explicit visibility level.

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

The user can explicitly choose to share supported items.

Never assume that being members of the same couple workspace means every item is automatically visible.

---

# 8. Two-User Couple Workspace

The application must support exactly two users in the initial implementation.

Example:

```text
User A
Me

User B
Wife
```

Create a private couple workspace:

```text
Couple Workspace
├── User A
└── User B
```

Only these two accounts can access it.

The backend must enforce membership.

Do not use a predictable workspace ID as a security mechanism.

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

If the existing website already has authentication, evaluate whether it can safely be reused.

Otherwise support a secure authentication system such as:

* Email/password
* Magic link
* OAuth/OIDC

Users must have unique IDs.

Example:

```text
User
├── id: UUID
├── email
├── createdAt
└── updatedAt
```

Never store authentication tokens in plain SharedPreferences.

Use secure storage / Android Keystore as appropriate.

---

# 10. Couple Pairing

Provide an easy way to connect the two accounts.

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

Do not use a simple predictable numeric pairing code as the sole security mechanism.

Invitation tokens should:

* expire
* be single-use
* be random
* be revocable
* be tied to the initiating account

---

# 11. Tasks

Tasks remain an important part of the application.

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

The wife must not be able to retrieve private tasks through the API.

---

# 13. Folders

Add a folder system.

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

Unlimited nesting can be supported if it does not create UX/performance problems.

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

Design the system so additional formats can be added later.

---

# 15. Android File Picker

Use Android Storage Access Framework.

The user should see:

```text
+ Add

├── Upload File
├── Scan Document
├── Take Photo
└── Create Folder
```

Avoid broad filesystem permissions when scoped storage APIs are sufficient.

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

OCR should be optional.

Do not send private documents to third-party OCR services without explicit user consent.

OCR text is private data and must follow the same visibility rules as the document.

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

Do not implement full Office editing unless required.

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
* Menstrual cycle notes where the user has permission

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

If User A searches for something, private User B data must never appear.

---

# 20. Menstrual Cycle Tracking

Create a dedicated **Cycle Tracking** feature.

This is one of the primary application modules.

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

The Cycle screen should clearly communicate that this is menstrual-cycle tracking.

---

# 21. Cycle Data Model

A menstrual cycle should be modeled separately from tasks.

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

Do not store all calculated values as authoritative if they can be derived.

Prefer storing actual user-entered events and calculating predictions from historical data.

---

# 22. Period Logging

Allow the wife to easily record:

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

The user must be able to edit historical records.

---

# 23. Cycle Calendar

Create a dedicated menstrual-cycle calendar.

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

Do not make predictions look like confirmed events.

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

Do not claim that the prediction can guarantee:

* ovulation
* fertility
* pregnancy prevention
* pregnancy detection

The UI should make this distinction clear.

Example:

```text
Estimated next period

Around 20 Aug

Based on your previous cycles.
```

---

# 25. Irregular Cycles

Do NOT assume every cycle is exactly the same length.

Support:

```text
Cycle 1 → 27 days
Cycle 2 → 31 days
Cycle 3 → 25 days
Cycle 4 → 29 days
```

Calculate predictions using historical data.

If insufficient historical data exists:

```text
Not enough cycle history yet.

Continue tracking to improve estimates.
```

Do not fabricate precise predictions from insufficient data.

---

# 26. Symptoms

Allow the wife to record optional symptoms.

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

This should be optional.

Do not force the user to record medical details.

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

Keep the interface simple.

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

This is tracking data, not diagnosis.

Do not provide medical diagnoses based on this information.

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

Notes are sensitive/private by default.

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

Allow opening a cycle for details.

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

Do not overstate statistical accuracy.

Clearly indicate that these are based only on recorded history.

---

# 33. Cycle Sharing

Menstrual cycle data must be:

**PRIVATE BY DEFAULT.**

The wife decides whether to share it with her partner.

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

The husband must NEVER automatically receive:

* menstrual dates
* symptoms
* mood
* pain
* notes
* cycle history

simply because he is connected as the partner.

The wife must explicitly opt into sharing.

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

If the wife explicitly enables sharing, the husband may see only the information she selected.

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

If symptoms were not shared:

```text
Symptoms:
Private
```

Do not expose hidden data through statistics, notifications, search, or synchronization metadata.

---

# 36. Cycle Notifications

Optional reminders:

```text
Period prediction
Cycle logging reminder
Missed logging reminder
```

Do not create alarming notifications.

Notifications must respect privacy.

Avoid sensitive text on the lock screen by default.

Instead of:

```text
Your period is expected tomorrow.
```

prefer:

```text
Cycle reminder
```

unless the user explicitly enables sensitive notification content.

---

# 37. Pregnancy Mode — Optional

If appropriate, design the architecture so a future pregnancy mode can be added.

Do NOT automatically implement pregnancy calculations unless explicitly required.

The current system should allow future extension:

```text
Cycle Tracking
     ↓
Future:
Pregnancy Tracking
```

Keep the data model extensible.

---

# 38. Medical Safety

This application is a tracking tool.

It is NOT a medical diagnostic system.

Do not implement claims such as:

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

If medical warnings are eventually added, they must be conservative and appropriately sourced.

---

# 39. Offline-First Architecture

The application must work without Internet.

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

The UI should primarily read from local storage.

Network synchronization updates local data.

Do not make the UI dependent on the network.

---

# 40. Sensitive Data Sync

Menstrual cycle information must have an additional privacy layer.

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

Synchronization should retry safely.

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

Synchronization should produce:

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

The husband's device should never download the private cycle and then hide it in the UI.

The backend must prevent access in the first place.

---

# 43. Conflict Handling

Both devices may modify shared records.

Example:

```text
Task:
User A → "Buy milk"
User B → "Buy milk and bread"
```

Do not silently lose changes.

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

The owner of the cycle data should have authority to resolve the conflict.

Partner edits to cycle data should generally not be allowed unless explicitly supported.

---

# 44. File Synchronization

Large files should not be embedded directly in normal JSON API requests.

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

Treat menstrual cycle information and personal documents as sensitive.

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

If E2EE is implemented:

* use established cryptographic libraries
* do not invent cryptographic algorithms
* document key management
* document device recovery
* document account recovery

Do not claim "end-to-end encrypted" unless the implementation actually provides end-to-end encryption.

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

Adjust according to the existing application.

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

Do not duplicate cycle data for the partner.

The partner should receive authorized views of the owner's data.

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

The husband's account must not learn private cycle information through:

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

For example, do NOT return:

```json
{
  "cycleCount": 12
}
```

to the husband if even the count itself reveals private information.

---

# 50. Calendar

The main application calendar may contain:

```text
Tasks
Shared events
Cycle events
```

But cycle events must respect privacy.

If the wife has not shared cycle data:

```text
Husband calendar:
No cycle information
```

If shared:

```text
Husband calendar:
Shared cycle information only
```

Avoid visually exposing private cycle information through colors, dots, badges, or empty-space patterns.

---

# 51. Dashboard

Create a clean dashboard.

Example for the wife:

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

Example for the husband when cycle data is private:

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

No hidden cycle information should appear.

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

The **Log Period** action should only appear for the user who owns the cycle data.

Do not expose sensitive cycle information through widgets by default.

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

Do not generate thousands of unnecessary task records.

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

Cycle-related tags should be treated as sensitive if they reveal private health information.

Do not expose private tags to the partner.

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

Allow the user to disable it.

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

Default to a generic notification.

Allow the user to explicitly enable sensitive notification content.

---

# 58. Document Privacy

Documents have the same visibility model:

```text
PRIVATE
SHARED
```

A private document:

* syncs to owner's devices
* is stored securely
* cannot be accessed by partner

A shared document:

* syncs to authorized devices
* is visible to both users
* remains protected by backend authorization

---

# 59. Document Attachments to Cycle

Allow the wife to optionally attach documents to cycle records.

Examples:

```text
Cycle
 ├── Notes
 ├── Symptoms
 └── Documents
     ├── medical-document.pdf
     └── image.jpg
```

These attachments must inherit the cycle's privacy rules by default.

If the cycle is private:

```text
Document → Private
```

If the cycle is shared:

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

Do not rely solely on color to communicate:

* period days
* fertile window
* predicted dates
* private/shared status

Use labels and patterns in addition to colors.

---

# 61. UI / UX

Use modern Material 3.

The application should feel like a polished native Android application.

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

For the husband:

If he does not have access to the wife's cycle data, the Cycle section should either:

* not appear, or
* show only explicitly shared cycle information

Do not show an empty/private state that indirectly reveals that private data exists unless appropriate.

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

Do not assume that Room alone is sufficient for highly sensitive data.

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

The user must be able to:

* create tasks
* edit tasks
* create folders
* import documents
* scan documents
* record menstrual period
* edit cycle data
* add symptoms
* add notes

Changes are saved locally.

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

If the wife records:

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

If the cycle is shared:

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

Testing is mandatory.

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

Ensure predictions don't assume a fixed cycle.

### Insufficient history

Ensure the app does not fabricate precise predictions.

### Manual correction

Test editing previous cycle dates.

---

# 69. Testing — Cycle Privacy

These tests are critical.

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

Do not only test the Android UI.

Test the backend authorization directly.

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

The app should remain responsive with:

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

Store files outside the relational database where practical.

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

If a backend is required, provide Docker support.

Example:

```text
docker-compose.yml

services:

  backend
  postgres
  object-storage
```

Do not commit secrets.

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

If the existing website already contains data:

create an import/migration system.

Do not delete existing data.

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

Keep it updated throughout implementation.

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

Update this after every meaningful development phase.

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

Document why each decision was made.

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

Before modifying the project:

```bash
git status
git branch
git log --oneline -10
```

Never overwrite uncommitted user changes.

Create a feature branch:

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

You are the lead Android engineer.

Do not merely describe what should be done.

**Actually implement the project.**

Start by inspecting the repository.

Then:

1. Read/use the relevant Android skills from:
   https://github.com/rcosteira79/android-skills

2. Analyze the existing website.

3. Analyze its backend.

4. Analyze its database.

5. Analyze authentication.

6. Create the architecture plan.

7. Create `PROGRESS.md`.

8. Create the native Android application.

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

30. Build the application.

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

Do not make one giant change.

---

# 82. Important Product Principle

The menstrual cycle functionality is **not a secondary task timer feature**.

It is a dedicated health-data tracking module.

The architecture should clearly separate:

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

They may integrate in the UI, but their data models, permissions, and business logic should remain separate.

---

# 83. Critical Privacy Principle

The wife owns her menstrual-cycle data.

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

The husband:

```text
Can view only what the wife explicitly shares.
```

He should not automatically gain access because the accounts are paired.

---

# 84. Definition of Done

The project is NOT complete merely because the Android APK builds.

It is complete when:

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
* Predictions are clearly labeled as estimates
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
* Backend authorization is tested
* Local sensitive data is protected
* Documents are protected
* Notifications respect privacy
* Tests pass
* Lint passes
* Debug build works
* Release build can be generated
* Documentation exists
* `PROGRESS.md` is current

---

# 85. Final Report

When implementation is complete, provide:

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

Do not claim that the application is secure without verifying it through tests.

---

# 86. Final Architecture Target

The desired architecture should look approximately like:

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

The fundamental security rule is:

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

The menstrual cycle system must follow this rule strictly.

The app should be **offline-first, privacy-first, native Android, and designed specifically for two trusted users**, while remaining extensible for future features.
