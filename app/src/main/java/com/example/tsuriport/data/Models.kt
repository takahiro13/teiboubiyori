package com.example.tsuriport.data

import java.time.LocalDate
import java.time.LocalDateTime

enum class PortKind(val label: String) {
    PORT("港"),
    FISHING("漁港"),
    FACILITY("釣り施設"),
}

data class Port(
    val kind: PortKind,
    val prefecture: String,
    val name: String,
    /** 市区町村や管理者など。同名の港を見分けるための補足。 */
    val area: String,
    val lat: Double,
    val lon: Double,
    /** 波・潮位の取得に使う座標。湾奥などで波データが空になる港は、沖寄りに補正してある。 */
    val marineLat: Double = lat,
    val marineLon: Double = lon,
) {
    /** 保存・比較用のキー。 */
    val id: String get() = "${kind.name}|$prefecture|$name|$area|$lat|$lon"

    /** 一覧に出す補足つきの名前。 */
    val label: String get() = if (area.isEmpty()) name else "$name($area)"
}

data class HourlyPoint(
    val time: LocalDateTime,
    val weatherCode: Int?,
    val temperature: Double?,
    val precipProbability: Int?,
    val windSpeed: Double?,
    val windGust: Double?,
    val windDirection: Int?,
    val waveHeight: Double?,
    val wavePeriod: Double?,
    val seaLevel: Double?,
)

data class TideEvent(
    val time: LocalDateTime,
    val height: Double,
    val isHigh: Boolean,
)

data class DaySun(
    val date: LocalDate,
    val sunrise: LocalDateTime?,
    val sunset: LocalDateTime?,
)

data class Forecast(
    val fetchedAt: LocalDateTime,
    val hourly: List<HourlyPoint>,
    val tideEvents: List<TideEvent>,
    val sun: List<DaySun>,
    /** 波・潮位の取得に失敗した(通信エラーなど)場合は false。地点にデータが無い場合とは区別する。 */
    val marineFetched: Boolean = true,
) {
    val dates: List<LocalDate> get() = hourly.map { it.time.toLocalDate() }.distinct()
}
