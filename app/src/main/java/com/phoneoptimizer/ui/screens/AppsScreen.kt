package com.phoneoptimizer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phoneoptimizer.data.AppEntry
import com.phoneoptimizer.data.AppsRepository
import com.phoneoptimizer.ui.components.LocalResumeTick
import com.phoneoptimizer.ui.components.PermissionCard
import com.phoneoptimizer.ui.components.ScreenTitle
import com.phoneoptimizer.ui.components.formatBytes
import com.phoneoptimizer.ui.components.openAppDetails
import com.phoneoptimizer.ui.components.openUsageAccessSettings
import com.phoneoptimizer.ui.components.requestUninstall
import com.phoneoptimizer.ui.theme.Warn

private enum class AppFilter(val title: String) {
    ALL("Все"), UNUSED("Не используются"), BIG_CACHE("Большой кэш"),
}

private const val UNUSED_DAYS = 30
private const val BIG_CACHE_BYTES = 100L * 1024 * 1024

@Composable
fun AppsScreen() {
    val context = LocalContext.current
    val resumeTick = LocalResumeTick.current

    var hasUsageAccess by remember { mutableStateOf(AppsRepository.hasUsageAccess(context)) }
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var filter by remember { mutableStateOf(AppFilter.ALL) }

    // Перезагружаем список при возврате — например, после удаления приложения
    LaunchedEffect(resumeTick) {
        hasUsageAccess = AppsRepository.hasUsageAccess(context)
        apps = AppsRepository.loadUserApps(context)
    }

    val shown = apps.orEmpty().filter {
        when (filter) {
            AppFilter.ALL -> true
            AppFilter.UNUSED -> (it.daysSinceUse() ?: 0) >= UNUSED_DAYS
            AppFilter.BIG_CACHE -> (it.cacheBytes ?: 0) >= BIG_CACHE_BYTES
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenTitle("Приложения")

        if (!hasUsageAccess) {
            PermissionCard(
                "Дайте доступ к статистике использования, чтобы видеть размер приложений " +
                    "и находить те, которыми вы давно не пользуетесь.",
                "Открыть настройки",
            ) { context.openUsageAccessSettings() }
        }

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFilter.entries.forEach {
                FilterChip(selected = filter == it, onClick = { filter = it }, label = { Text(it.title) })
            }
        }

        if (apps == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        } else {
            Text(
                "Найдено: ${shown.size}" +
                    shown.mapNotNull { it.sizeBytes }.sum().takeIf { it > 0 }
                        ?.let { " · ${context.formatBytes(it)}" }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.packageName }) { AppRow(it) }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppEntry) {
    val context = LocalContext.current
    val days = app.daysSinceUse()
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(app.icon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val details = listOfNotNull(
                    app.sizeBytes?.let { context.formatBytes(it) },
                    app.cacheBytes?.takeIf { it > 0 }?.let { "кэш ${context.formatBytes(it)}" },
                    when {
                        days == null -> null
                        days == 0L -> "сегодня"
                        else -> "не открывалось $days дн."
                    },
                ).joinToString(" · ")
                Text(
                    details,
                    style = MaterialTheme.typography.bodySmall,
                    color = if ((days ?: 0) >= UNUSED_DAYS) Warn else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { context.openAppDetails(app.packageName) }) {
                Icon(Icons.Filled.Info, contentDescription = "Сведения и очистка кэша")
            }
            IconButton(onClick = { context.requestUninstall(app.packageName) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
            }
        }
    }
}
