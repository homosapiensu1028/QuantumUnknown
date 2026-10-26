QUANTUM UNKNOWN 0.2.0 — Prism Launcher用パック

使い方（Windows）
1. Prism Launcherを開く
2. 「インスタンスを追加」→「インポート」を開く
3. このZIPを選んでインポートする
4. インスタンスを起動する

初回起動について
- この環境では完成済みMODのJARを事前ビルドできなかったため、初回起動前に自動ビルドします。
- 初回のみインターネット接続が必要です。JDK 21、Gradle、Minecraft/NeoForgeの開発依存関係をダウンロードします。
- QuantumCoreのビルドテストが通った後、modsフォルダへインストールし、JEIもダウンロードします。
- 初回ビルドは通常起動よりかなり時間がかかることがあります。
- Windows向けの初回ビルド自動化です。macOS/LinuxではこのPreLaunchCommandは動作しません。

安全確認
- PreLaunchCommandはこのパックに含まれるPowerShellビルドスクリプトを実行します。
- 実行内容は .minecraft/quantumcore-prism-bootstrap.ps1 と .minecraft/quantumcore-source/build.ps1 で確認できます。
- もし起動に失敗したら、Prismのコンソールログと .minecraft/quantumcore-source/build/ を確認してください。

このパックはMinecraft Java Edition 1.21.1 / NeoForge 21.1.262用です。
