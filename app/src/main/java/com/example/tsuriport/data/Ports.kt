package com.example.tsuriport.data

import android.content.Context

/**
 * 港・漁港・釣り施設の一覧。assets/ports.csv は tools/build_ports.py で生成する
 * (国土数値情報の港湾・漁港データ + tools/facilities.csv)。
 */
object Ports {
    private const val ASSET = "ports.csv"
    private const val DEFAULT_NAME = "若洲海浜公園(釣り施設)"

    @Volatile
    private var cache: List<Port>? = null

    fun all(context: Context): List<Port> = cache ?: synchronized(this) {
        cache ?: load(context).also { cache = it }
    }

    fun default(context: Context): Port =
        all(context).let { list -> list.firstOrNull { it.name == DEFAULT_NAME } ?: list.first() }

    fun byId(context: Context, id: String?): Port? =
        if (id == null) null else all(context).firstOrNull { it.id == id }

    private fun load(context: Context): List<Port> =
        context.assets.open(ASSET).bufferedReader().useLines { lines ->
            lines.drop(1).filter { it.isNotBlank() }.map { line ->
                val c = line.split(',')
                Port(
                    kind = PortKind.valueOf(c[0].uppercase()),
                    prefecture = c[1],
                    name = c[2],
                    area = c[3],
                    lat = c[4].toDouble(),
                    lon = c[5].toDouble(),
                    marineLat = c[6].toDouble(),
                    marineLon = c[7].toDouble(),
                )
            }.distinctBy { it.id }.toList() // ports.csv に重複行があっても LazyColumn の key 衝突でクラッシュしないようにする
        }
}
