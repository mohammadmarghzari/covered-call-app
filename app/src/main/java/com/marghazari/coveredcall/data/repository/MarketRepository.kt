package com.marghazari.coveredcall.data.repository

import com.marghazari.coveredcall.data.model.CommodityContract
import com.marghazari.coveredcall.data.model.OptionContract
import com.marghazari.coveredcall.data.model.OptionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

interface MarketRepository {
    fun observeOptionContracts(): Flow<List<OptionContract>>
    fun observeCommodityContracts(): Flow<List<CommodityContract>>
}

class MockMarketRepository : MarketRepository {

    private val baseOptions = listOf(
        OptionContract("ضخود7014", "خودرو", OptionType.CALL, 3200, 410, 3450, "1404/09/15", 42),
        OptionContract("ضخود7015", "خودرو", OptionType.CALL, 3500, 260, 3450, "1404/09/15", 42),
        OptionContract("ضفولاد3021", "فولاد", OptionType.CALL, 8200, 890, 8600, "1404/10/10", 67),
        OptionContract("ضفملی2011", "فملی", OptionType.CALL, 12500, 1100, 12800, "1404/08/20", 16),
        OptionContract("ضشتران4051", "شتران", OptionType.CALL, 2100, 180, 2050, "1404/09/30", 57),
        OptionContract("طخود7014", "خودرو", OptionType.PUT, 3200, 150, 3450, "1404/09/15", 42),
        OptionContract("ضبانک1044", "وبملت", OptionType.CALL, 4200, 520, 4600, "1404/11/05", 93)
    )

    private val baseCommodities = listOf(
        CommodityContract("سکه‌آتی۰۹", "سکه امامی", "آتی", 452_000_000, "1404/09/25", 1200),
        CommodityContract("زعفران‌نگین۱", "زعفران نگین", "سلف موازی", 128_500_000, "1404/10/01", 340),
        CommodityContract("فولاد‌شمش۲", "شمش فولادی", "نقدی", 262_000_000, "1404/09/12", 890),
        CommodityContract("نقره‌آتی۰۳", "نقره", "آتی", 38_900_000, "1404/09/18", 210)
    )

    override fun observeOptionContracts(): Flow<List<OptionContract>> = flow {
        while (true) {
            emit(baseOptions.map { it.copy(premium = jitter(it.premium)) })
            delay(4000)
        }
    }

    override fun observeCommodityContracts(): Flow<List<CommodityContract>> = flow {
        while (true) {
            emit(baseCommodities.map { it.copy(price = jitter(it.price)) })
            delay(4000)
        }
    }

    private fun jitter(value: Long): Long {
        val percent = Random.nextDouble(-0.015, 0.015)
        return (value * (1 + percent)).toLong()
    }
}
