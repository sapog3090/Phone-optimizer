package com.phoneoptimizer.data

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Bitmap,
    /** Размер приложения с данными и кэшем, null — нет доступа к статистике. */
    val sizeBytes: Long?,
    val cacheBytes: Long?,
    /** Время последнего использования, null — неизвестно. */
    val lastUsedMillis: Long?,
) {
    fun daysSinceUse(now: Long = System.currentTimeMillis()): Long? =
        lastUsedMillis?.let { TimeUnit.MILLISECONDS.toDays(now - it) }
}

object AppsRepository {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun loadUserApps(context: Context): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val usageAccess = hasUsageAccess(context)
        val storageStats = context.getSystemService(StorageStatsManager::class.java)
        val lastUsed = if (usageAccess) lastUsedMap(context) else emptyMap()
        val iconSize = 96

        pm.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 && it.packageName != context.packageName }
            .map { app ->
                val stats = if (usageAccess) {
                    runCatching {
                        storageStats.queryStatsForPackage(
                            StorageManager.UUID_DEFAULT, app.packageName, Process.myUserHandle()
                        )
                    }.getOrNull()
                } else null

                AppEntry(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    icon = pm.getApplicationIcon(app).toBitmap(iconSize, iconSize),
                    sizeBytes = stats?.let { it.appBytes + it.dataBytes + it.cacheBytes },
                    cacheBytes = stats?.cacheBytes,
                    lastUsedMillis = lastUsed[app.packageName],
                )
            }
            .sortedByDescending { it.sizeBytes ?: 0 }
    }

    private fun lastUsedMap(context: Context): Map<String, Long> {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(365)
        return usm.queryAndAggregateUsageStats(start, end)
            .mapValues { it.value.lastTimeUsed }
            .filterValues { it > 0 }
    }
}
