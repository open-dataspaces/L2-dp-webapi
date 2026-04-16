## 概要・目的
本リポジトリでは、Open Data Spaces（ODS）における技術参照文書「Open Data Spaces Reference Architecture Model（ODS-RAM）」の参照実装のうち、トランザクションレイヤ（L2）のデータプレーンモジュールの1つであるWeb API転送モジュールについて公開する。  

ODS-RAMの詳細については[こちら](http://open-dataspaces.gitbook.io/ods-docs/jp)を参照すること。

## 基本概念
「Open Data Spaces (ODS)」は、国や組織ごとの多様性を尊重する、オープンでスケーラブルな分散データマネジメントの技術コンセプトである。  

ODSでは産業界がデータスペースの社会実装を早急に進めるためのサービスライフサイクルに焦点をおいたアーキテクチャモデルである「ODS-RAM」を公開しており、その中で「形式」、「要求」、「手段」の問題を解決するレイヤとして「トランザクションレイヤ」が定義されている。Web API転送モジュールは、トランザクションレイヤにおけるデータプレーンモジュールの1つであり、低ペイロードのWeb API転送に特化したデータプレーンモジュールである。

## 機能概要・機能一覧
機能についてはL2のプロトコル仕様をまとめた[Open Data Spaces Protocol（ODP）](http://open-dataspaces.gitbook.io/ods-docs/jp)および[Web API転送モジュール仕様書](docs//WebAPI転送モジュール（APIゲートウェイ）仕様書.md)を参照すること。

## ディレクトリ構成
```
.
├── src/                                     # ソースコード
│   ├── main/
│   │   ├── java/nedo/ods/svc/dp/
│   │   │   ├── OdsSvcDpHttpApplication.java # Spring Bootアプリケーションエントリーポイント
│   │   │   ├── authzen/                     # AuthZEN統合機能
│   │   │   │   ├── api/                     # AuthZEN APIクライアント
│   │   │   │   ├── config/                  # AuthZEN設定
│   │   │   │   ├── context/                 # 認可コンテキスト管理
│   │   │   │   ├── exception/               # 認可例外処理
│   │   │   │   ├── model/                   # AuthZENモデル
│   │   │   │   ├── serialization/           # シリアライズ処理
│   │   │   │   └── transport/               # トランスポート層
│   │   │   ├── gateway/                     # Spring Cloud Gateway設定
│   │   │   │   ├── config/                  # ゲートウェイ設定
│   │   │   │   ├── error/                   # エラーハンドリング
│   │   │   │   ├── filter/                  # リクエスト/レスポンスフィルター
│   │   │   │   └── logger/                  # ロギング機能
│   │   │   ├── persistence/                 # データアクセス層（ルート情報管理）
│   │   │   └── security/                    # 認証・認可機能
│   │   │       ├── config/                  # セキュリティ設定
│   │   │       ├── exception/               # 認証例外処理
│   │   │       ├── oauth2/                  # OAuth2/JWT検証
│   │   │       └── server/                  # セキュリティサーバー設定
│   │   └── resources/
│   │       ├── application.yml              # アプリケーション設定
│   │       ├── logback-spring.xml           # ログ設定
│   │       └── db/migration/                # Flywayデータベースマイグレーション
│   └── test/                                # テストコード
│       ├── java/nedo/ods/svc/dp/            # ユニット・統合テスト
│       └── resources/                       # テスト用設定
├── docs/                                    # ドキュメント
├── pom.xml                                  # Mavenビルド設定
├── Dockerfile                               # Dockerイメージビルド設定
├── docker-compose.yml                       # コンテナオーケストレーション設定
├── openapi.yaml                             # OpenAPI仕様（Prism Mock用）
└── README.md                                # 本ファイル
```

## 環境構築手順
本モジュール（L2 / Web API転送モジュール）を動かすための実行環境を事前に用意する。

### ライブラリ・開発フレームワーク・ビルドツール・各種提供形態のパッケージ
本リポジトリのシステムが動作するための動作環境の一式を事前にインストールする。
- java関係のソフトウェア
  - OpenJDK
  - Apache Maven

- Web API転送モジュールとmockサーバを起動させるために必要となる(コンテナ実行)
  - Docker
  - Docker Compose

### 動作確認済み実行環境
- Windows 11 + WSL(Windows Subsystem for Linux)
  - WSLバージョン : 2.6.3.0
  - Linuxディストリビューション : Ubuntu-24.04

以下、各ソフトウェアのバージョンを示す。  
|Name                                        |Version |
|:-------------------------------------------|:-------|
| java                                     | 21.0.9 |
| apache-maven                             | 3.8.7 |
| docker                                   | 27.5.1 |
| docker-compose                           | 2.38.2 |

### 資材
**WebAPI転送モジュール起動用資材：**<br>
[L2-dp-webapi](https://github.com/open-dataspaces/L2-dp-webapi)<br>

上記に含まれているもの
  - Web API転送モジュール
  - ルート登録用データベース（PostgreSQL）
  - IdP（Keycloak）
  - AuthZ（OpenFGA）
  - Mock（Prism）
  <br>
※Keycloak、Prismはdocker-compose.ymlに含まれている

### 環境変数の設定（docker-compose.yml → application.yml）
起動時に設定が必要となる環境変数を設定する。<br>
環境変数は以下の12個であり、[docker-compose.yml](docker-compose.yml)で設定することで[application.yml](src/main/resources/application.yml)に引き渡される。<br>
変更が必要な場合は[docker-compose.yml](docker-compose.yml)を変更する。

| 環境変数                      | 説明                                                                  | 例 |
| :---------------------------- | :-------------------------------------------------------------------- | :--- |
| KEYCLOAK_URL                  | JWT検証先のKeycloakのURL                                              | http://keycloak:8081 |
| KEYCLOAK_REALM                | JWT検証先のKeycloakのRealm name                                       | master |
| FGA_URL                       | OpenFGAのURL                                                          | http://openFGA:8080 |
| FGA_STORE_ID                  | OpenFGAのStoreID                                                      |  |
| DB_URL                        | ルート登録用データベースURL                                           | jdbc:postgresql://postgres:5432/postgres |
| DB_USERNAME                   | データベース接続用ユーザ                                              | postgres |
| DB_PASSWORD                   | データベース接続用パスワード                                          | password |
| DB_SCHEMA                     | ルート登録用データベーススキーマ                                      | public |
| VALID_API_KEYS                | クライアントから送信され、Web API転送モジュールで検証を行うAPI-KEYの値 | 12345-test-key |
| LOGLEVEL                      | ログレベル                                                            | INFO, DEBUG |
| VALID_API_KEYS_ENABLED        | Web API転送モジュールでAPI-Key検証を実施するかどうかを設定             | 有⇒true　無⇒false |
| AUTHZEN_AUTHORIZATION_ENABLED | L3と連携してPEPとしての機能を有効化するかどうかを設定                 |有⇒true　無⇒false |


## ビルド・起動手順
以下、コマンド実行例はUNIX/LINUXコマンドで記載する。

### 1. Web API転送モジュールの資材の取得
```
$ git clone https://github.com/open-dataspaces/L2-dp-webapi.git
$ git checkout <任意のブランチ>
```

### 2. Web API転送モジュールDocker Image作成
```
$ mvn clean install -DskipTests
$ docker build -t l2-test .
```

### 3. docker networkの作成
```
$ docker network create test-network
```

### 4. docker起動
```
$ docker compose up -d
```

### 5. keycloakホスト名の設定<br>
```
$ echo "127.0.0.1 keycloak" | sudo tee -a /etc/hosts
$ getent hosts keycloak
```
  レスポンスで```127.0.0.1 keycloak```が返ってくることを確認。
  

## 参考実装
環境準備が整ったら、下記の業務フローのチュートリアルを実行できる。  
[参考実装チュートリアル](/docs/tutorial.md)

## ライセンス
- 本リポジトリはMITライセンスで提供されています。
- ソースコードおよび関連ドキュメントの著作権は株式会社NTTデータグループ、株式会社NTTデータに帰属します。  

## 免責事項
- 本リポジトリの内容は予告なく変更・削除する可能性があります。
- 本リポジトリの利用により生じた損失及び損害等について、いかなる責任も負わないものとします。