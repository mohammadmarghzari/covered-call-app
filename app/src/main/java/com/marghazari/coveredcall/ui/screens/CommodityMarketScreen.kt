package com.marghazari.coveredcall.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marghazari.coveredcall.data.model.CommodityContract
import com.marghazari.coveredcall.data.repository.MarketRepository

@Composable
fun CommodityMarketScreen(
    isSubscribed: Boolean,
    marketRepository: MarketRepository,
    onGoToSubscription: () -> Unit,
    onContractViewed: (CommodityContract) -> Unit
) {
    if (!isSubscribed) {
        LockedCommodityView(onGoToSubscription)
        return
    }

    val contracts by marketRepository.observeCommodityContracts()
        .collectAsState(initial = null)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "بازار بورس کالا",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "قیمت لحظه‌ای طلا و سکه",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        val list = contracts
        when {
            list == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            list.isEmpty() -> {
                EmptyState(
                    "فعلاً داده‌ای برای بازار کالا در دسترس نیست.\n" +
                        "ممکن است سرویس موقتاً پاسخ ندهد یا کلید API تنظیم نشده باشد."
                )
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list) { contract ->
                        ElevatedCard(
                            onClick = { onContractViewed(contract) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(contract.commodityName, fontWeight = FontWeight.Bold)
                                    Text(contract.contractType, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("نماد: ${contract.symbol}")
                                Text("قیمت: ${formatRial(contract.price)}")
                                Text("سررسید: ${contract.settlementDate}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedCommodityView(onGoToSubscription: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "بازار بورس کالا فقط برای مشترکین است",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "با ۱۰۰ هزار تومان در ماه، به قیمت زنده قراردادها و گواهی‌های بورس کالا دسترسی داشته باش.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onGoToSubscription) {
                Text("خرید اشتراک")
            }
        }
    }
}
