package com.marghazari.coveredcall.domain

import com.marghazari.coveredcall.data.model.CoveredCallResult
import com.marghazari.coveredcall.data.model.OptionContract
import com.marghazari.coveredcall.data.model.OptionType

object CoveredCallCalculator {

    fun calculate(contract: OptionContract): CoveredCallResult? {
        if (contract.type != OptionType.CALL) return null
        if (contract.daysToExpiry <= 0) return null

        val lotSize = contract.contractSize
        val totalCost = contract.underlyingPrice * lotSize
        val premiumIncome = contract.premium * lotSize

        val breakEven = contract.underlyingPrice - contract.premium

        val profitPerShareIfExercised = (contract.strikePrice - contract.underlyingPrice) + contract.premium
        val maxProfit = profitPerShareIfExercised * lotSize

        val staticReturnPercent = if (totalCost > 0) {
            (premiumIncome.toDouble() / totalCost.toDouble()) * 100.0
        } else 0.0

        val annualizedReturnPercent = if (contract.daysToExpiry > 0) {
            staticReturnPercent * (365.0 / contract.daysToExpiry)
        } else 0.0

        val downsideProtectionPercent = if (contract.underlyingPrice > 0) {
            (contract.premium.toDouble() / contract.underlyingPrice.toDouble()) * 100.0
        } else 0.0

        return CoveredCallResult(
            contract = contract,
            totalCostPerLot = totalCost,
            premiumIncomePerLot = premiumIncome,
            breakEvenPrice = breakEven,
            maxProfitIfExercised = maxProfit,
            staticReturnPercent = staticReturnPercent,
            annualizedReturnPercent = annualizedReturnPercent,
            downsideProtectionPercent = downsideProtectionPercent
        )
    }

    fun rankBestContracts(contracts: List<OptionContract>): List<CoveredCallResult> {
        return contracts
            .filter { it.type == OptionType.CALL }
            .mapNotNull { calculate(it) }
            .filter { it.staticReturnPercent > 0 }
            .sortedByDescending { it.annualizedReturnPercent }
    }

    fun profitLossCurve(
        contract: OptionContract,
        priceRangePercent: Double = 0.30,
        steps: Int = 40
    ): List<Pair<Long, Long>> {
        val basePrice = contract.underlyingPrice
        val minPrice = (basePrice * (1 - priceRangePercent)).toLong()
        val maxPrice = (basePrice * (1 + priceRangePercent)).toLong()
        val stepSize = ((maxPrice - minPrice) / steps).coerceAtLeast(1)

        val points = mutableListOf<Pair<Long, Long>>()
        var price = minPrice
        while (price <= maxPrice) {
            val stockPnl = (price - basePrice) * contract.contractSize
            val optionPnl = if (price > contract.strikePrice) {
                (contract.strikePrice - price) * contract.contractSize
            } else 0L
            val premiumPnl = contract.premium * contract.contractSize
            val totalPnl = stockPnl + optionPnl + premiumPnl
            points.add(price to totalPnl)
            price += stepSize
        }
        return points
    }
}
