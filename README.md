# 堤防日和

日本の港で釣りをする人向けの Android アプリ。天気・風速・波高・満潮干潮を港ごとに表示する。

## セットアップ

`app/src/main/assets/ports.csv`(港・漁港・釣り施設のデータ)はリポジトリに含めていない。
国土数値情報(港湾・漁港データ)を加工したもので、非商用ライセンスのため公開リポジトリでの再配布を避けている。
ビルド前に生成する。

```bash
python3 tools/build_ports.py
```

Open-Meteo Marine API への問い合わせを伴うため、無料枠の上限(1日10,000回)に近い場合は
日本時間 9:00 のリセット以降に実行する。`tools/.cache/` に進捗を保存するため、中断しても再実行で続きから再開する。

生成後、通常どおりビルドできる。

```bash
./gradlew assembleDebug
```

## データの出典

- 天気・波・潮位: [Open-Meteo](https://open-meteo.com/)(CC BY 4.0)
- 港・漁港の位置: [国土数値情報](https://nlftp.mlit.go.jp/ksj/)(港湾データ・漁港データ、国土交通省)を加工して作成
