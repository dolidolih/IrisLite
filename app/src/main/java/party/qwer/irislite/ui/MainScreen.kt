package party.qwer.irislite.ui

import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import party.qwer.irislite.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var hasRequiredPermissions by remember { mutableStateOf(true) }

    val tabs = listOf("설정&테스트", "히스토리", "권한")
    val icons = listOf(Icons.Default.Settings, Icons.Default.History, Icons.Default.Security)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
                val hasNotif = enabledPackages.contains(context.packageName)
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val hasBatt = pm.isIgnoringBatteryOptimizations(context.packageName)

                hasRequiredPermissions = hasNotif && hasBatt
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        topBar = {
            TopAppBar(
                title = { Text("IrisLite", fontWeight = FontWeight.ExtraBold, color = AppColors.TextMain) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.DarkBg)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = AppColors.BottomNavBg,
                contentColor = AppColors.TextSub,
                tonalElevation = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (index == 2 && !hasRequiredPermissions) {
                                        Badge(
                                            containerColor = AppColors.ErrorVivid,
                                            modifier = Modifier.offset(x = 6.dp, y = (-2).dp).size(10.dp)
                                        )
                                    }
                                }
                            ) {
                                Icon(icons[index], contentDescription = title)
                            }
                        },
                        label = { Text(title, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.TextMain,
                            selectedTextColor = AppColors.TextMain,
                            indicatorColor = AppColors.PrimaryAccent,
                            unselectedIconColor = AppColors.TextSub,
                            unselectedTextColor = AppColors.TextSub
                        )
                    )
                }
            }
        },
        containerColor = AppColors.DarkBg
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> ConfigScreen()
                1 -> HistoryScreen()
                2 -> PermissionScreen()
            }
        }
    }
}