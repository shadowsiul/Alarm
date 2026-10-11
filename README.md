# Alarm

Android alarm clock. Completely vibe-coded with Grok and Claude.

Version 1.5. Requires Android 8.0 (API 26) or later.

## What it does

Set one-time or repeating alarms with a label, custom sound, and vibrate. Alarms fire with a lock-screen screen, sound, and notification so you can dismiss or snooze.

Wake-up alarms stay silent on observed US federal holidays unless you turn on **Ring on holidays** (useful for bedtime alarms).

## Features

- Create, edit, enable/disable, and delete alarms
- Repeat on chosen weekdays, every day, or once
- Custom ringtone and vibrate
- Per-alarm snooze length: 5, 10, or 15 minutes
- First snooze is free; snoozing again requires a simple math question
- Auto dismiss after 10 or 15 minutes, or leave it ringing until you stop it
- Upcoming-alarm notification about 6 hours before it rings, with Dismiss and Turn off
- US federal holiday calendar (OPM observed dates) and a holiday banner on those days
- English, Spanish, and Chinese
- Local backup to Downloads as `alarms-bk.json`, plus Google backup so alarms can return after reinstall

## Build

From the project folder:

```powershell
.\gradlew.bat assembleDebug
```

APK output:

`app\build\outputs\apk\debug\app-debug.apk`

Install on a connected device or emulator:

```powershell
.\gradlew.bat installDebug
```

Allow notifications and exact alarms when the app asks. Open it once after creating alarms so the backup file is written.
