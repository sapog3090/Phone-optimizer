package com.phoneoptimizer.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs

data class MemoryStats(val totalBytes: Long, val availBytes: Long, val isLow: Boolean) {
    val usedBytes get() = totalBytes - availBytes
    val usedFraction get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

data class StorageStats(val totalBytes: Long, val freeBytes: Long) {
    val usedBytes get() = totalBytes - freeBytes
    val usedFraction get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

data class BatteryStats(
    val levelPercent: Int,
    val temperatureC: Float,
    val voltageMv: Int,
    val health: String,
    val status: String,
    val source: String,
    val technology: String,
    val isCharging: Boolean,
    val isOverheated: Boolean,
)

object SystemInfo {

    fun memory(context: Context): MemoryStats {
        val am = context.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return MemoryStats(info.totalMem, info.availMem, info.lowMemory)
    }

    fun storage(): StorageStats {
        val stat = StatFs(Environment.getDataDirectory().path)
        return StorageStats(stat.totalBytes, stat.availableBytes)
    }

    fun battery(context: Context): BatteryStats? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        return parseBattery(intent)
    }

    fun parseBattery(intent: Intent): BatteryStats {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val statusCode = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        val status = when (statusCode) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Заряжается"
            BatteryManager.BATTERY_STATUS_FULL -> "Полностью заряжена"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Разряжается"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Не заряжается"
            else -> "Неизвестно"
        }
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Хорошее"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Перегрев"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Изношена"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Перенапряжение"
            BatteryManager.BATTERY_HEALTH_COLD -> "Переохлаждение"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Неисправность"
            else -> "Неизвестно"
        }
        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Сеть"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Беспроводная"
            else -> "Батарея"
        }

        return BatteryStats(
            levelPercent = if (level >= 0) level * 100 / scale else 0,
            temperatureC = temperature,
            voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
            health = health,
            status = status,
            source = source,
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "—",
            isCharging = statusCode == BatteryManager.BATTERY_STATUS_CHARGING,
            isOverheated = temperature >= 42f || healthCode == BatteryManager.BATTERY_HEALTH_OVERHEAT,
        )
    }

    /** Общая оценка состояния телефона от 0 до 100. */
    fun healthScore(memory: MemoryStats, storage: StorageStats, battery: BatteryStats?): Int {
        var score = 100f
        // Память: штраф, если занято больше 60% ОЗУ
        score -= ((memory.usedFraction - 0.6f).coerceAtLeast(0f) / 0.4f) * 30f
        // Хранилище: штраф, если занято больше 70%
        score -= ((storage.usedFraction - 0.7f).coerceAtLeast(0f) / 0.3f) * 35f
        if (battery != null) {
            if (battery.isOverheated) score -= 20f
            else if (battery.temperatureC > 38f) score -= 8f
            if (battery.levelPercent < 20 && !battery.isCharging) score -= 10f
        }
        return score.toInt().coerceIn(0, 100)
    }
}
