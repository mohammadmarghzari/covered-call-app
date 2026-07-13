package com.marghazari.coveredcall.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marghazari.coveredcall.data.model.AppUser
import com.marghazari.coveredcall.data.remote.ApiResult
import com.marghazari.coveredcall.data.remote.BackendClient
import kotlinx.coroutines.launch

private enum class Step { PHONE, CODE }

@Composable
fun LoginScreen(
    backendClient: BackendClient,
    onLoggedIn: (token: String, user: AppUser) -> Unit
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(Step.PHONE) }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Text("آپشن یار", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "بازار آپشن بورس و بورس کالای ایران، زنده و آنالیز شده",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            when (step) {
                Step.PHONE -> {
                    Text("شماره موبایلت را وارد کن", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("مثال: 09121234567") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                when (val r = backendClient.requestOtp(phone.trim())) {
                                    is ApiResult.Ok -> {
                                        step = Step.CODE
                                        code = ""
                                    }
                                    is ApiResult.Err -> errorMessage = r.message
                                }
                                isLoading = false
                            }
                        },
                        enabled = !isLoading && phone.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLoading) "در حال ارسال..." else "ارسال کد تأیید")
                    }
                }

                Step.CODE -> {
                    Text("کد پیامک‌شده به $phone را وارد کن", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it; errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("کد تأیید") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                when (val r = backendClient.verifyOtp(phone.trim(), code.trim())) {
                                    is ApiResult.Ok -> onLoggedIn(r.data.token, r.data.user)
                                    is ApiResult.Err -> errorMessage = r.message
                                }
                                isLoading = false
                            }
                        },
                        enabled = !isLoading && code.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLoading) "در حال بررسی..." else "ورود")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            step = Step.PHONE
                            code = ""
                            errorMessage = null
                        },
                        enabled = !isLoading
                    ) {
                        Text("تغییر شماره / ارسال دوباره")
                    }
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}
