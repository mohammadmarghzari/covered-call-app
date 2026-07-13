package com.marghazari.coveredcall.domain

import com.marghazari.coveredcall.data.model.CoveredCallResult
import com.marghazari.coveredcall.data.model.OptionContract
import com.marghazari.coveredcall.data.model.OptionType
import com.marghazari.coveredcall.data.model.TargetMatch

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

    /**
     * فرمول‌یاب: قراردادهایی را پیدا می‌کند که استراتژی کاورد کال آن‌ها به سود هدف کاربر
     * (targetReturnPercent) در بازه زمانی دلخواه (horizonMonths ماه) می‌رسد و برای هرکدام استدلال می‌سازد.
     *
     * منطق: بازده ایستای هر قرارداد تا سررسیدش محاسبه می‌شود، سپس به نرخ ماهانه تبدیل و
     * روی بازه هدف تصویر (project) می‌شود. قراردادهایی که تصویرشان به هدف می‌رسد نگه داشته می‌شوند.
     */
    fun findForTarget(
        contracts: List<OptionContract>,
        targetReturnPercent: Double,
        horizonMonths: Int
    ): List<TargetMatch> {
        if (targetReturnPercent <= 0 || horizonMonths <= 0) return emptyList()
        val horizonDays = horizonMonths * 30.0

        return rankBestContracts(contracts).mapNotNull { result ->
            val days = result.contract.daysToExpiry
            if (days <= 0) return@mapNotNull null

            val monthly = result.staticReturnPercent * (30.0 / days)
            val projected = result.staticReturnPercent * (horizonDays / days)
            if (projected < targetReturnPercent) return@mapNotNull null

            // اگر سررسید قرارداد نزدیک بازه هدف باشد، تخمین قابل‌اتکاتر است.
            val fitsHorizon = days >= horizonDays * 0.5 && days <= horizonDays * 1.5

            val fitNote = if (fitsHorizon) {
                "سررسید این قرارداد (${days} روز) به بازه‌ی هدف شما نزدیک است، پس این تخمین قابل‌اتکاست."
            } else {
                "سررسید این قرارداد ${days} روز است؛ عدد بالا با فرض تکرار همین بازده تا پایان بازه‌ی هدف محاسبه شده و صرفاً تخمینی است."
            }

            val reasoning = buildString {
                append("بازده ایستای این قرارداد تا سررسید %.1f%% است".format(result.staticReturnPercent))
                append(" (معادل حدود %.1f%% در ماه).".format(monthly))
                append(" با این نرخ، بازده تخمینی در ${horizonMonths} ماه حدود %.1f%% می‌شود".format(projected))
                append(" که از سود هدف شما (%.0f%%) بیشتر است.".format(targetReturnPercent))
                append(" همچنین پرمیوم دریافتی %.1f%% از قیمت سهم، در برابر ریزش قیمت از شما محافظت می‌کند.".format(result.downsideProtectionPercent))
                append(" ")
                append(fitNote)
            }

            TargetMatch(
                result = result,
                monthlyReturnPercent = monthly,
                projectedReturnPercent = projected,
                fitsHorizon = fitsHorizon,
                reasoning = reasoning
            )
        }.sortedWith(
            // اول قراردادهایی که سررسیدشان به بازه هدف نزدیک است، سپس بیشترین بازده تصویرشده
            compareByDescending<TargetMatch> { it.fitsHorizon }
                .thenByDescending { it.projectedReturnPercent }
        )
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
