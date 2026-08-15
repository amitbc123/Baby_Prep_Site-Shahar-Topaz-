# App Auto-Update & Version Control

## Goal

Add an automatic **version control and update notification system** to the application.

Whenever a new application version is committed/released to the Git repository, users should be notified when they open the app and offered the option to install the latest version.

The system must be designed to be reliable, secure, easy to maintain, and avoid forcing updates unless explicitly configured as mandatory.

---

## 1. Version Management

The application must have a single source of truth for its current version.

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

The version should be automatically updated as part of the release process rather than manually changing it in multiple files.

### Requirements

* Store the application version in one central location.
* The running application must be able to expose its current version.
* Git tags should be used for releases.
* Recommended tag format:

```text
v1.0.0
v1.1.0
v1.2.0
```

---

## 2. Git Repository as Update Source

Use the Git repository/release system as the source for update information.

Prefer **GitHub Releases** if the project is hosted on GitHub.

Each release should contain:

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

The application should query the repository's release information when it starts.

Do NOT depend on scraping normal Git commits or HTML pages.

Use a proper release/API mechanism.

---

## 3. Startup Update Check

When the user launches the application:

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

The update check should happen asynchronously so it does not block application startup.

The application must remain usable if the update server/Git repository is unavailable.

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

Do not compare versions as strings.

Correctly handle:

```text
1.9.0 < 1.10.0
```

The system should also support pre-release versions if appropriate:

```text
1.3.0-beta.1
1.3.0-rc.1
```

By default, stable releases should be compared against stable releases.

---

## 5. Update Notification

When a newer version exists, display a clear notification.

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

Starts the update process.

#### View Release

Opens the GitHub release page in the user's browser.

#### Later

Closes the notification.

The application should remember that the user dismissed the notification and avoid repeatedly showing the same notification during every startup.

---

## 6. Update Download

When the user selects:

```text
Install Update
```

the application should:

1. Determine the correct release asset for the user's platform.
2. Download the update.
3. Validate the downloaded file.
4. Install/update the application.
5. Restart the application if required.

Show download progress:

```text
Downloading update...

████████████████░░░░ 80%

Version 1.3.0
```

The UI must clearly communicate errors if the download fails.

---

## 7. Security

Do not blindly execute arbitrary files downloaded from Git.

The update system should support integrity verification.

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

For stronger security, support signed releases/artifacts where practical.

Never install an update if:

* The download is corrupted.
* The checksum does not match.
* The release metadata is invalid.
* The downloaded file is not the expected platform/package.

---

## 8. Release Manifest

Create a machine-readable release manifest.

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

The manifest should only contain information required by the application.

---

## 9. Release Automation

Create a release workflow in GitHub Actions.

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

A release should only be created after the application passes the required CI checks.

---

## 10. Automatic Versioning

Prefer automated version generation from Git tags.

Example:

```text
git tag v1.4.0
git push origin v1.4.0
```

The CI pipeline should:

1. Detect the tag.
2. Extract `1.4.0`.
3. Build the application using that version.
4. Package the application.
5. Generate checksums.
6. Create the GitHub Release.
7. Upload the application artifacts.

Avoid having to manually edit the version in multiple source files.

---

## 11. Update Check Frequency

Do not query GitHub repeatedly during application usage.

Recommended:

* Check once on application startup.
* Cache the result.
* Do not check more than once every few hours unless the user manually selects "Check for Updates".

Add a menu item:

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

The application must work normally without Internet access.

If the update check fails:

```text
Unable to check for updates.
```

Do not prevent the application from starting.

Do not display a scary error for a normal network failure.

Log the failure for diagnostics.

---

## 13. Mandatory Updates

Support an optional mandatory-update flag.

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

The `Later` button should not be available for mandatory updates.

Use mandatory updates only when necessary.

---

## 14. Skipping Versions

The updater must support users upgrading directly from older versions.

Example:

```text
Installed: 1.0.0
Latest:    1.5.0
```

The application should offer:

```text
1.0.0 → 1.5.0
```

rather than requiring:

```text
1.0.0 → 1.1.0 → 1.2.0 → 1.3.0 → 1.4.0 → 1.5.0
```

Unless the application has a specific migration requirement.

---

## 15. Database/Data Migration

If a new version changes the application's data format, add migration support.

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

Never allow an update to silently destroy or invalidate existing user data.

Before migrations:

* Create backups when appropriate.
* Validate the existing data.
* Run migrations.
* Verify the result.
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

This prevents repeatedly notifying the user about the same release.

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

Errors should never leave the application in a broken state.

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

Do not log sensitive information such as tokens or credentials.

---

## 19. UI/UX Requirements

The updater should feel like a native part of the application.

Do not show technical GitHub/API information to normal users.

Use friendly messages:

```text
A new version is available.
```

instead of:

```text
GET /repos/xxx/releases/latest returned 200.
```

The notification should clearly show:

* Current version.
* New version.
* Release notes.
* Update button.
* Later button.
* Release page option.

---

## 20. Architecture

Create a dedicated update service/module.

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

Returns the currently installed version.

### ReleaseChecker

Checks the GitHub release/manifest.

### VersionComparator

Determines whether the remote version is newer.

### DownloadManager

Downloads the correct update package.

### IntegrityVerifier

Validates SHA-256/signature.

### UpdateInstaller

Performs the actual installation.

### UpdateState

Stores update-check and notification state.

### UpdateNotification

Displays the update UI.

Keep these components separated so the updater can be tested independently.

---

## 21. Configuration

Keep repository information configurable rather than hard-coded throughout the application.

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

The default channel should be:

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

The intended developer workflow should be as simple as:

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

GitHub Actions should handle the rest:

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

the next time they open the application.

---

## 24. Important Implementation Rule

Do **not** implement this as:

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

This provides a much safer and more predictable update mechanism.

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

After implementation, the complete experience should be:

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
