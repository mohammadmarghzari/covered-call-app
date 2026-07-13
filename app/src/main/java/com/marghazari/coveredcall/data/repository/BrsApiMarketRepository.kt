package com.marghazari.coveredcall.data.repository

import com.marghazari.coveredcall.BuildConfig
import com.marghazari.coveredcall.data.model.CommodityContract
import com.marghazari.coveredcall.data.model.OptionContract
import com.marghazari.coveredcall.data.model.OptionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * دریافت داده زنده بازار از BrsApi.ir
 * - بازار آپشن بورس تهران: https://brsapi.ir/bourse-api-option-webservice/
 * - قیمت لحظه‌ای طلا و سکه (به‌عنوان کالاهای بازار کالا): https://brsapi.ir/free-api-gold-currency-crypto/
 *
 * اگر کلید API خالی باشد یا سرویس در دسترس نباشد، لیست خالی برگردانده می‌شود
 * و صفحه مربوطه پیام «داده‌ای موجود نیست» را نمایش می‌دهد.
 */
class BrsApiMarketRepository : MarketRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun observeOptionContracts(): Flow<List<OptionContract>> = flow {
        while (true) {
            val contracts = try {
                fetchOptionContracts()
            } catch (e: Exception) {
                emptyList()
            }
            emit(contracts)
            delay(20_000) // هر ۲۰ ثانیه یک‌بار به‌روزرسانی
        }
    }

    override fun observeCommodityContracts(): Flow<List<CommodityContract>> = flow {
        while (true) {
            val contracts = try {
                fetchCommodityContracts()
            } catch (e: Exception) {
                emptyList()
            }
            emit(contracts)
            delay(20_000)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // آپشن
    // ---------------------------------------------------------------------------------------------

    private suspend fun fetchOptionContracts(): List<OptionContract> = withContext(Dispatchers.IO) {
        val key = BuildConfig.BRSAPI_KEY
        if (key.isBlank()) return@withContext emptyList()

        val url = "https://BrsApi.ir/Api/Tsetmc/Option.php?key=$key"
        val body = httpGet(url) ?: return@withContext emptyList()
        parseOptionContracts(body)
    }

    private fun parseOptionContracts(rawJson: String): List<OptionContract> {
        val jsonArray = extractArray(rawJson, "data", "result")
        val list = mutableListOf<OptionContract>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue
            val typeRaw = item.optString("type", "call")
            val optionType = if (typeRaw.equals("put", ignoreCase = true)) OptionType.PUT else OptionType.CALL

            val symbol = item.optString("l18", "")
            val underlyingSymbol = item.optString("base_l18", "")
            val category = item.optString("cs", "سایر")
            val strike = item.optDouble("price_strike", 0.0).toLong()
            val premium = item.optDouble("pc", item.optDouble("pl", 0.0)).toLong()
            val underlyingPrice = item.optDouble("base_pl", item.optDouble("base_pc", 0.0)).toLong()
            val daysRemain = item.optInt("day_remain", 0)
            val expiry = item.optString("date_end", "")
            val contractSize = item.optInt("size_contract", 1000)
            val openInterest = item.optLong("interest_open", 0L)

            if (symbol.isBlank() || strike <= 0) continue

            list.add(
                OptionContract(
                    symbol = symbol,
                    underlyingSymbol = underlyingSymbol,
                    type = optionType,
                    strikePrice = strike,
                    premium = premium,
                    underlyingPrice = underlyingPrice,
                    expiryDate = expiry,
                    daysToExpiry = daysRemain,
                    openInterest = openInterest,
                    contractSize = contractSize,
                    category = category
                )
            )
        }
        return list
    }

    // ---------------------------------------------------------------------------------------------
    // بازار کالا (طلا و سکه)
    // ---------------------------------------------------------------------------------------------

    private suspend fun fetchCommodityContracts(): List<CommodityContract> = withContext(Dispatchers.IO) {
        val key = BuildConfig.BRSAPI_KEY
        if (key.isBlank()) return@withContext emptyList()

        val url = "https://BrsApi.ir/Api/Market/Gold_Currency.php?key=$key"
        val body = httpGet(url) ?: return@withContext emptyList()
        parseCommodityContracts(body)
    }

    private fun parseCommodityContracts(rawJson: String): List<CommodityContract> {
        val trimmed = rawJson.trim()
        // پاسخ معمولاً یک آبجکت با آرایه‌های gold / currency / cryptocurrency است.
        // فقط طلا و سکه (کالاها) را نگه می‌داریم.
        val goldArray: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("gold")
                    ?: obj.optJSONArray("data")
                    ?: obj.optJSONArray("result")
                    ?: JSONArray()
            }
        }

        val list = mutableListOf<CommodityContract>()
        for (i in 0 until goldArray.length()) {
            val item = goldArray.optJSONObject(i) ?: continue

            val name = firstNonBlank(
                item.optString("name", ""),
                item.optString("name_fa", ""),
                item.optString("name_en", "")
            )
            val symbol = firstNonBlank(
                item.optString("symbol", ""),
                item.optString("code", ""),
                name
            )
            val price = parseNumber(item, "price", "value", "pl", "p")
            val unit = firstNonBlank(item.optString("unit", ""), "تومان")
            val date = firstNonBlank(
                item.optString("date", ""),
                item.optString("time", ""),
                "زنده"
            )

            if (name.isBlank() || price <= 0) continue

            list.add(
                CommodityContract(
                    symbol = symbol,
                    commodityName = name,
                    contractType = unit,
                    price = price,
                    settlementDate = date,
                    volume = 0
                )
            )
        }
        return list
    }

    // ---------------------------------------------------------------------------------------------
    // ابزارهای مشترک
    // ---------------------------------------------------------------------------------------------

    private fun httpGet(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "application/json, text/plain, */*")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    private fun extractArray(rawJson: String, vararg objectKeys: String): JSONArray {
        val trimmed = rawJson.trim()
        return when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> {
                val obj = JSONObject(trimmed)
                for (key in objectKeys) {
                    obj.optJSONArray(key)?.let { return it }
                }
                JSONArray()
            }
        }
    }

    private fun firstNonBlank(vararg values: String): String {
        for (v in values) if (v.isNotBlank()) return v
        return ""
    }

    /** مقدار عددی را می‌خواند؛ چه عدد خام باشد چه رشته‌ای با جداکننده هزارگان. */
    private fun parseNumber(item: JSONObject, vararg keys: String): Long {
        for (key in keys) {
            if (!item.has(key) || item.isNull(key)) continue
            val direct = item.optDouble(key, Double.NaN)
            if (!direct.isNaN()) return direct.toLong()
            val raw = item.optString(key, "").replace(",", "").replace(" ", "").trim()
            val parsed = raw.toDoubleOrNull()
            if (parsed != null) return parsed.toLong()
        }
        return 0L
    }
}
