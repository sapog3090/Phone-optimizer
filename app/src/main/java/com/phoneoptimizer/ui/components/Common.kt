package com.phoneoptimizer.ui.components

import android.content.Context
import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoneoptimizer.ui.theme.levelColor

/** Увеличивается при каждом возврате в приложение — экраны по нему обновляют данные. */
val LocalResumeTick = compositionLocalOf { 0 }

fun Context.formatBytes(bytes: Long): String = Formatter.formatShortFileSize(this, bytes)

@Composable
fun ScoreRing(score: Int, size: Dp = 180.dp) {
    val animated by animateFloatAsState(score / 100f, tween(900), label = "score")
    val color = levelColor(1f - animated)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 135f, 270f, useCenter = false, style = stroke)
            drawArc(color, 135f, 270f * animated, useCenter = false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(animated * 100).toInt()}", fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Text(
                when {
                    score >= 80 -> "Отлично"
                    score >= 60 -> "Нормально"
                    else -> "Нужна оптимизация"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun UsageCard(icon: ImageVector, title: String, fraction: Float, details: String) {
    val animated by animateFloatAsState(fraction, tween(700), label = "usage")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${(fraction * 100).toInt()}%", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = levelColor(fraction),
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(8.dp))
            Text(details, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PermissionCard(text: String, buttonText: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClick) { Text(buttonText) }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
fun ScreenTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
}
