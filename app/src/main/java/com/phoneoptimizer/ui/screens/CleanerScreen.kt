package com.phoneoptimizer.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phoneoptimizer.data.JunkCategory
import com.phoneoptimizer.data.JunkItem
import com.phoneoptimizer.data.JunkScanner
import com.phoneoptimizer.ui.components.LocalResumeTick
import com.phoneoptimizer.ui.components.PermissionCard
import com.phoneoptimizer.ui.components.ScreenTitle
import com.phoneoptimizer.ui.components.formatBytes
import com.phoneoptimizer.ui.components.openAllFilesAccessSettings
import com.phoneoptimizer.ui.components.openStorageSettings
import kotlinx.coroutines.launch

@Composable
fun CleanerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resumeTick = LocalResumeTick.current

    var hasAccess by remember { mutableStateOf(JunkScanner.hasStorageAccess(context)) }
    var scanning by remember { mutableStateOf(false) }
    var junk by remember { mutableStateOf<List<JunkItem>?>(null) }
    var selected by remember { mutableStateOf(emptySet<JunkItem>()) }
    val expanded = remember { mutableStateListOf<JunkCategory>() }
    var confirmDelete by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasAccess = JunkScanner.hasStorageAccess(context) }

    fun startScan() {
        scanning = true
        message = null
        scope.launch {
            val found = JunkScanner.scan(context)
            junk = found
            selected = found.filter { it.category.safeByDefault }.toSet()
            scanning = false
        }
    }

    LaunchedEffect(resumeTick) {
        hasAccess = JunkScanner.hasStorageAccess(context)
        if (junk == null && !scanning) startScan()
    }

    val grouped = junk.orEmpty().groupBy { it.category }
    val selectedSize = selected.sumOf { it.sizeBytes }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenTitle("Очистка")

        if (!hasAccess) {
            PermissionCard(
                "Чтобы найти мусор во всей памяти телефона (APK, временные файлы, миниатюры), " +
                    "разрешите доступ к файлам. Без него очищается только кэш оптимизатора.",
                "Разрешить доступ",
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.openAllFilesAccessSettings()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        )
                    )
                }
            }
        }

        if (scanning) {
            Text("Сканируем память…")
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (junk != null && !scanning && grouped.isEmpty()) {
                item { Text("Мусор не найден — телефон в порядке! 🎉") }
            }
            JunkCategory.entries.filter { it in grouped }.forEach { category ->
                val catItems = grouped.getValue(category)
                item(key = category.name) {
                    CategoryCard(
                        category = category,
                        items = catItems,
                        selected = selected,
                        onSelectionChange = { selected = it },
                        expanded = category in expanded,
                        onToggleExpand = {
                            if (category in expanded) expanded -= category else expanded += category
                        },
                    )
                }
                if (category in expanded) {
                    items(catItems, key = { it.file.absolutePath }) { item ->
                        FileRow(item, checked = item in selected) { checked ->
                            selected = if (checked) selected + item else selected - item
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { startScan() }, enabled = !scanning) { Text("Пересканировать") }
            TextButton(onClick = { context.openStorageSettings() }) { Text("Хранилище") }
        }
        Button(
            onClick = { confirmDelete = true },
            enabled = !scanning && selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Очистить ${context.formatBytes(selectedSize)}") }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить файлы?") },
            text = { Text("Будет удалено объектов: ${selected.size} (${context.formatBytes(selectedSize)}). Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    val toDelete = selected
                    scope.launch {
                        val freed = JunkScanner.delete(toDelete)
                        junk = junk.orEmpty() - toDelete
                        selected = emptySet()
                        message = "Освобождено ${context.formatBytes(freed)}"
                    }
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun CategoryCard(
    category: JunkCategory,
    items: List<JunkItem>,
    selected: Set<JunkItem>,
    onSelectionChange: (Set<JunkItem>) -> Unit,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    val context = LocalContext.current
    val selectedCount = items.count { it in selected }
    val state = when (selectedCount) {
        0 -> ToggleableState.Off
        items.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggleExpand)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TriStateCheckbox(state = state, onClick = {
                onSelectionChange(if (state == ToggleableState.On) selected - items.toSet() else selected + items)
            })
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(category.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${category.description} · ${items.size} шт.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(context.formatBytes(items.sumOf { it.sizeBytes }))
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
    }
}

@Composable
private fun FileRow(item: JunkItem, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(Modifier.weight(1f)) {
            Text(item.file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                item.file.parent.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(context.formatBytes(item.sizeBytes), style = MaterialTheme.typography.bodySmall)
    }
}
