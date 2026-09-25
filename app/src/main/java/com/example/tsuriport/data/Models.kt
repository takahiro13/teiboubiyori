package com.example.tsuriport.data

import java.time.LocalDate
import java.time.LocalDateTime

enum class PortKind(val label: String) {
    PORT("港"),
    FISHING("漁港"),
    FACILITY("釣り施設"),
}

/** 港選択画面で地方ごとにジャンプするための区分。都道府県は ports.csv と同じ北から南の順。 */
enum class Region(val label: String, val prefectures: List<String>) {
    HOKKAIDO("北海道", listOf("北海道")),
    TOHOKU("東北", listOf("青森県", "岩手県", "宮城県", "秋田県", "山形県", "福島県")),
    KANTO("関東", listOf("茨城県", "栃木県", "群馬県", "埼玉県", "千葉県", "東京都", "神奈川県")),
    CHUBU("中部", listOf("新潟県", "富山県", "石川県", "福井県", "山梨県", "長野県", "岐阜県", "静岡県", "愛知県")),
    KINKI("近畿", listOf("三重県", "滋賀県", "京都府", "大阪府", "兵庫県", "奈良県", "和歌山県")),
    CHUGOKU("中国", listOf("鳥取県", "島根県", "岡山県", "広島県", "山口県")),
    SHIKOKU("四国", listOf("徳島県", "香川県", "愛媛県", "高知県")),
    KYUSHU("九州・沖縄", listOf("福岡県", "佐賀県", "長崎県", "熊本県", "大分県", "宮崎県", "鹿児島県", "沖縄県"));

    companion object {
        fun of(prefecture: String): Region? = entries.firstOrNull { prefecture in it.prefectures }
    }
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
