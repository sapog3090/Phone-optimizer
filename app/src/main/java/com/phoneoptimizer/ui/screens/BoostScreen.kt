package com.phoneoptimizer.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phoneoptimizer.data.BoostResult
import com.phoneoptimizer.data.MemoryBooster
import com.phoneoptimizer.data.SystemInfo
import com.phoneoptimizer.ui.components.LocalResumeTick
import com.phoneoptimizer.ui.components.ScreenTitle
import com.phoneoptimizer.ui.components.UsageCard
import com.phoneoptimizer.ui.components.formatBytes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BoostScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resumeTick = LocalResumeTick.current

    var memory by remember { mutableStateOf(SystemInfo.memory(context)) }
    var boosting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<BoostResult?>(null) }

    // Обновляем показания памяти раз в 2 секунды, пока экран открыт
    LaunchedEffect(resumeTick) {
        while (true) {
            memory = SystemInfo.memory(context)
            delay(2000)
        }
    }

    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        0f, 360f, infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "angle",
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Ускорение")

        UsageCard(
            Icons.Filled.Memory, "Оперативная память", memory.usedFraction,
            "Свободно ${context.formatBytes(memory.availBytes)} из ${context.formatBytes(memory.totalBytes)}" +
                if (memory.isLow) " · мало памяти!" else "",
        )

        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.RocketLaunch,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(120.dp).rotate(if (boosting) angle else 0f),
            )
        }

        Button(
            onClick = {
                boosting = true
                scope.launch {
                    result = MemoryBooster.boost(context)
                    memory = SystemInfo.memory(context)
                    boosting = false
                }
            },
            enabled = !boosting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(if (boosting) "Освобождаем память…" else "Ускорить телефон") }

        result?.let {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Готово!", style = MaterialTheme.typography.titleMedium)
                    Text("Проверено приложений: ${it.stoppedApps}")
                    Text("Освобождено памяти: ${context.formatBytes(it.freedBytes)}")
                }
            }
        }

        Text(
            "Android сам управляет памятью, поэтому эффект зависит от телефона. " +
                "Ускорение закрывает только фоновые процессы — открытые приложения и данные не пострадают.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
