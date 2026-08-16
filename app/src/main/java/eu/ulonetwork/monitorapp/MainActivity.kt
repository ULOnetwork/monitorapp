package eu.ulonetwork.monitorapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import eu.ulonetwork.monitorapp.data.PreferencesManager
import eu.ulonetwork.monitorapp.ui.screens.ConsentScreen
import eu.ulonetwork.monitorapp.ui.screens.LogScreen
import eu.ulonetwork.monitorapp.ui.screens.RuleEditScreen
import eu.ulonetwork.monitorapp.ui.screens.RulesScreen
import eu.ulonetwork.monitorapp.ui.screens.SettingsScreen
import eu.ulonetwork.monitorapp.ui.screens.StatusScreen
import eu.ulonetwork.monitorapp.ui.theme.UnetworkMonitorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferencesManager = PreferencesManager(this)
        val openLog = intent?.getBooleanExtra(EXTRA_OPEN_LOG, false) ?: false

        setContent {
            UnetworkMonitorTheme {
                Surface {
                    UnetworkMonitorRoot(
                        hasAcceptedConsentInitially = preferencesManager.hasAcceptedConsent(),
                        openLogInitially = openLog
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_LOG = "open_log"
    }
}

private data class BottomTab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.STATUS, R.string.nav_status, Icons.Filled.Notifications),
    BottomTab(Routes.RULES, R.string.nav_rules, Icons.Filled.List),
    BottomTab(Routes.LOG, R.string.nav_log, Icons.Filled.History),
    BottomTab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings)
)

private object Routes {
    const val CONSENT = "consent"
    const val STATUS = "status"
    const val RULES = "rules"
    const val RULE_EDIT = "rule_edit"
    const val LOG = "log"
    const val SETTINGS = "settings"
}

@Composable
fun UnetworkMonitorRoot(hasAcceptedConsentInitially: Boolean, openLogInitially: Boolean) {
    var hasAcceptedConsent by remember { mutableStateOf(hasAcceptedConsentInitially) }
    val navController = rememberNavController()

    if (!hasAcceptedConsent) {
        ConsentScreen(onAccept = { hasAcceptedConsent = true })
        return
    }

    val startDestination = if (openLogInitially) Routes.LOG else Routes.STATUS

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination
            NavigationBar {
                bottomTabs.forEach { tab ->
                    val selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResourceCompat(tab.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.STATUS) { StatusScreen() }
            composable(Routes.RULES) {
                RulesScreen(onAddRule = { navController.navigate("${Routes.RULE_EDIT}/-1") },
                    onEditRule = { id -> navController.navigate("${Routes.RULE_EDIT}/$id") })
            }
            composable("${Routes.RULE_EDIT}/{ruleId}") { backStackEntry ->
                val ruleId = backStackEntry.arguments?.getString("ruleId")?.toLongOrNull() ?: -1L
                RuleEditScreen(ruleId = ruleId, onDone = { navController.popBackStack() })
            }
            composable(Routes.LOG) { LogScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}

@Composable
private fun stringResourceCompat(resId: Int): String = androidx.compose.ui.res.stringResource(id = resId)
