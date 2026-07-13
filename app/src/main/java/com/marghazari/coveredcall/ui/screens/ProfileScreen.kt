package com.marghazari.coveredcall.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    displayName: String,
    email: String,
    photoUrl: String?,
    isSubscribed: Boolean,
    subscriptionExpiryMillis: Long,
    onOpenSubscription: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFeedback: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("پروفایل", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        displayName.ifBlank { "کاربر" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (email.isNotBlank()) {
                        Text(
                            email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                if (isSubscribed) {
                    Text("اشتراک بورس کالا: فعال ✅", fontWeight = FontWeight.SemiBold)
                    if (subscriptionExpiryMillis > 0) {
                        val remainingDays =
                            ((subscriptionExpiryMillis - System.currentTimeMillis()) / (1000L * 60 * 60 * 24))
                                .coerceAtLeast(0)
                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("fa"))
                        Text(
                            "تا $remainingDays روز دیگر (${sdf.format(Date(subscriptionExpiryMillis))})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text("اشتراک بورس کالا: غیرفعال", fontWeight = FontWeight.SemiBold)
                    Text(
                        "برای دسترسی به بازار کالا اشتراک تهیه کن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        ProfileMenuItem(Icons.Filled.Wallet, "اشتراک بورس کالا", onOpenSubscription)
        ProfileMenuItem(Icons.Filled.History, "تاریخچه فعالیت", onOpenHistory)
        ProfileMenuItem(Icons.Filled.Feedback, "سوالات و انتقادات", onOpenFeedback)

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("خروج از حساب")
        }
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(14.dp))
            Text(label, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronLeft, contentDescription = null)
        }
    }
}
