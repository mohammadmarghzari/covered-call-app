package com.marghazari.coveredcall.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marghazari.coveredcall.data.model.ReceiptStatus
import com.marghazari.coveredcall.data.model.ReceiptSubmission
import com.marghazari.coveredcall.data.repository.UserRepository
import kotlinx.coroutines.launch

private const val CARD_NUMBER = "5859-4711-2302-9369"
private const val CARD_OWNER = "محمد مرغزاری - بانک خاورمیانه"
private const val PRICE_TOMAN = "100,000 تومان (یک ماه)"

@Composable
fun SubscriptionScreen(
    uid: String,
    isSubscribed: Boolean,
    subscriptionExpiryMillis: Long,
    userRepository: UserRepository
) {
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }

    val receipts by userRepository.observeReceipts(uid).collectAsState(initial = emptyList())

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            scope.launch {
                try {
                    userRepository.submitReceipt(uid, uri)
                    uploadMessage = "رسید ارسال شد. به محض بررسی، اشتراک فعال می‌شود."
                } catch (e: Exception) {
                    uploadMessage = "خطا در ارسال رسید، دوباره تلاش کن."
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("اشتراک بورس کالا", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (isSubscribed) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("اشتراک شما فعال است ✅", fontWeight = FontWeight.Bold)
                    if (subscriptionExpiryMillis > 0) {
                        val remainingDays = ((subscriptionExpiryMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24))
                        Text("روزهای باقیمانده: $remainingDays روز")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("در صورت تمایل می‌توانی زودتر تمدید کنی:")
            Spacer(modifier = Modifier.height(8.dp))
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("مبلغ اشتراک: $PRICE_TOMAN", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("مرحله ۱: واریز به کارت زیر")
                Text(CARD_NUMBER, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(CARD_OWNER, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Text("مرحله ۲: آپلود عکس رسید واریزی")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !isUploading
                ) {
                    Text(if (isUploading) "در حال ارسال..." else "انتخاب و ارسال رسید")
                }
                uploadMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "مرحله ۳: بعد از تایید رسید، اشتراک بورس کالا همان لحظه فعال می‌شود.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("تاریخچه رسیدهای ارسالی", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(receipts) { receipt ->
                ReceiptRow(receipt)
            }
        }
    }
}

@Composable
private fun ReceiptRow(receipt: ReceiptSubmission) {
    val statusText = when (receipt.status) {
        ReceiptStatus.PENDING -> "در حال بررسی"
        ReceiptStatus.APPROVED -> "تایید شد ✅"
        ReceiptStatus.REJECTED -> "رد شد ❌"
    }
    ElevatedCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("مبلغ: ${receipt.amountToman} تومان")
            Text(statusText, fontWeight = FontWeight.SemiBold)
        }
    }
}
