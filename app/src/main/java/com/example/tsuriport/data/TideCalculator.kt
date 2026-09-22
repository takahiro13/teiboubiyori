package com.example.tsuriport.data

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.floor

object TideCalculator {
    /** これより小さい振幅の山谷は、モデルの揺らぎとみなして無視する (m) */
    private const val MIN_SWING = 0.15

    /** 1時間ごとの潮位から満潮・干潮の時刻と潮位を求める(放物線補間で時刻を補正)。 */
    fun findEvents(points: List<HourlyPoint>): List<TideEvent> {
        val raw = mutableListOf<TideEvent>()
        for (i in 1 until points.size - 1) {
            val y0 = points[i - 1].seaLevel ?: continue
            val y1 = points[i].seaLevel ?: continue
            val y2 = points[i + 1].seaLevel ?: continue
            val isHigh = y1 > y0 && y1 >= y2
            val isLow = y1 < y0 && y1 <= y2
            if (!isHigh && !isLow) continue

            val denom = y0 - 2 * y1 + y2
            val offset = if (denom != 0.0) (0.5 * (y0 - y2) / denom).coerceIn(-0.5, 0.5) else 0.0
            raw += TideEvent(
                time = points[i].time.plusSeconds((offset * 3600).toLong()),
                height = y1 - 0.25 * (y0 - y2) * offset,
                isHigh = isHigh,
            )
        }

        // 小さな山谷を除去して、満潮と干潮が交互に並ぶようにする
        val result = ArrayDeque<TideEvent>()
        for (e in raw) {
            val last = result.lastOrNull()
            if (last != null && abs(e.height - last.height) < MIN_SWING) {
                result.removeLast()
            } else {
                result.addLast(e)
            }
        }
        return result.toList()
    }

    /** 月齢(0〜29.5)。新月=0。 */
    fun moonAge(date: LocalDate): Double {
        val jd = date.toEpochDay() + 2440587.5 + 0.125 // 日本時間の正午ごろ
        val synodic = 29.530588853
        val age = (jd - 2451550.1) % synodic
        return if (age < 0) age + synodic else age
    }

    /** 月齢から求める潮回りの目安(大潮・中潮・小潮・長潮・若潮)。 */
    fun tideName(date: LocalDate): String = when (floor(moonAge(date)).toInt()) {
        in 0..2, 29 -> "大潮"
        in 3..5 -> "中潮"
        in 6..8 -> "小潮"
        9 -> "長潮"
        10 -> "若潮"
        in 11..13 -> "中潮"
        in 14..17 -> "大潮"
        in 18..20 -> "中潮"
        in 21..23 -> "小潮"
        24 -> "長潮"
        25 -> "若潮"
        else -> "中潮"
    }
}
