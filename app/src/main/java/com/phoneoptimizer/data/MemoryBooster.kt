package com.phoneoptimizer.data

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class BoostResult(val stoppedApps: Int, val freedBytes: Long)

object MemoryBooster {

    /**
     * Останавливает фоновые процессы пользовательских приложений.
     * Android не даёт обычным приложениям трогать системные и активные процессы,
     * поэтому это безопасно: выгружается только то, что система и так может закрыть.
     */
    suspend fun boost(context: Context): BoostResult = withContext(Dispatchers.Default) {
        val before = SystemInfo.memory(context).availBytes
        val am = context.getSystemService(ActivityManager::class.java)
        val apps = context.packageManager.getInstalledApplications(0)

        var stopped = 0
        for (app in apps) {
            if (app.packageName == context.packageName) continue
            if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) continue
            am.killBackgroundProcesses(app.packageName)
            stopped++
        }

        // Даём системе время вернуть освободившуюся память
        delay(1500)
        val after = SystemInfo.memory(context).availBytes
        BoostResult(stopped, (after - before).coerceAtLeast(0))
    }
}
