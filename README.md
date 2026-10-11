# Alarm

An Android alarm clock. Completely vibe-coded with Grok and Claude.

Works on Android 8 and newer.

## Using it

Add alarms from the list. Each one can repeat on certain days or run once. You can set a label, sound, vibrate, snooze length (5, 10, or 15 minutes), and whether it should stop on its own after 10 or 15 minutes.

When it rings, dismiss it or snooze. The first snooze is open. After that you have to solve a short math problem to snooze again.

By default, alarms stay quiet on US federal holidays. Turn on **Ring on holidays** for a bedtime alarm that should still go off.

About 6 hours before the next alarm, a notification appears so you can dismiss that time or turn the alarm off.

Menus are available in English, Spanish, and Chinese.

## Saving your alarms

Alarms live on the phone. Whenever they change, the app also writes `alarms-bk.json` to Downloads. After an uninstall, open the app once and it will try to load that file if the list is empty.

If the phone has Google Backup turned on, Android may also restore the alarm database when you reinstall from the Play Store on the same account. That is automatic. There is no backup button in the app.
