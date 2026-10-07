# Web API転送モジュール（APIゲートウェイ方式）仕様
このドキュメントは、APIゲートウェイ方式のWeb API転送モジュールの仕様を整理したものである。


## はじめに
本仕様書は、Open Data Spaces（ODS）における技術参照文書「Open Data Spaces Reference Architecture Model（ODS-RAM）」の参照実装のうち、トランザクションレイヤ（L2）のデータプレーンモジュールの1つであるWeb API転送モジュールの仕様となる。 
ODS-RAMの詳細については[こちら](http://open-dataspaces.gitbook.io/ods-docs/jp)を参照すること。  

## 前提事項
Web API転送モジュールは、透過的APIゲートウェイ方式を採用し、ルーティング機能のみに限定している。このため、過去の参照実装で対応していたデータ解析やモデル変換の機能は提供していない。また、Web API転送モジュールは、インダストリサービス（クライアントアプリケーション）と外部システム間の通信に介在し、アイデンティコンポーネントと連携したトークン検証およびPEP（Policy Enforcement Point）機能を提供する。PEP機能ではAPIレベルでの認可制御機能を提供する。   

透過的APIゲートウェイ方式を採用しているため、外部システム側のAPIバージョンやAPI仕様変更による制約は存在しない。一方、Web API転送モジュールはクライアントアプリケーションとのインタフェース層として動作するため、通信時に特定のヘッダー情報の付与が必要となる。指定が必要なヘッダーについては、ヘッダー仕様を参照すること。 

※本ドキュメントにおける「外部システム」とは、クライアントアプリケーションからのリクエスト時にWeb API転送モジュールを介して通信するシステム（クライアントアプリケーションとWeb API転送モジュール以外でトランザクションに関与するシステム）を指す。  

## ヘッダー仕様
### リクエスト時のヘッダー項目について
クライアントアプリケーションがWeb API転送モジュールを経由してAPIをリクエストする際のヘッダー項目は、以下の表に示す4項目である。このうち、X-TrackingIdについては、本項目がリクエストヘッダーに付与されていない場合、Web API転送モジュール内でUUIDを自動採番することで以降のトラッキング管理を可能としている。そのためX-TrackingIdは任意項目としている。  
また、接頭辞に「X-ODS-」ヘッダーを付与してリクエストすると、その内容をWeb API転送モジュールが自身のログに記録する。利用例として、データスペースコンプリメンタリサービス(DCS)の一つである精算・決済サービスにおいて、ログを用いた整合性確認処理に必要な項目を付与するケースが挙げられる。本ヘッダー項目は利用サービスやユースケースに依存するため、任意項目とする。   

| ヘッダー名 | 内容 | 必須/任意 |
|---:|---|---|
| API-Key | 本サービスから払い出されたAPIキー | 必須 |
| Authorization | アイデンティティコンポーネントで発行したアクセストークン (JWT形式)  | 必須 |
| X-TrackingId | 来歴管理⽤ログ出⼒項⽬ (UUID形式) | 任意 |
| X-ODS-xxx | ロギング対象項目。xxxには任意の文字列を記載。（例：X-ODS-UserId） | 任意 |

## レスポンス仕様
### 外部システムからのレスポンスについて
Web API転送モジュールは透過的APIゲートウェイ方式を採用している。このため、外部システムから返却された正常系レスポンス（HTTP 200系）およびエラーレスポンス（HTTP 400系、500系の外部サービス固有のエラー）を原則としてそのまま透過（変更せずに返却）する。これらのレスポンスの詳細な仕様については、該当する外部システムごとの仕様書を参照すること。  

### レスポンス一覧
以下に、**Web API転送モジュール自体が返却する**レスポンス一覧を示す。  

| HTTP ステータスコード | メッセージ | 概要 |
|---|---|---|
| 正常系  | 外部システムのAPI仕様に準拠 | 外部システムのAPI仕様に準拠 |
| 400 BadRequest | Invalid request parameters | リクエスト内容不正の場合（必須項目のヘッダーの不足等） |
| 401 Unauthorized | Invalid or expired token | JWT Bearer トークンが無効または期限切れの場合 |
| 401 Unauthorized | Failed to fetch user info | OIDC UserInfo取得失敗の場合 |
| 403 Forbidden | Access denied | リクエストが認可拒否された場合 |
| 404 NotFound | Endpoint not found | 要求されたリソース（エンドポイントやID等）が存在しない場合 |
| 500 InternalServerError | Unexpected error occurred | Web API転送モジュール内部での想定外エラー |
| 502 BadGateway | Received an invalid response from the outer service | 外部システムからのレスポンスが無効な場合 |
| 503 ServiceUnavailable | Failed to connect to the outer service | 外部システムと通信できない場合 |
| 504 GatewayTimeout | Too long to receive a response from outer service | 外部システムからの応答がタイムアウトした場合 |


### エラーレスポンスの共通フォーマット
以下は Web API転送モジュールで発生するエラーメッセージの共通フォーマットである。すべてのエラー応答は JSON で返却する。

```json
{
  "code": "[prefix] {Error Status}",
  "message": "{Error Message}",
  "detail": "{Timestamp}"
}
```

- code: prefix を含むエラーコード（例: "[dataspace] NotFound"）
- message: 具体的なエラーメッセージ
- detail: タイムスタンプ（ISO 8601 の UTC 形式）

現状の仕様では以下の prefix を使用する。

- **dataspace**: Web API転送モジュールでエラーが発生した場合
- **auth**: 認証認可関連のエラーが発生した場合


### エラーメッセージ詳細（JSON）

以下に各エラーで返される JSON 応答を示す。

- **400 BadRequest**   
リクエスト内容が不正の場合に返す。（必須項目のヘッダーの不足等）
  ```json
    {
      "code": "[dataspace] BadRequest",
      "message": "Invalid request parameters",
      "detail": "timeStamp: 2025-09-26T14:30:00Z"
    }
  ```

- **401 Unauthorized**  
  - **パターン①: JWT認証エラー**  
  Authorization ヘッダーの Bearer トークン（JWT）が無効または期限切れの場合に返す。
    ```json
    {
      "code": "[auth] Unauthorized",
      "message": "Invalid or expired token",
      "detail": "timeStamp: 2025-09-26T14:30:00Z"
    }
    ```

  - **パターン②: OIDC UserInfo取得エラー**  
  OIDC UserInfoエンドポイント呼び出しが失敗した場合に返す。
    ```json
    {
      "code": "[auth] Unauthorized",
      "message": "Failed to fetch user info",
      "detail": "timeStamp: 2025-09-26T14:30:00Z"
    }
    ```

- **403 Forbidden**   
リクエストが認可拒否された場合に返す。
  ```json
    {
      "code": "[auth] Forbidden",
      "message": "Access denied",
      "detail": "timeStamp: 2025-09-25T14:30:00Z"
    }
  ```

- **404 NotFound**  
要求されたリソース（エンドポイントや ID 等）が存在しない場合に返す。
  ```json
  {
    "code": "[dataspace] NotFound",
    "message": "Endpoint not found",
    "detail": "timeStamp: 2025-09-25T14:30:00Z"
  }
  ```

- **500 InternalServerError**  
Web API転送モジュール内部で処理中に想定外のエラーが発生した場合に返す。
  ```json
  {
    "code": "[dataspace] InternalServerError",
    "message": "Unexpected error occurred",
    "detail": "timeStamp: 2025-09-25T14:30:00Z"
  }
  ```

- **502 BadGateway**   
Web API転送モジュールが外部システムから受け取ったレスポンスが無効である場合に返す。
  ```json
  {
    "code": "[dataspace] BadGateway",
    "message": "Received an invalid response from the outer service",
    "detail": "timeStamp: 2025-09-25T14:30:00Z"
  }
  ```

- **503 ServiceUnavailable**  
Web API転送モジュールが外部システムと通信できない場合に返す。
  ```json
  {
    "code": "[dataspace] ServiceUnavailable",
    "message": "Failed to connect to the outer service",
    "detail": "timeStamp: 2025-09-25T14:30:00Z"
  }
  ```

- **504 GatewayTimeout**  
外部システムからの応答がタイムアウトした場合に返す。
  ```json
  {
    "code": "[dataspace] GatewayTimeout",
    "message": "Too long to receive a response from outer service",
    "detail": "timeStamp: 2025-09-25T14:30:00Z"
  }
  ```


## 管理系Actuator API

### 管理系Actuator API 仕様
Web API転送モジュールでは、Spring Boot Actuator と Spring Cloud Gateway Actuator の管理 API を利用する。公開対象は以下の 3 つである。

- Spring Boot Actuator
  - `/actuator/health`
  - `/actuator/info`
- Spring Cloud Gateway Actuator
  - `/actuator/gateway/**`

#### Spring Boot Actuator
##### `/actuator/health`

サービス生存確認や監視基盤からのヘルスチェックに利用する。既定ではアプリケーション全体の稼働状態を返し、`UP`、`DOWN`、`OUT_OF_SERVICE`、`UNKNOWN` などの状態を含む。Spring Boot の health group を設定している場合は、`/actuator/health/liveness` や `/actuator/health/readiness` のような個別グループも利用できる。ヘルスチェック用途のため、ヘッダー検証は行わない。

##### `/actuator/info`

ビルド情報やアプリケーション情報の確認に利用する。`build`、`git`、`java`、`os`、`process`、`ssl` など、Spring Boot が提供する InfoContributor に基づいた情報を返す。`info.*` プロパティを設定している場合は、追加情報も公開できる。呼び出し時は `X-API-Key` ヘッダーに管理用 API キーを指定する。

Spring Boot Actuatorの詳細については以下、公式リファレンスを参照のこと。

公式リファレンス: [Spring Boot Actuator Endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)

#### Spring Cloud Gateway Actuator
##### `/actuator/gateway/**`

Spring Cloud Gateway の管理 API である。`/actuator/gateway` では利用可能な子エンドポイントの一覧を確認できる。主な子エンドポイントは以下の通りである。

- `/actuator/gateway/globalfilters` : グローバルフィルタの一覧を取得する
- `/actuator/gateway/routefilters` : ルートに適用可能な GatewayFilter factory の一覧を取得する
- `/actuator/gateway/routes` : ルート定義一覧を取得する
- `/actuator/gateway/routes/{id}` : 個別ルート定義を取得する
- `/actuator/gateway/routes/{id}` (POST) : ルート定義を作成する
- `/actuator/gateway/routes/{id}` (DELETE) : ルート定義を削除する
- `/actuator/gateway/routes/{id}/combinedfilters` : ルートに適用されるフィルタの組み合わせを取得する
- `/actuator/gateway/refresh` : ルートキャッシュを再読み込みする

これらのエンドポイントを呼び出す際は、`X-API-Key` ヘッダーに管理用 API キーを指定する。

Spring Cloud Gateway Actuatorの詳細については以下、公式リファレンスを参照のこと。

公式リファレンス: [Spring Cloud Gateway Actuator API](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/actuator-api.html)

### API キー認証
管理系 Actuator API では、アプリケーション側で設定した管理用 API キーと、リクエスト側で送信する API キーを一致させる必要がある。

- アプリケーション側: `src/main/resources/application.yml` の `apigateway.management-api-key`
- リクエスト側: 管理系 Actuator API 呼び出し時の `X-API-Key` ヘッダー

#### アプリケーション側の設定
管理用 API キーは `src/main/resources/application.yml` の `apigateway.management-api-key` に設定する。

設定例:

- `apigateway.management-api-key: "your-secret-management-api-key"`

この値は、実運用では環境変数や外部設定で上書きして使用する。`ApiGatewayProperties` の `managementApiKey` にバインドされ、管理系 Actuator API の照合に使われる。

#### リクエスト側の設定
管理系 Actuator API を呼び出す際は、HTTP リクエストヘッダーの `X-API-Key` に、アプリケーション側で設定したキーと同じ値を指定する。

設定例:

- `X-API-Key: your-secret-management-api-key`

たとえば、`/actuator/info` や `/actuator/gateway/**` を呼び出す場合は、このヘッダーを付与する。

`management-base-path` の既定値は `/actuator` である。

### ヘッダー検証との関係
通常の gateway リクエストでは `API-Key` と `Authorization` の検証を行うが、次のパスはヘッダー検証の対象外とする。

- `/actuator/health`
- `/health`
- `/actuator/`

また、`RequestTimingFilter` では `/actuator` で始まるリクエストに対して `requestReceivedTime` の記録を行わない。

### 設定値
主な設定値は以下の通りである。

- `server.port: 8090`
- `management.endpoints.web.exposure.include: gateway,health,info`
- `management.endpoint.gateway.access: unrestricted`
- `security.management-base-path: /actuator`
- `security.skip-validation-paths: /actuator/health, /health, /actuator/`
- `security.valid-API-Keys-enabled` により API キー検証の有効 / 無効を制御する

※ Actuator はアプリケーションと同一ポート（`server.port`）で公開する。
