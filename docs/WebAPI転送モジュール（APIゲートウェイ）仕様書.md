# Web API転送モジュール（APIゲートウェイ方式）仕様
このドキュメントは、APIゲートウェイ方式のWeb API転送モジュールの仕様を整理したものである。


## はじめに
本仕様書は、ウラノス・エコシステムにおける技術参照⽂書「ウラノス・エコシステム・データスペーシズリファレンスアーキテクチャモデル（ODS-RAM）」の参照実装のうち、トランザクションレイヤ（L2）に規定されたODS Flex Dataspace Connector（ODS-FDC）のデータプレーンモジュールの⼀つであるWeb API転送モジュールの仕様となる。  
ODS-RAM、ODS-FDCの詳細については[こちら](http://open-dataspaces.gitbook.io/ods-docs/jp)を参照すること。  

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


