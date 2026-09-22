# Google Play リリース手順

## 1. アップロード鍵を作る(初回のみ。あなた自身で実行)
```bash
keytool -genkeypair -v -keystore ~/tsuriport-upload.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```
- パスワードは自分で決めて、パスワード管理ツールなどに控える
- `~/tsuriport-upload.jks` は **バックアップを取り、リポジトリには入れない**
- Play App Signing を使うので、この鍵は「アップロード鍵」。万一なくしても、Play サポートに申請してリセットできる

## 2. 署名情報を置く
プロジェクト直下に `keystore.properties` を作る (`.gitignore` 済み)。
```properties
storeFile=/home/takahiro/tsuriport-upload.jks
storePassword=<キーストアのパスワード>
keyAlias=upload
keyPassword=<鍵のパスワード>
```

## 3. 署名済みの AAB を作る
```bash
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```
公開のたびに `app/build.gradle.kts` の `versionCode` を1つ上げる。

## 4. Play Console
1. デベロッパーアカウントを登録する(登録料は一度だけ。公式の案内で金額と本人確認を確認)
2. アプリを作成 → ストア掲載情報(`store/listing_ja.md`)と「アプリのコンテンツ」を入力
3. プライバシーポリシーのURLを入力(`store/privacy-policy.md` を公開したもの。GitHub Pages なら `https://takahiro13.github.io/<リポジトリ名>/` の形になる)
4. **個人アカウントの場合**: クローズドテストで、12人以上が14日間連続で参加していることが、製品版公開の条件
5. AAB をアップロードして審査に提出

## 5. 公開前の確認
- `applicationId` は `io.github.takahiro13.teiboubiyori`(確定済み。公開後は変更不可)
- 出典表示(アプリ内「出典」)が入っている
- 広告SDK・課金がない(国土数値情報と Open-Meteo の無料枠は非商用限定のため)
