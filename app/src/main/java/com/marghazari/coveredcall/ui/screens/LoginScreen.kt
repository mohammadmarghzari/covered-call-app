package com.marghazari.coveredcall.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.marghazari.coveredcall.auth.GoogleAuthClient
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authClient: GoogleAuthClient,
    onSignedIn: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                isLoading = true
                scope.launch {
                    try {
                        authClient.signInWithIdToken(idToken)
                        onSignedIn()
                    } catch (e: Exception) {
                        errorMessage = "ورود ناموفق بود، دوباره تلاش کنید."
                    } finally {
                        isLoading = false
                    }
                }
            }
        } catch (e: ApiException) {
            errorMessage = "ورود با جیمیل لغو شد یا خطا داشت."
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "آپشن یار",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "بازار آپشن بورس و بورس کالای ایران، زنده و آنالیز شده",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(onClick = { launcher.launch(authClient.getSignInIntent()) }) {
                    Text("ورود با جیمیل")
                }
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
