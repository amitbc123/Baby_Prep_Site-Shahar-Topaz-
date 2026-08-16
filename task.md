# App Auto-Update & Version Control

## Goal

Add auto **version control + update notification system** to app.

New app version commit/release to Git repo → users notified on open, offered install latest.

System must: reliable, secure, easy maintain, no forced update unless configured mandatory.

---

## 1. Version Management

App need single source of truth for current version.

Example:

```text
Current app version: 1.2.0
```

Use Semantic Versioning:

```text
MAJOR.MINOR.PATCH
```

Examples:

```text
1.0.0
1.0.1
1.1.0
2.0.0
```

Version auto-update as part release process, not manual multi-file edit.

### Requirements

* Store app version one central location.
* Running app must expose current version.
* Git tags used for releases.
* Recommended tag format:

```text
v1.0.0
v1.1.0
v1.2.0
```

---

## 2. Git Repository as Update Source

Use Git repo/release system as update info source.

Prefer **GitHub Releases** if project hosted GitHub.

Each release contain:

```text
Version
Release date
Release notes
Downloadable application package
```

Example:

```text
v1.3.0

Release notes:
- Added feature X
- Fixed issue Y
- Improved performance
```

App should query repo release info on start.

Do NOT scrape normal Git commits or HTML pages.

Use proper release/API mechanism.

---

## 3. Startup Update Check

On user launch app:

```text
Application starts
        ↓
Load current application version
        ↓
Check latest release
        ↓
Compare versions
        ↓
Is newer version available?
      /       \
    No         Yes
    ↓           ↓
Continue    Show update notification
```

Update check async — no block startup.

App must stay usable if update server/Git repo unavailable.

---

## 4. Version Comparison

Implement proper semantic version comparison.

Example:

```text
Current: 1.2.0
Latest:  1.3.0

Result:
UPDATE AVAILABLE
```

```text
Current: 1.3.0
Latest:  1.2.5

Result:
NO UPDATE
```

Don't compare versions as strings.

Handle correctly:

```text
1.9.0 < 1.10.0
```

System should support pre-release versions if appropriate:

```text
1.3.0-beta.1
1.3.0-rc.1
```

Default: stable releases compared against stable releases only.

---

## 5. Update Notification

Newer version exists → show clear notification.

Example:

```text
🎉 New version available!

A new version of MyApp is available.

Current version: 1.2.0
Latest version:  1.3.0

What's new:
• Added automatic backups
• Improved performance
• Fixed several bugs

[Install Update] [View Release] [Later]
```

### Buttons

#### Install Update

Starts update process.

#### View Release

Opens GitHub release page in user browser.

#### Later

Closes notification.

App remember user dismissed notification, avoid repeat-show same notification every startup.

---

## 6. Update Download

User selects:

```text
Install Update
```

app should:

1. Determine correct release asset for user platform.
2. Download update.
3. Validate downloaded file.
4. Install/update application.
5. Restart application if required.

Show download progress:

```text
Downloading update...

████████████████░░░░ 80%

Version 1.3.0
```

UI must clearly show errors if download fail.

---

## 7. Security

Don't blindly execute arbitrary files downloaded from Git.

Update system should support integrity verification.

Preferred approach:

```text
Release
   ↓
Application package
   ↓
SHA-256 checksum
   ↓
Application downloads package
   ↓
Calculate SHA-256
   ↓
Compare checksum
   ↓
Only install if checksum matches
```

Stronger security: support signed releases/artifacts where practical.

Never install update if:

* Download corrupted.
* Checksum mismatch.
* Release metadata invalid.
* Downloaded file not expected platform/package.

---

## 8. Release Manifest

Create machine-readable release manifest.

Example:

```json
{
  "version": "1.3.0",
  "release_date": "2026-08-15",
  "mandatory": false,
  "release_url": "https://github.com/OWNER/REPOSITORY/releases/tag/v1.3.0",
  "notes": [
    "Added automatic backups",
    "Improved performance",
    "Fixed bugs"
  ],
  "assets": {
    "windows": {
      "url": "...",
      "sha256": "..."
    },
    "linux": {
      "url": "...",
      "sha256": "..."
    },
    "macos": {
      "url": "...",
      "sha256": "..."
    }
  }
}
```

Manifest should only contain info app needs.

---

## 9. Release Automation

Create release workflow in GitHub Actions.

Recommended flow:

```text
Developer changes code
        ↓
Commit
        ↓
Push
        ↓
CI
        ↓
Tests
        ↓
Build
        ↓
Package application
        ↓
Calculate SHA-256
        ↓
Create GitHub Release
        ↓
Create release assets
        ↓
Publish version
```

Release only created after app passes required CI checks.

---

## 10. Automatic Versioning

Prefer automated version generation from Git tags.

Example:

```text
git tag v1.4.0
git push origin v1.4.0
```

CI pipeline should:

1. Detect tag.
2. Extract `1.4.0`.
3. Build application using that version.
4. Package application.
5. Generate checksums.
6. Create GitHub Release.
7. Upload application artifacts.

Avoid manual version edit across multiple source files.

---

## 11. Update Check Frequency

Don't query GitHub repeatedly during app usage.

Recommended:

* Check once on app startup.
* Cache result.
* No check more than once few hours unless user manually selects "Check for Updates".

Add menu item:

```text
Help
 └── Check for Updates
```

Example:

```text
✓ You are running the latest version.

Version 1.3.0
```

---

## 12. Offline Behavior

App must work normally without Internet.

Update check fails:

```text
Unable to check for updates.
```

Don't prevent app starting.

Don't show scary error for normal network failure.

Log failure for diagnostics.

---

## 13. Mandatory Updates

Support optional mandatory-update flag.

Example:

```json
{
  "version": "2.0.0",
  "mandatory": true
}
```

If `mandatory` is `true`:

```text
Important Update Required

Version 2.0.0 is required to continue using this application.

[Update Now]
```

`Later` button unavailable for mandatory updates.

Use mandatory updates only when necessary.

---

## 14. Skipping Versions

Updater must support users upgrading direct from older versions.

Example:

```text
Installed: 1.0.0
Latest:    1.5.0
```

App should offer:

```text
1.0.0 → 1.5.0
```

not require:

```text
1.0.0 → 1.1.0 → 1.2.0 → 1.3.0 → 1.4.0 → 1.5.0
```

Unless app has specific migration requirement.

---

## 15. Database/Data Migration

New version changes app data format → add migration support.

Example:

```text
Application 1.2.0
        ↓
Update
        ↓
Migration
        ↓
Application 1.3.0
```

Never let update silently destroy/invalidate existing user data.

Before migrations:

* Create backups when appropriate.
* Validate existing data.
* Run migrations.
* Verify result.
* Report failures clearly.

---

## 16. Update State

Store updater state locally.

Example:

```json
{
  "last_update_check": "2026-08-15T08:30:00Z",
  "last_notified_version": "1.3.0",
  "skipped_version": null
}
```

Prevents repeat-notify user same release.

---

## 17. Error Handling

Handle at least:

* No Internet connection.
* GitHub unavailable.
* API rate limiting.
* Invalid release metadata.
* Invalid version.
* Download failure.
* Corrupted download.
* Checksum mismatch.
* Unsupported platform.
* Insufficient permissions.
* Installation failure.
* Restart failure.

Errors should never leave app broken state.

---

## 18. Logging

Add structured logs for:

```text
Update check started
Current version: 1.2.0
Latest version: 1.3.0
Update available
Download started
Download completed
Checksum verified
Installation started
Installation completed
Update failed
```

Don't log sensitive info like tokens/credentials.

---

## 19. UI/UX Requirements

Updater should feel native part of app.

Don't show technical GitHub/API info to normal users.

Use friendly messages:

```text
A new version is available.
```

instead of:

```text
GET /repos/xxx/releases/latest returned 200.
```

Notification should clearly show:

* Current version.
* New version.
* Release notes.
* Update button.
* Later button.
* Release page option.

---

## 20. Architecture

Create dedicated update service/module.

Recommended structure:

```text
Updater
├── VersionManager
├── ReleaseChecker
├── VersionComparator
├── ReleaseManifest
├── DownloadManager
├── IntegrityVerifier
├── UpdateInstaller
├── UpdateState
└── UpdateNotification
```

Responsibilities:

### VersionManager

Returns currently installed version.

### ReleaseChecker

Checks GitHub release/manifest.

### VersionComparator

Determines if remote version newer.

### DownloadManager

Downloads correct update package.

### IntegrityVerifier

Validates SHA-256/signature.

### UpdateInstaller

Performs actual installation.

### UpdateState

Stores update-check + notification state.

### UpdateNotification

Displays update UI.

Keep components separated so updater testable independently.

---

## 21. Configuration

Keep repo info configurable, not hard-coded throughout app.

Example:

```text
UPDATE_PROVIDER=github
GITHUB_OWNER=YOUR_OWNER
GITHUB_REPOSITORY=YOUR_REPOSITORY
UPDATE_CHANNEL=stable
```

For example:

```text
stable
beta
nightly
```

Default channel:

```text
stable
```

---

## 22. Testing

Add automated tests for:

### Version comparison

```text
1.0.0 → 1.0.1 = update
1.0.1 → 1.0.0 = no update
1.9.0 → 1.10.0 = update
1.0.0 → 1.0.0 = no update
```

### Update service

Test:

* New version available.
* No new version.
* Invalid response.
* Network failure.
* API failure.
* Unsupported platform.

### Download

Test:

* Successful download.
* Failed download.
* Interrupted download.
* Checksum mismatch.

### UI

Test:

* Update notification.
* Later.
* Install Update.
* Mandatory update.
* Release notes display.

---

## 23. Developer Workflow

Intended dev workflow simple as:

```bash
git checkout main
git pull

# Make changes

git add .
git commit -m "feat: add new feature"

git tag v1.3.0
git push origin main
git push origin v1.3.0
```

GitHub Actions handles rest:

```text
Build
↓
Test
↓
Package
↓
Checksum
↓
Release
↓
Publish
```

Users then receive:

```text
New version available: 1.3.0
```

next time they open app.

---

## 24. Important Implementation Rule

Do **not** implement as:

```text
Git commit detected
↓
Download random files from repository
↓
Replace application
```

Instead use:

```text
Git tag
↓
CI build
↓
GitHub Release
↓
Release assets
↓
Version/checksum metadata
↓
Application checks latest release
↓
User approves update
↓
Download
↓
Verify
↓
Install
```

Gives much safer, predictable update mechanism.

---

## 25. Definition of Done

* [ ] Application has a centralized version.
* [ ] Version follows Semantic Versioning.
* [ ] Git tags are used for releases.
* [ ] CI automatically builds releases.
* [ ] GitHub Release contains application artifacts.
* [ ] Application checks for updates on startup.
* [ ] Update checking is asynchronous.
* [ ] Version comparison is semantic.
* [ ] User receives a clear update notification.
* [ ] User can install the update.
* [ ] User can postpone the update.
* [ ] User can open the release page.
* [ ] Update download supports progress.
* [ ] Download integrity is verified.
* [ ] Offline mode continues working normally.
* [ ] Update failures do not break the application.
* [ ] Mandatory updates are supported.
* [ ] Update state is persisted.
* [ ] "Check for Updates" is available manually.
* [ ] Automated tests cover the updater.
* [ ] CI/CD automatically publishes new releases.
* [ ] Existing user data is protected during upgrades.

## Expected Result

After implementation, complete experience:

```text
Developer
   │
   │ git tag v1.4.0
   ▼
GitHub
   │
   ├── CI Build
   ├── Tests
   ├── Package
   ├── SHA-256
   └── Release v1.4.0
            │
            ▼
       User opens app
            │
            ▼
       Check latest version
            │
            ▼
      1.3.0 < 1.4.0
            │
            ▼
    ┌───────────────────────┐
    │ New version available │
    │                       │
    │ Current: 1.3.0        │
    │ Latest:  1.4.0        │
    │                       │
    │ [Install Update]      │
    │ [View Release] [Later]│
    └───────────────────────┘
            │
            ▼
       Download update
            │
            ▼
       Verify checksum
            │
            ▼
       Install update
            │
            ▼
       Restart application
            │
            ▼
          1.4.0
```