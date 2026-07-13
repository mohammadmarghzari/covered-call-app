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
import com.marghazari.coveredcall.data.model.OptionContract
import com.marghazari.coveredcall.data.model.TargetMatch
import com.marghazari.coveredcall.data.repository.MarketRepository
import com.marghazari.coveredcall.domain.CoveredCallCalculator
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormulaScreen(
    marketRepository: MarketRepository,
    onOpenPnlChart: (OptionContract) -> Unit,
    onContractViewed: (OptionContract) -> Unit
) {
    val contracts by marketRepository.observeOptionContracts()
        .collectAsState(initial = null)

    var targetPercent by remember { mutableStateOf(20f) }
    var horizonMonths by remember { mutableStateOf(1) }

    val matches = remember(contracts, targetPercent, horizonMonths) {
        contracts?.let {
            CoveredCallCalculator.findForTarget(it, targetPercent.toDouble(), horizonMonths)
        } ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("فرمول‌یاب سود هدف", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "سود موردنظرت و بازه‌ی زمانی را مشخص کن تا قراردادهای کاورد کال مناسب را با استدلال معرفی کنم.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("سود هدف: %${targetPercent.roundToInt()}", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = targetPercent,
                    onValueChange = { targetPercent = it },
                    valueRange = 5f..60f,
                    steps = 10
                )
                Spacer(Modifier.height(8.dp))
                Text("بازه‌ی زمانی:", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { m ->
                        FilterChip(
                            selected = horizonMonths == m,
                            onClick = { horizonMonths = m },
                            label = { Text("$m ماه") }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            contracts == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            matches.isEmpty() -> {
                EmptyState(
                    "با سود هدف %${targetPercent.roundToInt()} در $horizonMonths ماه، فعلاً قرارداد مناسبی پیدا نشد.\n" +
                        "سود هدف را کمی کمتر کن یا بازه را بلندتر بگیر."
                )
            }
            else -> {
                Text(
                    "${matches.size} قرارداد به سود هدف تو می‌رسد:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(matches) { match ->
                        TargetMatchCard(
                            match = match,
                            onClick = {
                                onContractViewed(match.result.contract)
                                onOpenPnlChart(match.result.contract)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetMatchCard(match: TargetMatch, onClick: () -> Unit) {
    val c = match.result.contract
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(c.symbol, fontWeight = FontWeight.Bold)
                if (match.fitsHorizon) {
                    AssistChip(
                        onClick = onClick,
                        label = { Text("منطبق با بازه") }
                    )
                }
            }
            Text("سهم پایه: ${c.underlyingSymbol}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "بازده تخمینی در بازه: %.1f%%".format(match.projectedReturnPercent),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text("معادل ماهانه: %.1f%%".format(match.monthlyReturnPercent), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text(match.reasoning, style = MaterialTheme.typography.bodySmall)
        }
    }
}
