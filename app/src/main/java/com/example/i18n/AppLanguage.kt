package com.example.i18n

import java.util.Locale

enum class AppLanguage(
    val code: String,
    val shortLabel: String,
    val nativeName: String
) {
    RUSSIAN("ru", "RU", "Русский"),
    GEORGIAN("ka", "ქარ", "ქართული");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: defaultFromLocale()
        }

        fun defaultFromLocale(): AppLanguage {
            val lang = Locale.getDefault().language
            return if (lang.equals("ka", ignoreCase = true)) GEORGIAN else RUSSIAN
        }
    }
}
