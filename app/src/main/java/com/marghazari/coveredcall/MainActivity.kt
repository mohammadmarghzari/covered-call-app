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
import com.marghazari.coveredcall.auth.GoogleAuthClient
import com.marghazari.coveredcall.data.local.AppDatabase
import com.marghazari.coveredcall.data.model.AppUser
import com.marghazari.coveredcall.data.model.CommodityContract
import com.marghazari.coveredcall.data.model.OptionContract
import com.marghazari.coveredcall.data.repository.MarketRepository
import com.marghazari.coveredcall.data.repository.BrsApiMarketRepository
import com.marghazari.coveredcall.data.repository.UserRepository
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

    private val authClient by lazy { GoogleAuthClient(this) }
    private val userRepository by lazy { UserRepository() }
    private val marketRepository: MarketRepository by lazy { BrsApiMarketRepository() }

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

@OptIn(ExperimentalMaterial3Api::class)
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

    val firebaseUser = authClient.currentUser()
    val displayName = appUser?.displayName?.takeIf { it.isNotBlank() }
        ?: firebaseUser?.displayName ?: ""
    val email = appUser?.email?.takeIf { it.isNotBlank() }
        ?: firebaseUser?.email ?: ""
    val photoUrl = firebaseUser?.photoUrl?.toString()

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
                        scope.launch {
                            logActivity(db, uid, "VIEW_OPTION", contract.symbol)
                        }
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
                        scope.launch {
                            logActivity(db, uid, "VIEW_OPTION", contract.symbol)
                        }
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
            composable(Routes.PROFILE) {
                ProfileScreen(
                    displayName = displayName,
                    email = email,
                    photoUrl = photoUrl,
                    isSubscribed = isSubscribed,
                    subscriptionExpiryMillis = appUser?.subscriptionExpiryMillis ?: 0L,
                    onOpenSubscription = { navController.navigate(Routes.SUBSCRIPTION) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) },
                    onOpenFeedback = { navController.navigate(Routes.FEEDBACK) },
                    onSignOut = {
                        authClient.signOut()
                        currentUid = null
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
            composable(Routes.FEEDBACK) {
                FeedbackScreen(
                    uid = uid,
                    email = email,
                    userRepository = userRepository
                )
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
