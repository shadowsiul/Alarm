package com.shadowsiul.alarm.alarm

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
                stopAlarmSound()
                AlarmNotifications.ringing = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                AlarmNotifications.cancelUpcoming(this)
                if (alarmId >= 0) {
                    scope.launch(Dispatchers.IO) {
                        AlarmRepository(this@AlarmRingtoneService).disableOneShot(alarmId)
                    }
                }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                stopAlarmSound()
                AlarmNotifications.ringing = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                if (alarmId >= 0) snooze(alarmId)
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
                ),
            )
            playSound(alarm?.soundUri)
            if (alarm?.vibrate != false) vibrate()
            val ring = Intent(this@AlarmRingtoneService, AlarmRingingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            }
            startActivity(ring)
        }
        return START_STICKY
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

    private fun snooze(alarmId: Long) {
        val triggerAt = System.currentTimeMillis() + 10 * 60 * 1000
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
    }
}
