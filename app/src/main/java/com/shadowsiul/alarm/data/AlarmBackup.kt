package com.shadowsiul.alarm.data

import android.app.backup.BackupManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AlarmBackup {
    const val FILE_NAME = "shadowsiul-alarms.json"
    private const val VERSION = 1

    fun encode(alarms: List<AlarmEntity>): String {
        val array = JSONArray()
        for (alarm in alarms) {
            array.put(
                JSONObject().apply {
                    put("hour", alarm.hour)
                    put("minute", alarm.minute)
                    put("label", alarm.label)
                    put("enabled", alarm.enabled)
                    put("repeatDays", alarm.repeatDays)
                    put("soundUri", alarm.soundUri ?: JSONObject.NULL)
                    put("soundName", alarm.soundName)
                    put("vibrate", alarm.vibrate)
                    put("ringOnHolidays", alarm.ringOnHolidays)
                },
            )
        }
        return JSONObject()
            .put("version", VERSION)
            .put("alarms", array)
            .toString()
    }

    fun decode(json: String): List<AlarmEntity> {
        val root = JSONObject(json)
        val array = root.optJSONArray("alarms") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    AlarmEntity(
                        hour = item.optInt("hour"),
                        minute = item.optInt("minute"),
                        label = item.optString("label"),
                        enabled = item.optBoolean("enabled", true),
                        repeatDays = item.optInt("repeatDays"),
                        soundUri = if (item.isNull("soundUri")) {
                            null
                        } else {
                            item.optString("soundUri").ifBlank { null }
                        },
                        soundName = item.optString("soundName", "Default"),
                        vibrate = item.optBoolean("vibrate", true),
                        ringOnHolidays = item.optBoolean("ringOnHolidays"),
                    ),
                )
            }
        }
    }

    fun persist(context: Context, alarms: List<AlarmEntity>) {
        val json = encode(alarms)
        runCatching { internalFile(context).writeText(json) }
        runCatching { writeDownloads(context, json) }
        runCatching { BackupManager(context).dataChanged() }
    }

    fun read(context: Context): List<AlarmEntity>? {
        val sources = listOfNotNull(
            runCatching { readDownloads(context) }.getOrNull(),
            runCatching { internalFile(context).takeIf { it.exists() }?.readText() }.getOrNull(),
        )
        return sources.firstNotNullOfOrNull { text ->
            runCatching { decode(text).takeIf { it.isNotEmpty() } }.getOrNull()
        }
    }

    private fun internalFile(context: Context) = File(context.filesDir, FILE_NAME)

    private fun writeDownloads(context: Context, json: String) {
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val existing = findDownloadUri(context)
            val uri = existing ?: resolver.insert(
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL),
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                },
            ) ?: return
            resolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
        } else {
            @Suppress("DEPRECATION")
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)
            file.parentFile?.mkdirs()
            file.writeText(json)
        }
    }

    private fun readDownloads(context: Context): String? {
        if (Build.VERSION.SDK_INT >= 29) {
            val uri = findDownloadUri(context) ?: return null
            return context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        }
        @Suppress("DEPRECATION")
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)
        return file.takeIf { it.exists() }?.readText()
    }

    private fun findDownloadUri(context: Context): android.net.Uri? {
        if (Build.VERSION.SDK_INT < 29) return null
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(FILE_NAME),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                return ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }
}
