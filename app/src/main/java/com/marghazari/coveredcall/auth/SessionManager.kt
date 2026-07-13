package com.marghazari.coveredcall.auth

import android.content.Context

/** ذخیره‌ی توکن ورود روی خود گوشی تا کاربر هر بار مجبور به ورود نباشد. */
class SessionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("coveredcall_session", Context.MODE_PRIVATE)

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
    }
}
