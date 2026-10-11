package com.shadowsiul.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.shadowsiul.alarm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlarmRingtoneService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L) ?: -1L
        when (intent?.action) {
            ACTION_DISMISS -> {
                cancelAutoDismiss(alarmId)
                stopAlarmSound()
                AlarmNotifications.ringing = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                AlarmNotifications.cancelUpcoming(this)
                sendBroadcast(Intent(ACTION_STOP_RINGING_UI).setPackage(packageName))
                if (alarmId >= 0) {
                    scope.launch(Dispatchers.IO) {
                        AlarmRepository(this@AlarmRingtoneService).disableOneShot(alarmId)
                    }
                }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                cancelAutoDismiss(alarmId)
                stopAlarmSound()
                AlarmNotifications.ringing = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                sendBroadcast(Intent(ACTION_STOP_RINGING_UI).setPackage(packageName))
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10).let {
                    if (it in listOf(5, 10, 15)) it else 10
                }
                if (alarmId >= 0) snooze(alarmId, minutes)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        AlarmNotifications.ringing = true
        AlarmNotifications.cancelUpcoming(this)
        AlarmNotifications.ensureChannel(this)
        startForeground(
            AlarmNotifications.RINGING_NOTIFICATION_ID,
            AlarmNotifications.ringingNotification(
                this,
                alarmId,
                getString(com.shadowsiul.alarm.R.string.alarm_fallback),
                getString(com.shadowsiul.alarm.R.string.channel_alarm_desc),
            ),
        )

        scope.launch(Dispatchers.IO) {
            val alarm = AlarmRepository(this@AlarmRingtoneService).getAlarm(alarmId)
            val fallback = getString(com.shadowsiul.alarm.R.string.alarm_fallback)
            val label = alarm?.label?.ifBlank { fallback } ?: fallback
            val timeText = alarm?.let { "%02d:%02d".format(it.hour, it.minute) } ?: ""
            startForeground(
                AlarmNotifications.RINGING_NOTIFICATION_ID,
                AlarmNotifications.ringingNotification(
                    this@AlarmRingtoneService,
                    alarmId,
                    label,
                    timeText,
                    alarm?.snoozeMinutes ?: 10,
                ),
            )
            playSound(alarm?.soundUri)
            if (alarm?.vibrate != false) vibrate()
            val ring = Intent(this@AlarmRingtoneService, AlarmRingingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            }
            startActivity(ring)
            scheduleAutoDismiss(alarmId, alarm?.autoDismissMinutes ?: 0)
        }
        return START_STICKY
    }

    private fun scheduleAutoDismiss(alarmId: Long, minutes: Int) {
        cancelAutoDismiss(alarmId)
        if (alarmId < 0 || minutes <= 0) return
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + minutes * 60 * 1000L,
            autoDismissIntent(alarmId),
        )
    }

    private fun cancelAutoDismiss(alarmId: Long) {
        if (alarmId < 0) return
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        am.cancel(autoDismissIntent(alarmId))
    }

    private fun autoDismissIntent(alarmId: Long): PendingIntent {
        return PendingIntent.getService(
            this,
            5000 + alarmId.toInt(),
            Intent(this, AlarmRingtoneService::class.java).apply {
                action = ACTION_DISMISS
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun playSound(uriString: String?) {
        stopAlarmSound()
        val uri = uriString?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        try {
            player = MediaPlayer().apply {
                setDataSource(this@AlarmRingtoneService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            player = MediaPlayer.create(this, fallback)?.apply {
                isLooping = true
                start()
            }
        }
    }

    private fun vibrate() {
        vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 500, 500, 500), 0)
        vibrator?.vibrate(effect)
    }

    private fun snooze(alarmId: Long, minutes: Int) {
        val triggerAt = System.currentTimeMillis() + minutes * 60 * 1000L
        scope.launch(Dispatchers.IO) {
            AlarmRepository(this@AlarmRingtoneService).setSnooze(alarmId, triggerAt)
        }
    }

    private fun stopAlarmSound() {
        player?.runCatching {
            if (isPlaying) stop()
            release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        stopAlarmSound()
        AlarmNotifications.ringing = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_DISMISS = "com.shadowsiul.alarm.DISMISS"
        const val ACTION_SNOOZE = "com.shadowsiul.alarm.SNOOZE"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        const val ACTION_STOP_RINGING_UI = "com.shadowsiul.alarm.STOP_RINGING_UI"
    }
}
