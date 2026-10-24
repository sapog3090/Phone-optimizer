package com.phoneoptimizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.phoneoptimizer.ui.components.LocalResumeTick
import com.phoneoptimizer.ui.screens.AppsScreen
import com.phoneoptimizer.ui.screens.BatteryScreen
import com.phoneoptimizer.ui.screens.BoostScreen
import com.phoneoptimizer.ui.screens.CleanerScreen
import com.phoneoptimizer.ui.screens.HomeScreen
import com.phoneoptimizer.ui.theme.PhoneOptimizerTheme

private enum class Tab(val title: String, val icon: ImageVector) {
    HOME("Главная", Icons.Filled.Home),
    CLEANER("Очистка", Icons.Filled.CleaningServices),
    BOOST("Ускорение", Icons.Filled.RocketLaunch),
    BATTERY("Батарея", Icons.Filled.BatteryChargingFull),
    APPS("Приложения", Icons.Filled.Apps),
}

class MainActivity : ComponentActivity() {

    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhoneOptimizerTheme {
                CompositionLocalProvider(LocalResumeTick provides resumeTick) {
                    App()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Пользователь мог выдать разрешение в настройках — сообщаем экранам
        resumeTick++
    }
}

@Composable
private fun App() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title, maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (Tab.entries[tab]) {
                Tab.HOME -> HomeScreen()
                Tab.CLEANER -> CleanerScreen()
                Tab.BOOST -> BoostScreen()
                Tab.BATTERY -> BatteryScreen()
                Tab.APPS -> AppsScreen()
            }
        }
    }
}
