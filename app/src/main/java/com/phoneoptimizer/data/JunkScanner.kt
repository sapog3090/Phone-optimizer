package com.phoneoptimizer.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

enum class JunkCategory(val title: String, val description: String, val safeByDefault: Boolean) {
    APP_CACHE("Кэш оптимизатора", "Временные данные этого приложения", true),
    TEMP("Временные файлы", "Файлы .tmp, .log, .bak и подобные", true),
    THUMBNAILS("Миниатюры", "Превью картинок — система создаст их заново", true),
    EMPTY_FOLDERS("Пустые папки", "Папки без единого файла", true),
    APK("Установочные APK", "Файлы установки приложений, обычно уже не нужны", false),
    LARGE_FILES("Большие файлы", "Файлы больше 100 МБ — проверьте перед удалением", false),
}

data class JunkItem(val file: File, val sizeBytes: Long, val category: JunkCategory)

object JunkScanner {

    private const val LARGE_FILE_BYTES = 100L * 1024 * 1024
    private const val MAX_DEPTH = 12
    private val tempExtensions = setOf("tmp", "temp", "log", "bak", "old", "chk", "dmp")

    fun hasStorageAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    suspend fun scan(context: Context): List<JunkItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<JunkItem>()

        listOfNotNull(context.cacheDir, context.externalCacheDir).forEach { dir ->
            dir.listFiles()?.forEach { result += JunkItem(it, sizeOf(it), JunkCategory.APP_CACHE) }
        }

        if (hasStorageAccess(context)) {
            walk(Environment.getExternalStorageDirectory(), depth = 0, out = result)
        }
        result.sortedByDescending { it.sizeBytes }
    }

    private suspend fun walk(dir: File, depth: Int, out: MutableList<JunkItem>) {
        coroutineContext.ensureActive()
        if (depth > MAX_DEPTH) return
        val children = dir.listFiles() ?: return

        if (children.isEmpty()) {
            if (depth > 0) out += JunkItem(dir, 0, JunkCategory.EMPTY_FOLDERS)
            return
        }

        for (file in children) {
            if (file.isDirectory) {
                when {
                    // Данные приложений недоступны и трогать их нельзя
                    depth == 0 && file.name == "Android" -> Unit
                    file.name == ".thumbnails" ->
                        out += JunkItem(file, sizeOf(file), JunkCategory.THUMBNAILS)
                    else -> walk(file, depth + 1, out)
                }
            } else {
                classify(file)?.let { out += it }
            }
        }
    }

    private fun classify(file: File): JunkItem? {
        val ext = file.extension.lowercase()
        val size = file.length()
        val category = when {
            ext in tempExtensions || file.name.endsWith("~") -> JunkCategory.TEMP
            ext == "apk" -> JunkCategory.APK
            size >= LARGE_FILE_BYTES -> JunkCategory.LARGE_FILES
            else -> return null
        }
        return JunkItem(file, size, category)
    }

    private fun sizeOf(file: File): Long =
        if (file.isDirectory) file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        else file.length()

    /** Удаляет выбранные элементы и возвращает освобождённый объём. */
    suspend fun delete(items: Collection<JunkItem>): Long = withContext(Dispatchers.IO) {
        var freed = 0L
        for (item in items) {
            val ok = if (item.file.isDirectory) item.file.deleteRecursively() else item.file.delete()
            if (ok) freed += item.sizeBytes
        }
        freed
    }
}
