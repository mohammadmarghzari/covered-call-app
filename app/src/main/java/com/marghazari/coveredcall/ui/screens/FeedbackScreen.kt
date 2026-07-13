package com.marghazari.coveredcall.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marghazari.coveredcall.data.model.FeedbackMessage
import com.marghazari.coveredcall.data.remote.ApiResult
import com.marghazari.coveredcall.data.remote.BackendClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeedbackScreen(
    backendClient: BackendClient,
    token: String
) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<FeedbackMessage>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        when (val r = backendClient.getFeedback(token)) {
            is ApiResult.Ok -> messages = r.data
            is ApiResult.Err -> statusMessage = r.message
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("سوالات و انتقادات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "هر سوال، پیشنهاد یا انتقادی داری اینجا بنویس. پاسخ در همین صفحه نمایش داده می‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("متن پیام") },
            minLines = 3
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val body = text.trim()
                if (body.isEmpty()) {
                    statusMessage = "لطفاً متن پیام را وارد کن."
                    return@Button
                }
                isSending = true
                statusMessage = null
                scope.launch {
                    when (val r = backendClient.submitFeedback(token, body)) {
                        is ApiResult.Ok -> {
                            text = ""
                            statusMessage = "پیام ارسال شد. ممنون از بازخوردت!"
                            refreshKey++
                        }
                        is ApiResult.Err -> statusMessage = r.message
                    }
                    isSending = false
                }
            },
            enabled = !isSending
        ) {
            Text(if (isSending) "در حال ارسال..." else "ارسال پیام")
        }
        statusMessage?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        Text("پیام‌های تو", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (messages.isEmpty()) {
            Text(
                "هنوز پیامی ارسال نکرده‌ای.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages) { message ->
                    FeedbackCard(message)
                }
            }
        }
    }
}

@Composable
private fun FeedbackCard(message: FeedbackMessage) {
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa")) }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(message.message)
            Spacer(Modifier.height(4.dp))
            if (message.submittedAtMillis > 0) {
                Text(
                    sdf.format(Date(message.submittedAtMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (message.reply.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            "پاسخ پشتیبانی:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(message.reply, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    "در انتظار پاسخ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
