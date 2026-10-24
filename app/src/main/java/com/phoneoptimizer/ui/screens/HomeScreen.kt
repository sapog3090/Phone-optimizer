package com.phoneoptimizer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phoneoptimizer.data.JunkScanner
import com.phoneoptimizer.data.MemoryBooster
import com.phoneoptimizer.data.SystemInfo
import com.phoneoptimizer.ui.components.LocalResumeTick
import com.phoneoptimizer.ui.components.ScoreRing
import com.phoneoptimizer.ui.components.ScreenTitle
import com.phoneoptimizer.ui.components.UsageCard
import com.phoneoptimizer.ui.components.formatBytes
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resumeTick = LocalResumeTick.current

    var refresh by remember { mutableIntStateOf(0) }
    var memory by remember { mutableStateOf(SystemInfo.memory(context)) }
    var storage by remember { mutableStateOf(SystemInfo.storage()) }
    var battery by remember { mutableStateOf(SystemInfo.battery(context)) }
    var optimizing by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resumeTick, refresh) {
        memory = SystemInfo.memory(context)
        storage = SystemInfo.storage()
        battery = SystemInfo.battery(context)
    }

    val score = SystemInfo.healthScore(memory, storage, battery)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Состояние телефона")

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ScoreRing(score)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    optimizing = true
                    report = null
                    scope.launch {
                        val boost = MemoryBooster.boost(context)
                        // Быстрая оптимизация удаляет только безопасные категории
                        val junk = JunkScanner.scan(context).filter { it.category.safeByDefault }
                        val freedFiles = JunkScanner.delete(junk)
                        report = "Освобождено ОЗУ: ${context.formatBytes(boost.freedBytes)}\n" +
                            "Удалено мусора: ${context.formatBytes(freedFiles)} (${junk.size} объектов)"
                        optimizing = false
                        refresh++
                    }
                },
                enabled = !optimizing,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (optimizing) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(12.dp))
                    Text("Оптимизация…")
                } else {
                    Icon(Icons.Filled.Bolt, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Быстрая оптимизация")
                }
            }
        }

        report?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(it, Modifier.padding(16.dp).fillMaxWidth())
            }
        }

        UsageCard(
            Icons.Filled.Memory, "Оперативная память", memory.usedFraction,
            "Занято ${context.formatBytes(memory.usedBytes)} из ${context.formatBytes(memory.totalBytes)}",
        )
        UsageCard(
            Icons.Filled.SdStorage, "Хранилище", storage.usedFraction,
            "Свободно ${context.formatBytes(storage.freeBytes)} из ${context.formatBytes(storage.totalBytes)}",
        )
        battery?.let {
            UsageCard(
                Icons.Filled.BatteryStd, "Батарея", it.levelPercent / 100f,
                "${it.status} · ${"%.1f".format(it.temperatureC)} °C · состояние: ${it.health.lowercase()}",
            )
        }
    }
}
