package com.marghazari.coveredcall.data.remote

import com.marghazari.coveredcall.BuildConfig
import com.marghazari.coveredcall.data.model.AppUser
import com.marghazari.coveredcall.data.model.FeedbackMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Ok<T>(val data: T) : ApiResult<T>()
    data class Err(val message: String, val authExpired: Boolean = false) : ApiResult<Nothing>()
}

data class AuthResult(val token: String, val user: AppUser)

/**
 * ارتباط با بک‌اند خودمان (ورود موبایلی، اشتراک، سوالات). جایگزین Firebase.
 * آدرس سرور از BuildConfig.BACKEND_URL خوانده می‌شود (سکرت BACKEND_URL موقع build).
 */
class BackendClient(
    private val baseUrl: String = BuildConfig.BACKEND_URL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    val isConfigured: Boolean get() = baseUrl.isNotBlank()

    private fun url(path: String) = baseUrl.trimEnd('/') + path

    private data class HttpResp(val code: Int, val body: String)

    private fun postRaw(path: String, json: JSONObject, token: String?): HttpResp {
        val builder = Request.Builder()
            .url(url(path))
            .post(json.toString().toRequestBody(jsonMedia))
        if (token != null) builder.header("Authorization", "Bearer $token")
        client.newCall(builder.build()).execute().use { r ->
            return HttpResp(r.code, r.body?.string() ?: "")
        }
    }

    private fun getRaw(path: String, token: String?): HttpResp {
        val builder = Request.Builder().url(url(path)).get()
        if (token != null) builder.header("Authorization", "Bearer $token")
        client.newCall(builder.build()).execute().use { r ->
            return HttpResp(r.code, r.body?.string() ?: "")
        }
    }

    // ---------------------------------------------------------------------------------------------
    // احراز هویت
    // ---------------------------------------------------------------------------------------------

    suspend fun requestOtp(phone: String): ApiResult<Unit> = call {
        val r = postRaw("/auth/request-otp", JSONObject().put("phone", phone), null)
        if (r.code in 200..299) ApiResult.Ok(Unit) else errResult(r)
    }

    suspend fun verifyOtp(phone: String, code: String): ApiResult<AuthResult> = call {
        val body = JSONObject().put("phone", phone).put("code", code)
        val r = postRaw("/auth/verify-otp", body, null)
        if (r.code in 200..299) {
            val obj = JSONObject(r.body)
            ApiResult.Ok(AuthResult(obj.optString("token"), parseUser(obj)))
        } else errResult(r)
    }

    // ---------------------------------------------------------------------------------------------
    // کاربر و اشتراک
    // ---------------------------------------------------------------------------------------------

    suspend fun getMe(token: String): ApiResult<AppUser> = call {
        val r = getRaw("/me", token)
        if (r.code in 200..299) ApiResult.Ok(parseUser(JSONObject(r.body)))
        else errResult(r)
    }

    suspend fun redeemCode(token: String, code: String): ApiResult<AppUser> = call {
        val r = postRaw("/subscription/redeem", JSONObject().put("code", code), token)
        if (r.code in 200..299) ApiResult.Ok(parseUser(JSONObject(r.body)))
        else errResult(r)
    }

    // ---------------------------------------------------------------------------------------------
    // سوالات و انتقادات
    // ---------------------------------------------------------------------------------------------

    suspend fun submitFeedback(token: String, message: String): ApiResult<Unit> = call {
        val r = postRaw("/feedback", JSONObject().put("message", message), token)
        if (r.code in 200..299) ApiResult.Ok(Unit) else errResult(r)
    }

    suspend fun getFeedback(token: String): ApiResult<List<FeedbackMessage>> = call {
        val r = getRaw("/feedback", token)
        if (r.code !in 200..299) return@call errResult(r)
        val arr = JSONObject(r.body).optJSONArray("items")
        val list = mutableListOf<FeedbackMessage>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    FeedbackMessage(
                        id = o.optString("id"),
                        message = o.optString("message"),
                        reply = o.optString("reply"),
                        submittedAtMillis = o.optLong("submittedAt"),
                        repliedAtMillis = o.optLong("repliedAt")
                    )
                )
            }
        }
        ApiResult.Ok(list)
    }

    // ---------------------------------------------------------------------------------------------
    // کمکی‌ها
    // ---------------------------------------------------------------------------------------------

    private suspend fun <T> call(block: () -> ApiResult<T>): ApiResult<T> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext ApiResult.Err(NOT_CONFIGURED)
        try {
            block()
        } catch (e: Exception) {
            ApiResult.Err("خطا در اتصال به سرور: ${e.message ?: "اتصال اینترنت را بررسی کن"}")
        }
    }

    private fun parseUser(obj: JSONObject): AppUser {
        val u = obj.optJSONObject("user") ?: obj
        return AppUser(
            id = u.optString("id"),
            phone = u.optString("phone"),
            displayName = u.optString("displayName"),
            isSubscribed = u.optBoolean("isSubscribed"),
            subscriptionExpiryMillis = u.optLong("subscriptionExpiryMillis")
        )
    }

    private fun errResult(r: HttpResp): ApiResult.Err {
        val err = try {
            JSONObject(r.body).optString("error")
        } catch (e: Exception) {
            ""
        }
        val authExpired = r.code == 401 &&
            err in setOf("invalid_token", "user_not_found", "unauthorized")
        return ApiResult.Err(errorMessage(r.code, err), authExpired)
    }

    private fun errorMessage(code: Int, err: String): String {
        return when (err) {
            "invalid_phone" -> "شماره موبایل نامعتبر است."
            "too_soon" -> "کمی صبر کن، کد قبلی هنوز معتبر است."
            "sms_failed" -> "ارسال پیامک ناموفق بود. کمی بعد دوباره تلاش کن."
            "no_code" -> "کدی ارسال نشده. اول دکمه‌ی «ارسال کد» را بزن."
            "expired" -> "کد منقضی شده. دوباره کد بگیر."
            "wrong_code" -> "کد واردشده اشتباه است."
            "too_many_attempts" -> "تعداد تلاش زیاد شد. دوباره کد بگیر."
            "invalid_code", "code_not_found" -> "کد اشتراک نامعتبر است."
            "code_already_used" -> "این کد اشتراک قبلاً استفاده شده است."
            "empty_message" -> "متن پیام خالی است."
            "invalid_token", "user_not_found", "unauthorized" -> "نشست منقضی شده است. دوباره وارد شو."
            "" -> if (code >= 500) "خطای سرور ($code)." else "خطا ($code)."
            else -> "خطا: $err"
        }
    }

    private companion object {
        const val NOT_CONFIGURED =
            "آدرس سرور در اپ تنظیم نشده است. سکرت BACKEND_URL را ست کن و دوباره APK بساز."
    }
}
