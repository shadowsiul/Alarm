package com.shadowsiul.alarm.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class AppLanguage(val tag: String, val nativeLabel: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    CHINESE("zh", "中文"),
    ;

    companion object {
        fun current(): AppLanguage {
            val tag = AppCompatDelegate.getApplicationLocales()[0]?.language
            return entries.find { it.tag == tag } ?: ENGLISH
        }

        fun apply(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.tag),
            )
        }
    }
}
