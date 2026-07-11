package com.marghazari.coveredcall.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marghazari.coveredcall.data.local.ActivityRecordEntity
import com.marghazari.coveredcall.data.local.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(ownerUid: String) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<ActivityRecordEntity>>(emptyList()) }

    LaunchedEffect(ownerUid) {
        records = db.activityDao().getRecentForUser(ownerUid)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("فعالیت‌های قبلی شما", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "این لیست همیشه روی همین گوشی ذخیره می‌ماند، حتی بدون اینترنت.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز فعالیتی ثبت نشده — قراردادها را در بازار آپشن یا کالا مشاهده کنید.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records) { record ->
                    ActivityRow(record)
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        scope.launch {
                            db.activityDao().clearForUser(ownerUid)
                            records = emptyList()
                        }
                    }) {
                        Text("پاک کردن تاریخچه")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(record: ActivityRecordEntity) {
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa")) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.symbol, fontWeight = FontWeight.Bold)
                Text(typeLabel(record.type), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                sdf.format(Date(record.timestampMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun typeLabel(type: String): String = when (type) {
    "VIEW_OPTION" -> "مشاهده آپشن"
    "VIEW_COMMODITY" -> "مشاهده بورس کالا"
    "CALCULATION" -> "محاسبه کاورد کال"
    else -> type
}

suspend fun logActivity(
    db: AppDatabase,
    ownerUid: String,
    type: String,
    symbol: String,
    detailsJson: String = "{}"
) {
    db.activityDao().insert(
        ActivityRecordEntity(
            ownerUid = ownerUid,
            type = type,
            symbol = symbol,
            detailsJson = detailsJson,
            timestampMillis = System.currentTimeMillis()
        )
    )
}
