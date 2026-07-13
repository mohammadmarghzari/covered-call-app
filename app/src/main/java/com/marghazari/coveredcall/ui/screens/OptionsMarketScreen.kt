package com.marghazari.coveredcall.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marghazari.coveredcall.data.model.CoveredCallResult
import com.marghazari.coveredcall.data.model.OptionContract
import com.marghazari.coveredcall.data.repository.MarketRepository
import com.marghazari.coveredcall.domain.CoveredCallCalculator

@Composable
fun OptionsMarketScreen(
    marketRepository: MarketRepository,
    onOpenPnlChart: (OptionContract) -> Unit,
    onContractViewed: (OptionContract) -> Unit
) {
    val feed by marketRepository.observeOptionContracts()
        .collectAsState(initial = null)

    val bestContracts = remember(feed) {
        feed?.let { CoveredCallCalculator.rankBestContracts(it.items) } ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "به‌صرفه‌ترین قراردادهای کاورد کال",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "رتبه‌بندی بر اساس بازده موثر سالانه استراتژی",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        val f = feed
        when {
            f == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            bestContracts.isEmpty() -> {
                EmptyState(
                    if (f.items.isEmpty() && f.detail.isNotBlank()) f.detail
                    else "فعلاً قرارداد کاورد کال به‌صرفه‌ای پیدا نشد. کمی بعد دوباره تلاش کن."
                )
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(bestContracts) { result ->
                        CoveredCallCard(
                            result = result,
                            onClick = {
                                onContractViewed(result.contract)
                                onOpenPnlChart(result.contract)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun CoveredCallCard(result: CoveredCallResult, onClick: () -> Unit) {
    val c = result.contract
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(c.symbol, fontWeight = FontWeight.Bold)
                Text("${c.underlyingSymbol}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("قیمت اعمال: ${formatRial(c.strikePrice)}   |   قیمت پایه: ${formatRial(c.underlyingPrice)}")
            Text("پرمیوم: ${formatRial(c.premium)}   |   سررسید: ${c.expiryDate} (${c.daysToExpiry} روز)")
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "بازده ایستا: %.1f%%".format(result.staticReturnPercent),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "بازده سالانه: %.1f%%".format(result.annualizedReturnPercent),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "پوشش ریزش قیمت: %.1f%%".format(result.downsideProtectionPercent),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun formatRial(value: Long): String {
    return "%,d ریال".format(value)
}
