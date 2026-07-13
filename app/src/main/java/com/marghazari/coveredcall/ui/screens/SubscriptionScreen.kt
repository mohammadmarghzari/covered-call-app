package com.marghazari.coveredcall.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marghazari.coveredcall.data.model.AppUser
import com.marghazari.coveredcall.data.remote.ApiResult
import com.marghazari.coveredcall.data.remote.BackendClient
import kotlinx.coroutines.launch

private const val CARD_NUMBER = "5859-4711-2302-9369"
private const val CARD_OWNER = "محمد مرغزاری - بانک خاورمیانه"
private const val PRICE_TOMAN = "100,000 تومان (یک ماه)"
private const val SUPPORT_CONTACT = "@coveredcall_support" // آیدی تلگرام پشتیبانی — قابل تغییر

@Composable
fun SubscriptionScreen(
    isSubscribed: Boolean,
    subscriptionExpiryMillis: Long,
    backendClient: BackendClient,
    token: String,
    onSubscriptionUpdated: (AppUser) -> Unit
) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var isRedeeming by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("اشتراک بورس کالا", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        if (isSubscribed) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("اشتراک شما فعال است ✅", fontWeight = FontWeight.Bold)
                    if (subscriptionExpiryMillis > 0) {
                        val remainingDays =
                            ((subscriptionExpiryMillis - System.currentTimeMillis()) / (1000L * 60 * 60 * 24))
                                .coerceAtLeast(0)
                        Text("روزهای باقیمانده: $remainingDays روز")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("در صورت تمایل می‌توانی با کد جدید تمدید کنی:")
            Spacer(Modifier.height(8.dp))
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("مبلغ اشتراک: $PRICE_TOMAN", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("مرحله ۱: واریز به کارت زیر")
                Text(CARD_NUMBER, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(CARD_OWNER, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Text("مرحله ۲: رسید را برای پشتیبانی بفرست و «کد فعال‌سازی» بگیر:")
                Text(SUPPORT_CONTACT, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("مرحله ۳: کد فعال‌سازی را اینجا وارد کن:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; message = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("کد فعال‌سازی") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val c = code.trim()
                        if (c.isEmpty()) {
                            message = "کد فعال‌سازی را وارد کن."
                            return@Button
                        }
                        isRedeeming = true
                        message = null
                        scope.launch {
                            when (val r = backendClient.redeemCode(token, c)) {
                                is ApiResult.Ok -> {
                                    onSubscriptionUpdated(r.data)
                                    code = ""
                                    message = "اشتراک فعال شد ✅"
                                }
                                is ApiResult.Err -> message = r.message
                            }
                            isRedeeming = false
                        }
                    },
                    enabled = !isRedeeming
                ) {
                    Text(if (isRedeeming) "در حال بررسی..." else "فعال‌سازی اشتراک")
                }
                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
