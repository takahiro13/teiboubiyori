package com.example.tsuriport.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tsuriport.MainViewModel
import com.example.tsuriport.UiState
import com.example.tsuriport.data.Assessment
import com.example.tsuriport.data.Conditions
import com.example.tsuriport.data.Forecast
import com.example.tsuriport.data.HourlyPoint
import com.example.tsuriport.data.Level
import com.example.tsuriport.data.Port
import com.example.tsuriport.data.PortKind
import com.example.tsuriport.data.Ports
import com.example.tsuriport.data.TideCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat = DateTimeFormatter.ofPattern("M/d(E)", Locale.JAPAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val port by viewModel.port.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextButton(onClick = { showPicker = true }) {
                        Column {
                            Text("📍 ${port.name} ▾", fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(
                                listOf(port.prefecture, port.area).filter { it.isNotEmpty() }.joinToString(" "),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { showAbout = true }) { Text("出典") }
                    TextButton(onClick = viewModel::refresh) { Text("更新") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is UiState.Error -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(s.message, textAlign = TextAlign.Center)
                    Spacer(Modifier.padding(8.dp))
                    Button(onClick = viewModel::refresh) { Text("再試行") }
                }
                is UiState.Success -> ForecastContent(s.forecast)
            }
        }
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    if (showPicker) {
        val context = LocalContext.current
        PortPicker(
            ports = remember { Ports.all(context) },
            current = port,
            onSelect = {
                viewModel.selectPort(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
        title = { Text("出典・ご注意") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("天気・風", fontWeight = FontWeight.Bold)
                Text("Weather data by Open-Meteo.com (CC BY 4.0)\nhttps://open-meteo.com/")
                Text("波・潮位", fontWeight = FontWeight.Bold)
                Text("Open-Meteo Marine API の海洋モデルによる推定値です。実際の港の波・潮位とは差があります。満潮・干潮の時刻は1時間ごとの値から求めた目安です。")
                Text("港・漁港の位置", fontWeight = FontWeight.Bold)
                Text(
                    "「国土数値情報(港湾データ C02-14、漁港データ C09-06)」(国土交通省)を加工して作成。\n" +
                        "https://nlftp.mlit.go.jp/ksj/\n" +
                        "位置は代表点で、波・潮位の取得位置は港の沖側に補正している場合があります。",
                )
                Text("釣り施設の位置", fontWeight = FontWeight.Bold)
                Text("作者が入力したおおよその位置です。")
                Text("ご注意", fontWeight = FontWeight.Bold)
                Text("釣行・出航の判断には、気象庁や海上保安庁の最新情報を必ず確認してください。本アプリの情報により生じた損害について、作者は責任を負いません。")
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PortPicker(ports: List<Port>, current: Port, onSelect: (Port) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf<String?>(null) }

    val grouped = remember(query, kind, ports) {
        val q = query.trim()
        ports
            .filter { p ->
                (kind == null || p.kind.name == kind) &&
                    (q.isEmpty() || p.name.contains(q) || p.area.contains(q) || p.prefecture.contains(q))
            }
            .groupBy { it.prefecture }
    }
    val count = grouped.values.sumOf { it.size }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.systemBarsPadding().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("場所を選択(${count}件)", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("閉じる") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("港名・市町村・都道府県で検索") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == null, onClick = { kind = null }, label = { Text("すべて") })
                    PortKind.entries.forEach { k ->
                        FilterChip(
                            selected = kind == k.name,
                            onClick = { kind = if (kind == k.name) null else k.name },
                            label = { Text(k.label) },
                        )
                    }
                }
                if (count == 0) {
                    Text("該当する場所がありません。", modifier = Modifier.padding(16.dp))
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    grouped.forEach { (prefecture, list) ->
                        stickyHeader(key = "h-$prefecture") {
                            Text(
                                prefecture,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(vertical = 6.dp),
                            )
                        }
                        items(list, key = { it.id }) { p ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(p) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    p.label,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = if (p.id == current.id) FontWeight.Bold else FontWeight.Normal,
                                )
                                Text(p.kind.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastContent(forecast: Forecast) {
    val now = LocalDateTime.now()
    val dates = forecast.dates
    var dayIndex by rememberSaveable { mutableIntStateOf(0) }
    val date = dates.getOrElse(dayIndex) { dates.first() }
    val current = forecast.hourly.lastOrNull { !it.time.isAfter(now) } ?: forecast.hourly.first()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NowCard(current, forecast, now)

        WeekCard(
            days = remember(forecast) { summarize(forecast.hourly) },
            selected = date,
            onSelect = { dayIndex = dates.indexOf(it).coerceAtLeast(0) },
        )

        ScrollableTabRow(selectedTabIndex = dates.indexOf(date).coerceAtLeast(0), edgePadding = 0.dp) {
            dates.forEachIndexed { i, d ->
                Tab(
                    selected = d == date,
                    onClick = { dayIndex = i },
                    text = { Text(if (i == 0) "今日 ${d.format(dayFormat)}" else d.format(dayFormat)) },
                )
            }
        }

        TideCard(forecast, date, now)
        HourlyCard(forecast.hourly.filter { it.time.toLocalDate() == date }, now)

        Text(
            "天気・風は Open-Meteo の予報、波・潮位は海洋モデルによる推定値です。実際の港の潮位・波とは差があります。" +
                "出航・釣行の判断には気象庁や海上保安庁の最新情報を必ず確認してください。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun levelColor(level: Level): Color = when (level) {
    Level.GOOD -> Color(0xFF2E7D32)
    Level.CAUTION -> Color(0xFFEF8A00)
    Level.DANGER -> Color(0xFFC62828)
}

@Composable
private fun NowCard(current: HourlyPoint, forecast: Forecast, now: LocalDateTime) {
    val assessment = Conditions.assess(current)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Conditions.weatherIcon(current.weatherCode), fontSize = 48.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(Conditions.weatherText(current.weatherCode), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "現在 ${current.temperature?.let { "%.1f℃".format(it) } ?: "--"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                LevelBadge(assessment)
            }
            Text(assessment.reasons.joinToString(" / "), style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(
                    label = "風速",
                    value = current.windSpeed?.let { "%.1f".format(it) } ?: "--",
                    unit = "m/s",
                    sub = "${Conditions.directionName(current.windDirection)}から  突風 ${current.windGust?.let { "%.1f".format(it) } ?: "--"}",
                    arrowDeg = current.windDirection,
                    modifier = Modifier.weight(1f),
                )
                Metric(
                    label = "波高",
                    value = current.waveHeight?.let { "%.1f".format(it) } ?: "--",
                    unit = "m",
                    sub = "周期 ${current.wavePeriod?.let { "%.0f秒".format(it) } ?: "--"}",
                    modifier = Modifier.weight(1f),
                )
            }

            val next = forecast.tideEvents.firstOrNull { it.time.isAfter(now) }
            val name = TideCalculator.tideName(now.toLocalDate())
            Text(
                buildString {
                    append("🌊 今日は${name}")
                    if (next != null) {
                        append("  次は${if (next.isHigh) "満潮" else "干潮"} ${next.time.format(timeFormat)}")
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun LevelBadge(a: Assessment) {
    Text(
        a.level.label,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(levelColor(a.level), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun Metric(
    label: String,
    value: String,
    unit: String,
    sub: String,
    modifier: Modifier = Modifier,
    arrowDeg: Int? = null,
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.Bottom) {
            if (arrowDeg != null) {
                // 風向は「吹いてくる方角」なので、矢印は吹いていく向きに回転させる
                Text("↓", fontSize = 26.sp, modifier = Modifier.rotate(arrowDeg.toFloat()).padding(end = 6.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(unit, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
        }
        Text(sub, style = MaterialTheme.typography.bodySmall)
    }
}

private data class DaySummary(
    val date: LocalDate,
    val icon: String,
    val tempMax: Double?,
    val tempMin: Double?,
    val windMax: Double?,
    val waveMax: Double?,
    val rainMax: Int?,
    val level: Level,
)

/** 1日ごとのまとめ。風・波・目安は釣りをする日中(4〜20時)の最悪値。天気は正午のもの。 */
private fun summarize(points: List<HourlyPoint>): List<DaySummary> =
    points.groupBy { it.time.toLocalDate() }.map { (date, list) ->
        val day = list.filter { it.time.hour in 4..20 }.ifEmpty { list }
        val noon = list.firstOrNull { it.time.hour == 12 } ?: list.first()
        DaySummary(
            date = date,
            icon = Conditions.weatherIcon(noon.weatherCode),
            tempMax = list.mapNotNull { it.temperature }.maxOrNull(),
            tempMin = list.mapNotNull { it.temperature }.minOrNull(),
            windMax = day.mapNotNull { it.windSpeed }.maxOrNull(),
            waveMax = day.mapNotNull { it.waveHeight }.maxOrNull(),
            rainMax = day.mapNotNull { it.precipProbability }.maxOrNull(),
            level = day.map { Conditions.assess(it).level }.maxBy { it.ordinal },
        )
    }

@Composable
private fun WeekCard(days: List<DaySummary>, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    SectionCard("📅 週間予報") {
        Row(Modifier.fillMaxWidth()) {
            HeaderCell("日付", 1.3f)
            HeaderCell("天気", 0.7f)
            HeaderCell("気温", 1.0f)
            HeaderCell("風 m/s", 0.9f)
            HeaderCell("波 m", 0.8f)
            HeaderCell("雨%", 0.8f)
            HeaderCell("目安", 0.6f)
        }
        HorizontalDivider()
        days.forEach { d ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (d.date == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onSelect(d.date) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(d.date.format(dayFormat), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(TideCalculator.tideName(d.date), style = MaterialTheme.typography.labelSmall)
                }
                Cell(d.icon, 0.7f)
                Cell("${d.tempMax?.let { "%.0f".format(it) } ?: "--"}/${d.tempMin?.let { "%.0f".format(it) } ?: "--"}°", 1.0f)
                Cell(d.windMax?.let { "%.0f".format(it) } ?: "--", 0.9f)
                Cell(d.waveMax?.let { "%.1f".format(it) } ?: "--", 0.8f)
                Cell(d.rainMax?.toString() ?: "--", 0.8f)
                Box(Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(levelColor(d.level)))
                }
            }
        }
        Text(
            "目安: 緑=釣り日和 / 橙=注意 / 赤=危険(日中の最悪値)。風・波は日中の最大値。" +
                "先の日ほど予報の精度は下がります。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun TideCard(forecast: Forecast, date: LocalDate, now: LocalDateTime) {
    val events = forecast.tideEvents.filter { it.time.toLocalDate() == date }
    val sun = forecast.sun.firstOrNull { it.date == date }
    val hasData = forecast.hourly.any { it.time.toLocalDate() == date && it.seaLevel != null }

    SectionCard("🌊 潮汐(${TideCalculator.tideName(date)})") {
        if (!hasData) {
            Text(
                if (forecast.marineFetched) "この地点の潮位データはありません。"
                else "波・潮位データの取得に失敗しました(通信エラー)。右上の「更新」で再試行してください。",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@SectionCard
        }
        TideChart(date, forecast.hourly, forecast.tideEvents, now)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            events.forEach { e ->
                Column(
                    Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (e.isHigh) "満潮" else "干潮",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (e.isHigh) Color(0xFFD84315) else MaterialTheme.colorScheme.primary,
                    )
                    Text(e.time.format(timeFormat), fontWeight = FontWeight.Bold)
                    Text("%.2fm".format(e.height), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (sun != null) {
            Text(
                "🌅 日の出 ${sun.sunrise?.format(timeFormat) ?: "--"}   🌇 日の入り ${sun.sunset?.format(timeFormat) ?: "--"}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            "潮位は平均海面からの高さ(モデル推定)。満潮・干潮の時刻は目安です。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun HourlyCard(points: List<HourlyPoint>, now: LocalDateTime) {
    SectionCard("🕐 1時間ごとの予報") {
        Row(Modifier.fillMaxWidth()) {
            HeaderCell("時刻", 0.9f)
            HeaderCell("天気", 0.7f)
            HeaderCell("気温", 0.9f)
            HeaderCell("風 m/s", 1.6f)
            HeaderCell("波 m", 0.9f)
            HeaderCell("潮位 m", 1.0f)
            HeaderCell("雨%", 0.8f)
        }
        HorizontalDivider()
        points.forEach { p ->
            val isNow = p.time.toLocalDate() == now.toLocalDate() && p.time.hour == now.hour
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (isNow) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cell("%02d:00".format(p.time.hour), 0.9f)
                Cell(Conditions.weatherIcon(p.weatherCode), 0.7f)
                Cell(p.temperature?.let { "%.0f°".format(it) } ?: "--", 0.9f)
                Row(Modifier.weight(1.6f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (p.windDirection != null) {
                        Text("↓", fontSize = 13.sp, modifier = Modifier.rotate(p.windDirection.toFloat()))
                    }
                    val wind = p.windSpeed?.let { "%.1f".format(it) } ?: "--"
                    val gust = p.windGust?.let { "(%.0f)".format(it) } ?: ""
                    Text(
                        " $wind$gust",
                        fontSize = 13.sp,
                        color = if ((p.windSpeed ?: 0.0) >= 6) Color(0xFFEF8A00) else Color.Unspecified,
                    )
                }
                Cell(p.waveHeight?.let { "%.1f".format(it) } ?: "--", 0.9f)
                Cell(p.seaLevel?.let { "%+.2f".format(it) } ?: "--", 1.0f)
                Cell(p.precipProbability?.let { "$it" } ?: "--", 0.8f)
            }
        }
        Text(
            "風向の矢印は風が吹いていく向き、( )内は突風。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text,
        modifier = Modifier.weight(weight).padding(bottom = 4.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(text: String, weight: Float) {
    Text(text, modifier = Modifier.weight(weight), textAlign = TextAlign.Center, fontSize = 13.sp)
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
