package com.marghazari.coveredcall.data.repository

import com.marghazari.coveredcall.BuildConfig
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
import java.util.concurrent.TimeUnit

/**
 * دریافت داده زنده بازار آپشن بورس تهران از BrsApi.ir
 * مستندات: https://brsapi.ir/bourse-api-option-webservice/
 */
class BrsApiOptionRepository : MarketRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun observeOptionContracts(): Flow<List<OptionContract>> = flow {
        while (true) {
            val contracts = try {
                fetchContracts()
            } catch (e: Exception) {
                emptyList()
            }
            emit(contracts)
            delay(20_000) // هر ۲۰ ثانیه یک‌بار به‌روزرسانی
        }
    }

    override fun observeCommodityContracts() = kotlinx.coroutines.flow.flowOf(emptyList<com.marghazari.coveredcall.data.model.CommodityContract>())

    private suspend fun fetchContracts(): List<OptionContract> = withContext(Dispatchers.IO) {
        val key = BuildConfig.BRSAPI_KEY
        if (key.isBlank()) return@withContext emptyList()

        val url = "https://BrsApi.ir/Api/Tsetmc/Option.php?key=$key"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept", "application/json, text/plain, */*")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return@withContext emptyList()
            parseContracts(body)
        }
    }

    private fun parseContracts(rawJson: String): List<OptionContract> {
        val trimmed = rawJson.trim()
        val jsonArray: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> {
                val obj = org.json.JSONObject(trimmed)
                obj.optJSONArray("data") ?: obj.optJSONArray("result") ?: JSONArray()
            }
        }

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
}
