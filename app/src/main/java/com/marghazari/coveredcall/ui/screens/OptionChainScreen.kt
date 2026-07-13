package com.marghazari.coveredcall.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marghazari.coveredcall.data.model.OptionContract
import com.marghazari.coveredcall.data.model.OptionType
import com.marghazari.coveredcall.data.repository.MarketRepository

private data class UnderlyingChain(
    val underlyingSymbol: String,
    val underlyingPrice: Long,
    val contracts: List<OptionContract>
)

@Composable
fun OptionChainScreen(
    marketRepository: MarketRepository,
    onOpenPnlChart: (OptionContract) -> Unit,
    onContractViewed: (OptionContract) -> Unit
) {
    val feed by marketRepository.observeOptionContracts()
        .collectAsState(initial = null)

    val chains = remember(feed) {
        (feed?.items ?: emptyList())
            .groupBy { it.underlyingSymbol.ifBlank { "نامشخص" } }
            .map { (underlying, list) ->
                UnderlyingChain(
                    underlyingSymbol = underlying,
                    underlyingPrice = list.firstOrNull()?.underlyingPrice ?: 0L,
                    contracts = list.sortedWith(
                        compareBy({ it.type != OptionType.CALL }, { it.strikePrice })
                    )
                )
            }
            .sortedBy { it.underlyingSymbol }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("زنجیره قرارداد هر سهم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "همه‌ی قراردادهای هر سهم پایه، مرتب بر اساس نوع و قیمت اعمال.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        val f = feed
        when {
            f == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            chains.isEmpty() -> {
                EmptyState(
                    if (f.detail.isNotBlank()) f.detail
                    else "فعلاً قراردادی برای نمایش وجود ندارد."
                )
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(chains) { chain ->
                        ChainCard(chain, onOpenPnlChart, onContractViewed)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainCard(
    chain: UnderlyingChain,
    onOpenPnlChart: (OptionContract) -> Unit,
    onContractViewed: (OptionContract) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(chain.underlyingSymbol, fontWeight = FontWeight.Bold)
                    Text(
                        "قیمت پایه: ${formatRial(chain.underlyingPrice)} | ${chain.contracts.size} قرارداد",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    chain.contracts.forEach { contract ->
                        ContractRow(contract) {
                            onContractViewed(contract)
                            onOpenPnlChart(contract)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContractRow(contract: OptionContract, onClick: () -> Unit) {
    val isCall = contract.type == OptionType.CALL
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(contract.symbol, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(
                "اعمال: ${formatRial(contract.strikePrice)} | ${contract.daysToExpiry} روز",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (isCall) "کال" else "پوت",
                color = if (isCall) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text("پرمیوم: ${formatRial(contract.premium)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
