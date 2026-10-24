package com.phoneoptimizer.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.phoneoptimizer.data.BatteryStats
import com.phoneoptimizer.data.SystemInfo
import com.phoneoptimizer.ui.components.InfoRow
import com.phoneoptimizer.ui.components.ScreenTitle
import com.phoneoptimizer.ui.components.UsageCard
import com.phoneoptimizer.ui.components.openBatterySaverSettings
import com.phoneoptimizer.ui.components.openBatteryUsage
import com.phoneoptimizer.ui.components.openDisplaySettings
import com.phoneoptimizer.ui.theme.Bad
import com.phoneoptimizer.ui.theme.Good
import com.phoneoptimizer.ui.theme.Warn

@Composable
fun BatteryScreen() {
    val context = LocalContext.current
    var battery by remember { mutableStateOf(SystemInfo.battery(context)) }

    // Живое обновление: система присылает событие при каждом изменении батареи
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                battery = SystemInfo.parseBattery(intent)
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Батарея")

        val b = battery
        if (b == null) {
            Text("Не удалось получить данные о батарее")
            return@Column
        }

        UsageCard(
            if (b.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
            "Заряд", b.levelPercent / 100f, "${b.status} · источник: ${b.source}",
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                InfoRow(
                    "Температура", "%.1f °C".format(b.temperatureC),
                    when {
                        b.isOverheated -> Bad
                        b.temperatureC > 38f -> Warn
                        else -> Good
                    },
                )
                InfoRow("Напряжение", "%.2f В".format(b.voltageMv / 1000f))
                InfoRow("Состояние", b.health, if (b.health == "Хорошее") Good else Warn)
                InfoRow("Технология", b.technology)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Советы", style = MaterialTheme.typography.titleMedium)
                batteryTips(b).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }

        OutlinedButton({ context.openBatterySaverSettings() }, Modifier.fillMaxWidth()) {
            Text("Режим энергосбережения")
        }
        OutlinedButton({ context.openBatteryUsage() }, Modifier.fillMaxWidth()) {
            Text("Что расходует заряд")
        }
        OutlinedButton({ context.openDisplaySettings() }, Modifier.fillMaxWidth()) {
            Text("Яркость и тайм-аут экрана")
        }
    }
}

private fun batteryTips(b: BatteryStats): List<String> = buildList {
    if (b.isOverheated) add("Телефон перегрет! Закройте тяжёлые приложения и уберите его от солнца.")
    else if (b.temperatureC > 38f) add("Батарея тёплая. Не играйте и не заряжайте телефон под одеялом или на солнце.")
    if (b.levelPercent < 20 && !b.isCharging) add("Мало заряда — включите режим энергосбережения.")
    if (b.isCharging && b.levelPercent >= 80) add("Для долгой жизни батареи заряжайте её до 80–90%, а не до 100%.")
    add("Тёмная тема и пониженная яркость заметно экономят заряд на AMOLED-экранах.")
    add("Отключайте Bluetooth, GPS и NFC, когда они не нужны.")
    add("Проверьте в разделе «Приложения», какие программы давно не используются, и удалите лишнее.")
}
