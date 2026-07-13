package com.marghazari.coveredcall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marghazari.coveredcall.auth.SessionManager
import com.marghazari.coveredcall.data.local.AppDatabase
import com.marghazari.coveredcall.data.model.AppUser
import com.marghazari.coveredcall.data.model.CommodityContract
import com.marghazari.coveredcall.data.model.OptionContract
import com.marghazari.coveredcall.data.remote.ApiResult
import com.marghazari.coveredcall.data.remote.BackendClient
import com.marghazari.coveredcall.data.repository.BrsApiMarketRepository
import com.marghazari.coveredcall.data.repository.MarketRepository
import com.marghazari.coveredcall.ui.screens.*
import com.marghazari.coveredcall.ui.theme.CoveredCallTheme
import kotlinx.coroutines.launch

private object Routes {
    const val OPTIONS = "options"
    const val FORMULA = "formula"
    const val CHAIN = "chain"
    const val COMMODITY = "commodity"
    const val PROFILE = "profile"
    const val SUBSCRIPTION = "subscription"
    const val HISTORY = "history"
    const val FEEDBACK = "feedback"
    const val PNL_CHART = "pnl_chart"
}

class MainActivity : ComponentActivity() {

    private val backendClient by lazy { BackendClient() }
    private val marketRepository: MarketRepository by lazy { BrsApiMarketRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoveredCallTheme {
                Surface {
                    AppRoot(backendClient, marketRepository)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(
    backendClient: BackendClient,
    marketRepository: MarketRepository
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var token by remember { mutableStateOf(sessionManager.getToken()) }
    var appUser by remember { mutableStateOf<AppUser?>(null) }
    var selectedOption by remember { mutableStateOf<OptionContract?>(null) }

    // با تغییر توکن، پروفایل کاربر را از سرور می‌گیریم. فقط اگر توکن واقعاً باطل شده باشد، خارج می‌شویم.
    LaunchedEffect(token) {
        val t = token
        if (t == null) {
            appUser = null
            return@LaunchedEffect
        }
        when (val r = backendClient.getMe(t)) {
            is ApiResult.Ok -> appUser = r.data
            is ApiResult.Err -> if (r.authExpired) {
                sessionManager.clear()
                token = null
                appUser = null
            }
        }
    }

    if (token == null) {
        LoginScreen(backendClient = backendClient) { newToken, user ->
            sessionManager.saveToken(newToken)
            appUser = user
            token = newToken
        }
        return
    }

    val activeToken = token!!
    val isSubscribed = appUser?.isSubscribed == true &&
        (appUser?.subscriptionExpiryMillis ?: 0L) > System.currentTimeMillis()
    val displayName = appUser?.displayName ?: ""
    val phone = appUser?.phone ?: ""
    val ownerId = appUser?.id?.takeIf { it.isNotBlank() } ?: "me"

    val signOut = {
        sessionManager.clear()
        token = null
        appUser = null
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("آپشن یار", style = MaterialTheme.typography.titleMedium) })
        },
        bottomBar = { BottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.OPTIONS,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.OPTIONS) {
                OptionsMarketScreen(
                    marketRepository = marketRepository,
                    onOpenPnlChart = { contract ->
                        selectedOption = contract
                        navController.navigate(Routes.PNL_CHART)
                    },
                    onContractViewed = { contract ->
                        scope.launch { logActivity(db, ownerId, "VIEW_OPTION", contract.symbol) }
                    }
                )
            }
            composable(Routes.FORMULA) {
                FormulaScreen(
                    marketRepository = marketRepository,
                    onOpenPnlChart = { contract ->
                        selectedOption = contract
                        navController.navigate(Routes.PNL_CHART)
                    },
                    onContractViewed = { contract ->
                        scope.launch { logActivity(db, ownerId, "VIEW_OPTION", contract.symbol) }
                    }
                )
            }
            composable(Routes.CHAIN) {
                OptionChainScreen(
                    marketRepository = marketRepository,
                    onOpenPnlChart = { contract ->
                        selectedOption = contract
                        navController.navigate(Routes.PNL_CHART)
                    },
                    onContractViewed = { contract ->
                        scope.launch { logActivity(db, ownerId, "VIEW_OPTION", contract.symbol) }
                    }
                )
            }
            composable(Routes.COMMODITY) {
                CommodityMarketScreen(
                    isSubscribed = isSubscribed,
                    marketRepository = marketRepository,
                    onGoToSubscription = { navController.navigate(Routes.SUBSCRIPTION) },
                    onContractViewed = { contract: CommodityContract ->
                        scope.launch { logActivity(db, ownerId, "VIEW_COMMODITY", contract.symbol) }
                    }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    displayName = displayName,
                    phone = phone,
                    isSubscribed = isSubscribed,
                    subscriptionExpiryMillis = appUser?.subscriptionExpiryMillis ?: 0L,
                    onOpenSubscription = { navController.navigate(Routes.SUBSCRIPTION) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) },
                    onOpenFeedback = { navController.navigate(Routes.FEEDBACK) },
                    onSignOut = signOut
                )
            }
            composable(Routes.SUBSCRIPTION) {
                SubscriptionScreen(
                    isSubscribed = isSubscribed,
                    subscriptionExpiryMillis = appUser?.subscriptionExpiryMillis ?: 0L,
                    backendClient = backendClient,
                    token = activeToken,
                    onSubscriptionUpdated = { appUser = it }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(ownerUid = ownerId)
            }
            composable(Routes.FEEDBACK) {
                FeedbackScreen(backendClient = backendClient, token = activeToken)
            }
            composable(Routes.PNL_CHART) {
                val contract = selectedOption
                if (contract != null) {
                    PnlChartScreen(contract = contract)
                } else {
                    Text("ابتدا یک قرارداد را از بازار آپشن انتخاب کنید.")
                }
            }
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun BottomBar(navController: NavHostController) {
    val items = listOf(
        NavItem(Routes.OPTIONS, "آپشن", Icons.Filled.ShowChart),
        NavItem(Routes.FORMULA, "فرمول‌یاب", Icons.Filled.Calculate),
        NavItem(Routes.CHAIN, "زنجیره", Icons.Filled.AccountTree),
        NavItem(Routes.COMMODITY, "کالا", Icons.Filled.Storefront),
        NavItem(Routes.PROFILE, "پروفایل", Icons.Filled.Person)
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
