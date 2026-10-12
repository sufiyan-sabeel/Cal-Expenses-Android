# Cal-Expenses v1.3

An offline-first Android expense tracker with voice notes and task management.
Created by **Umaiz Sufiyan**.

## What it does

- **Expenses** — add, edit, delete, categorise and search transactions (expense or income).
- **Dashboard** — month balance, income/expense totals, 6-month bar chart, category donut, budget progress and recent transactions.
- **Budgets** — monthly per-category limits with spent / remaining and progress bars.
- **Voice notes** — record audio locally, in-app speech-to-text with an editable result, and a manual-typing fallback that always works.
- **Tasks** — titles, due dates, checklists and local reminder notifications.
- **Reports** — per-month summary, 6-month trend, weekly breakdown and category breakdown, plus a **print-quality multi-page PDF report** (summary, category breakdown, budget vs actual, full transaction ledger with repeated table headers, page numbers and footer) for either the selected month or all transactions.
- **Goals** — savings goals with target amount, saved amount, percentage progress, remaining amount, optional target date, an "add money" action and a completion state.
- **Cal AI** — an optional chat assistant backed by OpenRouter (`openrouter/free`, the free models router). Conversations are stored locally; the API key is entered at runtime and stored encrypted on-device.
- **Settings** — AMOLED pure-black toggle, currency choice, JSON backup export/import, CSV export, and clear-chat-history.

## Cal AI and your API key

Cal AI needs an OpenRouter API key, which **you enter in the app** (tap "Set key" on the Cal AI screen).
It is stored using `EncryptedSharedPreferences`, backed by the Android Keystore, and is **never**
placed in the APK, the source tree, or any downloadable file. Only your typed message — and, only if
you switch on the spending-summary toggle, a short summary of your totals — is sent to OpenRouter.
Every other feature works fully offline with no key and no account.

Note: OpenRouter's free tier is rate-limited and some free models may use prompts for training; check
the terms of whichever free model the router selects.

## Privacy

No account, no ads, no analytics, no tracking, no paid services. All data is stored
locally in a Room (SQLite) database on the device. Backups are plain files you choose
through the system file picker (no storage permission required).

## Design system (v1.3)

Rebuilt around a single token set, with a full light and a full dark theme (switchable in Settings as
System default / Light / Dark).

- Light: background `#F3F7F3`, card `#FFFFFF`, primary green `#087F5B`, text `#17211C` / `#68756D`, border `#E4EBE5`, income `#16834A`, expense `#C45A43`, accent `#3B82B8`
- Dark: background `#101512`, card `#191F1B`, elevated `#222A24`, primary `#35C58A`, text `#F1F5F2` / `#A0ADA4`, border `#2D3931`
- 8dp spacing scale, 20-24dp card radii, pill chips, subtle 1dp borders, line icons

Navigation is Home, Transactions, Budgets, Goals, Settings; Cal AI, Notes, Tasks and Reports live in
the toolbar. Transactions are grouped by date with directional income/expense icons and filter chips
(All / Expenses / Income / Transfers). Transfers are a distinct type and are excluded from income and
expense totals so nothing is double-counted.

## Tech

- Kotlin, single-activity + fragments, Material 3 (dark graphite / AMOLED theme)
- Room (KSP) for persistence
- Custom canvas chart views (no third-party chart library)
- MediaRecorder for audio, Android SpeechRecognizer for dictation, AlarmManager for reminders
- minSdk 24, targetSdk / compileSdk 34

## Build it yourself

Requirements: JDK 17 and the Android SDK (platform 34, build-tools 34.0.0).

```bash
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

You can also open the project in Android Studio and press Run.

## Install the APK

1. Copy `cal-expenses-1.0-debug.apk` to your phone.
2. Allow installation from unknown sources for the app you open it with.
3. Tap the file to install, then launch **Cal-Expenses**.

## Notes

- The PDF report is generated entirely on-device with Android's built-in PdfDocument — no library, no network, and no AI connection needed.
- Upgrading from v1.0 keeps your existing data (a validated Room migration adds the chat tables).
- The bundled APK is a **debug** build signed with the standard Android debug key,
  which is fine for personal use and sideloading. For Play Store distribution you
  would add your own release signing config.
- Speech recognition depends on the device having a recognition service; when it is
  unavailable the app tells you and you can type instead.
