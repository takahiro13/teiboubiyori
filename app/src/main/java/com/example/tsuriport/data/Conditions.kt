package com.example.tsuriport.data

enum class Level(val label: String) {
    GOOD("釣り日和"),
    CAUTION("注意"),
    DANGER("危険"),
}

data class Assessment(val level: Level, val reasons: List<String>)

/** 風・波・天気から釣行の目安を出す。あくまで簡易的な基準。 */
object Conditions {
    fun assess(p: HourlyPoint): Assessment {
        val danger = mutableListOf<String>()
        val caution = mutableListOf<String>()

        val wind = p.windSpeed
        val gust = p.windGust
        val wave = p.waveHeight
        val code = p.weatherCode

        if (wind != null && wind >= 10) danger += "風が強い(${fmt(wind)}m/s)"
        else if (wind != null && wind >= 6) caution += "風がやや強い(${fmt(wind)}m/s)"

        if (gust != null && gust >= 12) danger += "突風あり(${fmt(gust)}m/s)"
        else if (gust != null && gust >= 9) caution += "突風に注意(${fmt(gust)}m/s)"

        if (wave != null && wave >= 2.0) danger += "波が高い(${fmt(wave)}m)"
        else if (wave != null && wave >= 1.0) caution += "波がやや高い(${fmt(wave)}m)"

        if (code != null && code >= 95) danger += "雷雨"
        else if (code != null && (code in 63..67 || code in 81..86)) caution += "強めの雨・雪"
        else if ((p.precipProbability ?: 0) >= 60) caution += "降水確率が高い"

        return when {
            danger.isNotEmpty() -> Assessment(Level.DANGER, danger + caution)
            caution.isNotEmpty() -> Assessment(Level.CAUTION, caution)
            else -> Assessment(Level.GOOD, listOf("風・波ともに穏やか"))
        }
    }

    private fun fmt(v: Double) = "%.1f".format(v)

    fun weatherText(code: Int?): String = when (code) {
        null -> "--"
        0 -> "快晴"
        1 -> "晴れ"
        2 -> "一部曇り"
        3 -> "曇り"
        45, 48 -> "霧"
        51, 53, 55 -> "霧雨"
        56, 57 -> "着氷性の霧雨"
        61 -> "小雨"
        63 -> "雨"
        65 -> "大雨"
        66, 67 -> "着氷性の雨"
        71, 73, 75 -> "雪"
        77 -> "霧雪"
        80 -> "にわか雨"
        81 -> "にわか雨"
        82 -> "激しいにわか雨"
        85, 86 -> "にわか雪"
        95 -> "雷雨"
        96, 99 -> "雹を伴う雷雨"
        else -> "--"
    }

    fun weatherIcon(code: Int?): String = when (code) {
        null -> "・"
        0, 1 -> "☀️"
        2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67 -> "🌧️"
        in 71..77, 85, 86 -> "❄️"
        in 80..82 -> "🌦️"
        in 95..99 -> "⛈️"
        else -> "・"
    }

    private val directions = listOf(
        "北", "北北東", "北東", "東北東", "東", "東南東", "南東", "南南東",
        "南", "南南西", "南西", "西南西", "西", "西北西", "北西", "北北西",
    )

    /** 風が吹いてくる方角(例: 北西)。 */
    fun directionName(deg: Int?): String =
        if (deg == null) "--" else directions[((deg + 11.25) / 22.5).toInt() % 16]
}
