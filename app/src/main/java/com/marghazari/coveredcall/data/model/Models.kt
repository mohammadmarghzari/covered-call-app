package com.marghazari.coveredcall.data.model

data class OptionContract(
    val symbol: String,
    val underlyingSymbol: String,
    val type: OptionType,
    val strikePrice: Long,
    val premium: Long,
    val underlyingPrice: Long,
    val expiryDate: String,
    val daysToExpiry: Int,
    val openInterest: Long = 0,
    val contractSize: Int = 1000,
    val category: String = "سایر"
)

enum class OptionType { CALL, PUT }

data class CoveredCallResult(
    val contract: OptionContract,
    val totalCostPerLot: Long,
    val premiumIncomePerLot: Long,
    val breakEvenPrice: Long,
    val maxProfitIfExercised: Long,
    val staticReturnPercent: Double,
    val annualizedReturnPercent: Double,
    val downsideProtectionPercent: Double
)

data class CommodityContract(
    val symbol: String,
    val commodityName: String,
    val contractType: String,
    val price: Long,
    val settlementDate: String,
    val volume: Long = 0
)

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val isSubscribed: Boolean = false,
    val subscriptionExpiryMillis: Long = 0L
)

enum class ReceiptStatus { PENDING, APPROVED, REJECTED }

data class ReceiptSubmission(
    val id: String = "",
    val uid: String = "",
    val imageUrl: String = "",
    val submittedAtMillis: Long = 0L,
    val amountToman: Long = 100_000,
    val status: ReceiptStatus = ReceiptStatus.PENDING,
    val reviewedAtMillis: Long? = null
)

/** پیام کاربر در بخش سوالات و انتقادات. پاسخ ادمین در فیلد reply قرار می‌گیرد. */
data class FeedbackMessage(
    val id: String = "",
    val uid: String = "",
    val email: String = "",
    val message: String = "",
    val submittedAtMillis: Long = 0L,
    val reply: String = "",
    val repliedAtMillis: Long = 0L
)

/** نتیجه فرمول‌یاب: یک قرارداد کاورد کال که به سود هدف کاربر می‌رسد، همراه با استدلال. */
data class TargetMatch(
    val result: CoveredCallResult,
    val monthlyReturnPercent: Double,
    val projectedReturnPercent: Double,
    val fitsHorizon: Boolean,
    val reasoning: String
)
