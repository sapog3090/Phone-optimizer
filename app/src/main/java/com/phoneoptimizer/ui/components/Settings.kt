package com.phoneoptimizer.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

private fun Context.startSafely(intent: Intent, fallback: Intent? = null) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        fallback?.let { runCatching { startActivity(it) } }
    }
}

fun Context.openAllFilesAccessSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    startSafely(
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")),
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
    )
}

fun Context.openUsageAccessSettings() =
    startSafely(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

fun Context.openAppDetails(packageName: String) =
    startSafely(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

fun Context.requestUninstall(packageName: String) =
    startSafely(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))

fun Context.openBatterySaverSettings() =
    startSafely(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS), Intent(Settings.ACTION_SETTINGS))

fun Context.openBatteryUsage() =
    startSafely(Intent(Intent.ACTION_POWER_USAGE_SUMMARY), Intent(Settings.ACTION_SETTINGS))

fun Context.openDisplaySettings() =
    startSafely(Intent(Settings.ACTION_DISPLAY_SETTINGS))

fun Context.openStorageSettings() =
    startSafely(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS), Intent(Settings.ACTION_SETTINGS))
