package com.marghazari.coveredcall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marghazari.coveredcall.auth.GoogleAuthClient
import com.marghazari.coveredcall.data.local.AppDatabase
import com.marghazari.coveredcall.data.model.AppUser
import com.marghazari.coveredcall.data.model.CommodityContract
import com.marghazari.coveredcall.data.model.OptionContract
import com.marghazari.coveredcall.data.repository.MarketRepository
import com.marghazari.coveredcall.data.repository.BrsApiOptionRepository
import com.marghazari.coveredcall.data.repository.UserRepository
import com.marghazari.coveredcall.ui.screens.*
import com.marghazari.coveredcall.ui.theme.CoveredCallTheme
import kotlinx.coroutines.launch

private object Routes {
    const val LOGIN = "login"
    const val OPTIONS = "options"
    const val COMMODITY = "commodity"
    const val SUBSCRIPTION = "subscription"
    const val HISTORY = "history"
    const val PNL_CHART = "pnl_chart"
}

class MainActivity : ComponentActivity() {

    private val authClient by lazy { GoogleAuthClient(this) }
    private val userRepository by lazy { UserRepository() }
    private val marketRepository: MarketRepository by lazy { BrsApiOptionRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoveredCallTheme {
                Surface {
                    AppRoot(authClient, userRepository, marketRepository)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    authClient: GoogleAuthClient,
    userRepository: UserRepository,
    marketRepository: MarketRepository
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var currentUid by remember { mutableStateOf(authClient.currentUser()?.uid) }
    val appUser by produceState<AppUser?>(initialValue = null, currentUid) {
        val uid = currentUid
        if (uid != null) {
            userRepository.observeUser(uid).collect { value = it }
        } else {
            value = null
        }
    }

    var selectedOption by remember { mutableStateOf<OptionContract?>(null) }

    val isSignedIn = currentUid != null

    if (!isSignedIn) {
        LoginScreen(authClient = authClient, onSignedIn = {
            val user = authClient.currentUser()
            if (user != null) {
                scope.launch {
                    userRepository.ensureUserAccountExists(
                        uid = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: ""
                    )
                    currentUid = user.uid
                }
            }
        })
        return
    }

    val uid = currentUid!!
    val isSubscribed = appUser?.isSubscribed == true &&
        (appUser?.subscriptionExpiryMillis ?: 0L) > System.currentTimeMillis()

    Scaffold(
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
                        scope.launch {
                            logActivity(db, uid, "VIEW_OPTION", contract.symbol)
                        }
                    }
                )
            }
            composable(Routes.COMMODITY) {
                CommodityMarketScreen(
                    isSubscribed = isSubscribed,
                    marketRepository = marketRepository,
                    onGoToSubscription = { navController.navigate(Routes.SUBSCRIPTION) },
                    onContractViewed = { contract: CommodityContract ->
                        scope.launch {
                            logActivity(db, uid, "VIEW_COMMODITY", contract.symbol)
                        }
                    }
                )
            }
            composable(Routes.SUBSCRIPTION) {
                SubscriptionScreen(
                    uid = uid,
                    isSubscribed = isSubscribed,
                    subscriptionExpiryMillis = appUser?.subscriptionExpiryMillis ?: 0L,
                    userRepository = userRepository
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(ownerUid = uid)
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

@Composable
private fun BottomBar(navController: NavHostController) {
    val items = listOf(
        Triple(Routes.OPTIONS, "آپشن", Icons.Filled.ShowChart),
        Triple(Routes.COMMODITY, "بورس کالا", Icons.Filled.Storefront),
        Triple(Routes.SUBSCRIPTION, "اشتراک", Icons.Filled.Wallet),
        Triple(Routes.HISTORY, "تاریخچه", Icons.Filled.History)
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
