#!/usr/bin/env python3
"""国土数値情報の港湾(C02)・漁港(C09)データと facilities.csv から app/src/main/assets/ports.csv を作る。

  python3 tools/build_ports.py            # ダウンロード + 変換 + Open-Meteo での波データ確認
  python3 tools/build_ports.py --no-verify   # 波データ確認を省く(APIを使わない)

使用許諾: 国土数値情報の港湾・漁港データはどちらも「非商用」。アプリ内に出典を表示すること。
"""
import csv, datetime, fcntl, json, math, struct, sys, time, urllib.error, urllib.request, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CACHE = ROOT / "tools" / ".cache"
OUT = ROOT / "app" / "src" / "main" / "assets" / "ports.csv"
UA = "TsuriPortApp-build/1.0 (personal project)"
SOURCES = {
    "C02": "https://nlftp.mlit.go.jp/ksj/gml/data/C02/C02-14/C02-14_GML.zip",
    "C09": "https://nlftp.mlit.go.jp/ksj/gml/data/C09/C09-06/C09-06_GML.zip",
}
PREFS = "北海道 青森県 岩手県 宮城県 秋田県 山形県 福島県 茨城県 栃木県 群馬県 埼玉県 千葉県 東京都 神奈川県 新潟県 富山県 石川県 福井県 山梨県 長野県 岐阜県 静岡県 愛知県 三重県 滋賀県 京都府 大阪府 兵庫県 奈良県 和歌山県 鳥取県 島根県 岡山県 広島県 山口県 徳島県 香川県 愛媛県 高知県 福岡県 佐賀県 長崎県 熊本県 大分県 宮崎県 鹿児島県 沖縄県".split()


def fetch(key):
    CACHE.mkdir(parents=True, exist_ok=True)
    path = CACHE / f"{key}.zip"
    if not path.exists():
        req = urllib.request.Request(SOURCES[key], headers={"User-Agent": UA})
        path.write_bytes(urllib.request.urlopen(req, timeout=120).read())
    return zipfile.ZipFile(path)


def read_dbf(data, enc="cp932"):
    n, hl, rl = struct.unpack("<xxxxIHH", data[:12])
    fields, o = [], 32
    while data[o] != 0x0D:
        fields.append((data[o:o + 11].split(b"\0")[0].decode(), data[o + 16]))
        o += 32
    rows = []
    for i in range(n):
        r, p, d = data[hl + i * rl: hl + (i + 1) * rl], 1, {}
        for k, ln in fields:
            d[k] = r[p:p + ln].decode(enc, "replace").strip()
            p += ln
        rows.append(d)
    return rows


def read_shapes(data):
    """各レコードの代表点(lon, lat)。点はそのまま、線・面は外接矩形の中心。"""
    o, pts = 100, []
    while o < len(data):
        _, cl = struct.unpack(">ii", data[o:o + 8])
        rec = data[o + 8:o + 8 + cl * 2]
        t = struct.unpack("<i", rec[:4])[0]
        if t == 1:
            pts.append(struct.unpack("<dd", rec[4:20]))
        elif t in (3, 5, 8):
            x0, y0, x1, y1 = struct.unpack("<dddd", rec[4:36])
            pts.append(((x0 + x1) / 2, (y0 + y1) / 2))
        else:
            pts.append(None)
        o += 8 + cl * 2
    return pts


def load_layer(zf, suffix):
    base = next(n for n in zf.namelist() if n.endswith(suffix + ".dbf"))[:-4]
    return read_dbf(zf.read(base + ".dbf")), read_shapes(zf.read(base + ".shp"))


def ports():
    rows, pts = load_layer(fetch("C02"), "PortAndHarbor")
    for r, p in zip(rows, pts):
        name = r["C02_005"]
        if p and name:
            yield "port", PREFS[int(r["C02_003"][:2]) - 1], name if name.endswith("港") else name + "港", r["C02_007"], p[1], p[0]


def fishing_ports():
    rows, pts = load_layer(fetch("C09"), "FishingPort")
    for r, p in zip(rows, pts):
        name = r["C09_002"]
        if p and name:
            yield "fishing", PREFS[int(r["C09_003"][:2]) - 1], name if name.endswith("港") else name + "漁港", r["C09_008"].replace(".", " "), p[1], p[0]


def facilities():
    lines = [l for l in (ROOT / "tools" / "facilities.csv").read_text(encoding="utf-8").splitlines() if l and not l.startswith("#")]
    for pref, name, lat, lon in csv.reader(lines):
        assert pref in PREFS, pref
        yield "facility", pref, name, "", float(lat), float(lon)


CACHE_FILE = CACHE / "marine_ok.json"  # "lat,lon" -> 波・潮位が取れたか。無料枠(1時間5,000回)を超えても再開できるようにする


def _load_cache():
    return json.loads(CACHE_FILE.read_text()) if CACHE_FILE.exists() else {}


def _request(url):
    """429 は待って再試行する。分あたりの上限なら1分、時間あたりなら次の正時まで待つ。"""
    for attempt in range(6):
        try:
            return json.load(urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": UA}), timeout=90))
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8", "replace")
            if e.code != 429:
                print("  retry:", e, file=sys.stderr)
                time.sleep(20 * (attempt + 1))
                continue
            if "Daily" in body:
                # 1日10,000回の上限。UTC 0時 = 日本時間 9時 にリセットされる。確認済みの分は保存済みなので、再実行すれば続きから進む
                raise SystemExit("Open-Meteo の1日の上限に達しました。日本時間 9:00 以降に、もう一度実行してください(続きから再開します)")
            if "Hourly" in body:
                now = datetime.datetime.now()
                wake = (now + datetime.timedelta(hours=1)).replace(minute=1, second=0, microsecond=0)
                print(f"  時間あたりの上限。{wake:%H:%M} まで待ちます", flush=True)
                time.sleep((wake - now).total_seconds())
            else:
                print("  分あたりの上限。65秒待ちます", flush=True)
                time.sleep(65)
        except Exception as e:  # 一時的なタイムアウトなど
            print("  retry:", e, file=sys.stderr)
            time.sleep(20 * (attempt + 1))
    raise SystemExit("Open-Meteo に接続できませんでした")


def marine_ok(points):
    """points: [(lat,lon)] → 各点で波高・潮位が取れるか。Open-Meteo の複数地点リクエストを使う。"""
    cache = _load_cache()
    todo = [p for p in dict.fromkeys(points) if f"{p[0]},{p[1]}" not in cache]
    for i in range(0, len(todo), 100):
        chunk = todo[i:i + 100]
        url = ("https://marine-api.open-meteo.com/v1/marine?latitude=" + ",".join(str(p[0]) for p in chunk)
               + "&longitude=" + ",".join(str(p[1]) for p in chunk)
               + "&hourly=wave_height,sea_level_height_msl&forecast_days=1&timezone=Asia%2FTokyo")
        d = _request(url)
        d = d if isinstance(d, list) else [d]
        for p, x in zip(chunk, d):
            h = x["hourly"]
            cache[f"{p[0]},{p[1]}"] = (any(v is not None for v in h["wave_height"])
                                       and any(v is not None for v in h["sea_level_height_msl"]))
        CACHE_FILE.write_text(json.dumps(cache))
        print(f"  確認済み {min(i + 100, len(todo))}/{len(todo)}", flush=True)
        time.sleep(12)  # 無料枠は 600回/分
    return [cache[f"{p[0]},{p[1]}"] for p in points]


def offsets():
    """元の座標に近い順の補正候補。"""
    c = [(dy * 0.04, dx * 0.04) for dy in range(-6, 7) for dx in range(-6, 7) if (dy, dx) != (0, 0)]
    return sorted(c, key=lambda t: math.hypot(*t))[:40]


def main():
    CACHE.mkdir(parents=True, exist_ok=True)
    lock = open(CACHE / "build.lock", "w")  # 二重起動すると無料枠を使い切るので防ぐ
    try:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        raise SystemExit("すでに別の build_ports.py が動いています")
    verify = "--no-verify" not in sys.argv
    recs = list(ports()) + list(fishing_ports()) + list(facilities())
    print(f"{len(recs)} 件 (港湾+漁港+施設)")

    rows = []
    for kind, pref, name, area, lat, lon in recs:
        for s in (pref, name, area):
            assert "," not in s and '"' not in s, s
        rows.append([kind, pref, name, area, round(lat, 5), round(lon, 5), round(lat, 5), round(lon, 5)])

    if verify:
        ok = marine_ok([(r[4], r[5]) for r in rows])
        bad = [i for i, v in enumerate(ok) if not v]
        print(f"波・潮位が空の地点: {len(bad)} / {len(rows)}")
        fixed = 0
        for i in bad:
            cands = [(round(rows[i][4] + dy, 5), round(rows[i][5] + dx, 5)) for dy, dx in offsets()]
            for c, v in zip(cands, marine_ok(cands)):
                if v:
                    rows[i][6], rows[i][7] = c
                    fixed += 1
                    break
        print(f"座標補正できた: {fixed} / {len(bad)}(残りは波・潮位なしで表示)")

    order = {"port": 0, "fishing": 1, "facility": 2}
    rows.sort(key=lambda r: (PREFS.index(r[1]), order[r[0]], r[2], r[3]))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("w", encoding="utf-8", newline="") as f:
        f.write("kind,pref,name,area,lat,lon,mlat,mlon\n")
        csv.writer(f, lineterminator="\n").writerows(rows)
    print(f"書き出し: {OUT} ({OUT.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
