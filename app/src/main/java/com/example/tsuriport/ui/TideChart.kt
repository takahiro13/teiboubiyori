package com.example.tsuriport.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsuriport.data.HourlyPoint
import com.example.tsuriport.data.TideEvent
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/** 1日分(0〜24時)の潮位グラフ。満潮・干潮を印付きで表示し、今日なら現在時刻の線を引く。 */
@Composable
fun TideChart(
    date: LocalDate,
    points: List<HourlyPoint>,
    events: List<TideEvent>,
    now: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    val samples = points.filter { it.time.toLocalDate() == date && it.seaLevel != null }
    val measurer = rememberTextMeasurer()
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val nowColor = Color(0xFFE53935)
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    Canvas(modifier.fillMaxWidth().height(190.dp)) {
        if (samples.size < 2) return@Canvas

        val left = 34.dp.toPx()
        val right = 8.dp.toPx()
        val top = 22.dp.toPx()
        val bottom = 20.dp.toPx()
        val w = size.width - left - right
        val h = size.height - top - bottom

        val minV = samples.minOf { it.seaLevel!! } - 0.1
        val maxV = samples.maxOf { it.seaLevel!! } + 0.1
        fun x(hour: Double) = left + (hour / 24.0 * w).toFloat()
        fun y(v: Double) = top + ((maxV - v) / (maxV - minV) * h).toFloat()
        fun hourOf(t: LocalDateTime) = t.hour + t.minute / 60.0

        // 横軸(時刻)
        for (hour in listOf(0, 6, 12, 18, 24)) {
            drawLine(grid, Offset(x(hour.toDouble()), top), Offset(x(hour.toDouble()), top + h))
            val text = measurer.measure("${hour}時", labelStyle)
            drawText(text, topLeft = Offset(x(hour.toDouble()) - text.size.width / 2f, top + h + 3.dp.toPx()))
        }
        // 縦軸(潮位): 0m の基準線と上下端
        // 近すぎる目盛りは重なるので間引く(0m の基準線を優先)
        val minGap = (maxV - minV) * 0.15
        val ticks = buildList {
            if (0.0 in minV..maxV) add(0.0)
            for (v in listOf(maxV - 0.1, minV + 0.1)) {
                if (all { abs(it - v) >= minGap }) add(v)
            }
        }
        for (v in ticks) {
            drawLine(grid, Offset(left, y(v)), Offset(left + w, y(v)))
            val text = measurer.measure("%.1f".format(v), labelStyle)
            drawText(text, topLeft = Offset(left - text.size.width - 4.dp.toPx(), y(v) - text.size.height / 2f))
        }

        // 潮位曲線
        val path = Path()
        samples.forEachIndexed { i, p ->
            val px = x(hourOf(p.time))
            val py = y(p.seaLevel!!)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        val fill = Path().apply {
            addPath(path)
            lineTo(x(hourOf(samples.last().time)), top + h)
            lineTo(x(hourOf(samples.first().time)), top + h)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = 0.35f), line.copy(alpha = 0.03f)), top, top + h))
        drawPath(path, line, style = Stroke(width = 2.5.dp.toPx()))

        // 満潮・干潮
        for (e in events.filter { it.time.toLocalDate() == date }) {
            val cx = x(hourOf(e.time))
            val cy = y(e.height)
            drawCircle(line, 4.dp.toPx(), Offset(cx, cy))
            drawCircle(Color.White, 2.dp.toPx(), Offset(cx, cy))
            val label = measurer.measure(
                "%02d:%02d".format(e.time.hour, e.time.minute),
                labelStyle.copy(color = line),
            )
            val ly = if (e.isHigh) cy - label.size.height - 5.dp.toPx() else cy + 5.dp.toPx()
            val lx = (cx - label.size.width / 2f).coerceIn(left, size.width - label.size.width.toFloat())
            drawText(label, topLeft = Offset(lx, ly))
        }

        // 現在時刻
        if (now.toLocalDate() == date) {
            val nx = x(hourOf(now))
            drawLine(nowColor, Offset(nx, top), Offset(nx, top + h), strokeWidth = 1.5.dp.toPx())
        }
    }
}
