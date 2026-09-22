package com.example.tsuriport.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

/** Open-Meteo (https://open-meteo.com/) から天気・風・波・潮位を取得する。 */
object WeatherRepository {
    private const val FORECAST_DAYS = 7

    suspend fun fetch(port: Port): Forecast = coroutineScope {
        val weather = async { httpGetWithRetry(weatherUrl(port)) }
        // 海洋データが取れなくても天気だけは表示できるようにする
        val marine = async { runCatching { httpGetWithRetry(marineUrl(port)) }.getOrNull() }
        parse(weather.await(), marine.await())
    }

    private fun weatherUrl(p: Port) =
        "https://api.open-meteo.com/v1/forecast?latitude=${p.lat}&longitude=${p.lon}" +
            "&hourly=temperature_2m,weather_code,precipitation_probability," +
            "wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
            "&daily=sunrise,sunset&wind_speed_unit=ms" +
            "&timezone=Asia%2FTokyo&forecast_days=$FORECAST_DAYS"

    private fun marineUrl(p: Port) =
        "https://marine-api.open-meteo.com/v1/marine?latitude=${p.marineLat}&longitude=${p.marineLon}" +
            "&hourly=wave_height,wave_period,sea_level_height_msl" +
            "&timezone=Asia%2FTokyo&forecast_days=$FORECAST_DAYS"

    /** 一時的な通信エラー(タイムアウトなど)に備えて1回だけ再試行する。 */
    private suspend fun httpGetWithRetry(url: String): JSONObject =
        try {
            httpGet(url)
        } catch (e: java.io.IOException) {
            httpGet(url)
        }

    private suspend fun httpGet(url: String): JSONObject = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode !in 200..299) {
                throw java.io.IOException("HTTP ${conn.responseCode}")
            }
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(weather: JSONObject, marine: JSONObject?): Forecast {
        val wh = weather.getJSONObject("hourly")
        val times = wh.getJSONArray("time")

        val mh = marine?.optJSONObject("hourly")
        val marineIndex = mh?.getJSONArray("time")?.let { arr ->
            (0 until arr.length()).associate { arr.getString(it) to it }
        }.orEmpty()

        val hourly = (0 until times.length()).map { i ->
            val t = times.getString(i)
            val mi = marineIndex[t]
            HourlyPoint(
                time = LocalDateTime.parse(t),
                weatherCode = wh.getJSONArray("weather_code").intOrNull(i),
                temperature = wh.getJSONArray("temperature_2m").doubleOrNull(i),
                precipProbability = wh.optJSONArray("precipitation_probability")?.intOrNull(i),
                windSpeed = wh.getJSONArray("wind_speed_10m").doubleOrNull(i),
                windGust = wh.getJSONArray("wind_gusts_10m").doubleOrNull(i),
                windDirection = wh.getJSONArray("wind_direction_10m").intOrNull(i),
                waveHeight = mi?.let { mh?.optJSONArray("wave_height")?.doubleOrNull(it) },
                wavePeriod = mi?.let { mh?.optJSONArray("wave_period")?.doubleOrNull(it) },
                seaLevel = mi?.let { mh?.optJSONArray("sea_level_height_msl")?.doubleOrNull(it) },
            )
        }

        val daily = weather.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val sun = (0 until dates.length()).map { i ->
            DaySun(
                date = java.time.LocalDate.parse(dates.getString(i)),
                sunrise = daily.getJSONArray("sunrise").stringOrNull(i)?.let(LocalDateTime::parse),
                sunset = daily.getJSONArray("sunset").stringOrNull(i)?.let(LocalDateTime::parse),
            )
        }

        return Forecast(
            fetchedAt = LocalDateTime.now(),
            hourly = hourly,
            tideEvents = TideCalculator.findEvents(hourly),
            sun = sun,
            marineFetched = marine != null,
        )
    }

    private fun JSONArray.doubleOrNull(i: Int): Double? =
        if (i >= length() || isNull(i)) null else getDouble(i)

    private fun JSONArray.intOrNull(i: Int): Int? =
        if (i >= length() || isNull(i)) null else getInt(i)

    private fun JSONArray.stringOrNull(i: Int): String? =
        if (i >= length() || isNull(i)) null else getString(i)
}
