# 各コマンドについて
### ルート登録・確認
以下、サンプルとして3パターン記載している。

```
# リクエストされたpathと転送先のpathが変わる場合(pathの変換　【RewritePath】で指定)
# 登録
curl -X POST\
    -H "Content-Type: application/json"\
    -H "X-API-KEY: <マネージメントAPI-KEY>"\
    -d '{
    "id": "<ルート番号>",
    "uri": "<転送先ドメイン>",
    "predicates": [{
        "name": "Path",
        "args": {
        "_genkey_0": "<パス>**"　←　Gateway が受け付けるリクエストパス
        }
    }],
    "filters": [
     {
      "name": "RewritePath",
      "args": {
        "_genkey_0": "/test(?<segment>.*)",　←　書き換え前のリクエストパス
        "_genkey_1": "/demo/test/${segment}"　←　書き換え後の送信先パス
      }
    },
    {
        "name": "AddRequestHeader",
        "args": {
            "name": "X-API-KEY",
            "value": "sent-api-key-123"
        }
    },
    {
        "name": "RemoveRequestHeader",
        "args": {
        "name": "api-key"
    }
    }]
    }'\
    <Web API転送モジュールドメイン>/actuator/gateway/routes/<ルート番号>

# 確認
curl -s -H "X-API-KEY: <マネージメントAPI-KEY>"\
    <Web API転送モジュールドメイン>/actuator/gateway/routes
```

- **マネージメントAPI-KEY**：管理用APIのAPI-KEY（例：your-secret-management-api-key）
- **ルート番号**：登録ルートのインデックスとなる番号（例：route01）
- **転送先ドメイン**：転送先のドメイン（例：http://prism:4010）
- **predicates**：ルート分岐の判断材料
	- **パス**：リクエストパス。受信したリクエストパスを転送先に引き継ぐ（例：/test）
- **filters**：そのルートに入ったときにかかるフィルター
    - **RewritePath**：サブドメインの設定。パスを書き換えることができる
    - **AddRequestHeader**：指定したHeader追加
    - **RemoveRequestHeader**：指定したHeaderの削除
- **WebAPI転送モジュールドメイン**：WebAPI転送モジュールのドメイン（http://localhost:8090）
<br>

```
# リクエストされたpathと転送先のpathが同一の場合(pathの変換なし)
# 登録
curl -X POST\
    -H "Content-Type: application/json"\
    -H "X-API-KEY: <マネージメントAPI-KEY>"\
    -d '{
    "id": "<ルート番号>",
    "uri": "<転送先ドメイン>",
    "predicates": [{
        "name": "Path",
        "args": {
        "_genkey_0": "<パス>**"
        }
    }],
    "filters": [
    {
        "name": "AddRequestHeader",
        "args": {
            "name": "X-API-KEY",
            "value": "sent-api-key-123"
        }
    },
    {
        "name": "RemoveRequestHeader",
        "args": {
        "name": "api-key"
    }
    }]
    }'\
    <Web API転送モジュールドメイン>/actuator/gateway/routes/<ルート番号>

# 確認
curl -s -H "X-API-KEY: <マネージメントAPI-KEY>"\
    <Web API転送モジュールドメイン>/actuator/gateway/routes
```

- **マネージメントAPI-KEY**：管理用APIのAPI-KEY（例：your-secret-management-api-key）
- **ルート番号**：登録ルートのインデックスとなる番号（例：route01）
- **転送先ドメイン**：転送先のドメイン（例：http://prism:4010）
- **predicates**：ルート分岐の判断材料
	- **パス**：リクエストパス。受信したリクエストパスを転送先に引き継ぐ（例：/test）
- **filters**：そのルートに入ったときにかかるフィルター
    - **AddRequestHeader**：指定したHeader追加
    - **RemoveRequestHeader**：指定したHeaderの削除
- **WebAPI転送モジュールドメイン**：WebAPI転送モジュールのドメイン（http://localhost:8090）
<br>

```
# OpenFGAによる認可が必要な場合(metadataブロックを追加)
# 登録
curl -X POST\
    -H "Content-Type: application/json"\
    -H "X-API-KEY: <マネージメントAPI-KEY>"\
    -d '{
    "id": "<ルート番号>",
    "uri": "<転送先ドメイン>",
    "predicates": [{
        "name": "Path",
        "args": {
        "_genkey_0": "<パス>**"
      },
      { 
        "name": "Method",
        "args": { 
        "_genkey_0": "<HTTPメソッド>"
        }
    }
    }],
    "metadata": {
      "endpointId": "<エンドポイントID>"
     }
    }'\
    <Web API転送モジュールドメイン>/actuator/gateway/routes/<ルート番号>

# 確認
curl -s -H "X-API-KEY: <マネージメントAPI-KEY>"\
    <Web API転送モジュールドメイン>/actuator/gateway/routes
```

- **マネージメントAPI-KEY**：管理用APIのAPI-KEY（例：your-secret-management-api-key）
- **ルート番号**：登録ルートのインデックスとなる番号（例：route01）
- **転送先ドメイン**：転送先のドメイン（例：http://prism:4010）
- **predicates**：ルート分岐の判断材料
	- **パス**：リクエストパス。受信したリクエストパスを転送先に引き継ぐ（例：/test）
    - **HTTPメソッド**：HTTPメソッド名（例：POST, GET, PUT, DELETE）
- **metadata**：認可に必要な情報
    - **エンドポイントID**：認可対象APIエンドポイントの識別子。<br>
      リクエストが「どのAPIのどの操作（例：GET /api）なのか」を示すための目印。<br> 
	  OpenFGAはこの目印を使って、そのリクエストを通してよいかを判定する。
- **WebAPI転送モジュールドメイン**：WebAPI転送モジュールのドメイン（http://localhost:8090）

<br>

### ルート削除

```
# 削除
curl -X DELETE \
  -H "X-API-KEY: <マネージメントAPI-KEY>" \
  <Web API転送モジュールドメイン>/actuator/gateway/routes/<ルート番号>

# 確認
curl -s -H "X-API-KEY: <マネージメントAPI-KEY>"\
    <Web API転送モジュールドメイン>/actuator/gateway/routes
```

- **マネージメントAPI-KEY**：管理用APIのAPI-KEY（例：your-secret-management-api-key）
- **WebAPI転送モジュールドメイン**：WebAPI転送モジュールのドメイン（http://localhost:8090）
- **ルート番号**：登録ルートのインデックスとなる番号（例：route01）

<br>

### トークンの取得(clientID:test_client)
```
curl -X POST <keycloakドメイン>/realms/<keycloakレルム>/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "accept: application/json" \
  -H "Accept-Language: ja-JP" \
  -d "grant_type=client_credentials" \
  -d "client_id=<client_id>" \
  -d "client_secret=<コピーしたclient secret>" 
```
- **keycloakドメイン**：keycloakのドメイン。WebAPI転送モジュール環境変数で指定したKEYCLOAK_URLと同じものを設定する（例：http://keycloak:8010）
- **keycloakレルム**：keycloakのレルム（例：master）
- **client_id**：keycloakのclient id（例：test_client）
- **コピーしたclient secret**：client idで指定したクライアントのシークレット。クライアントのクレデンシャルタブから確認可。

<br>

### Web API転送モジュールへリクエスト
```
curl -v -X POST <Web API転送モジュールドメイン><パス> \
  -H "Content-Type: application/json" \
  -H "Authorization: bearer <取得したアクセストークン>" \
  -H "api-key: <API-KEY>" \
  -H <任意のヘッダー> \
  -d '{<任意のボディ>}'
```
- **WebAPI転送モジュールドメイン**：WebAPI転送モジュールのドメイン（http://localhost:8090）
- **パス**：リクエストパス。受信したリクエストパスを転送先に引き継ぐ（例：/test）
- **取得したアクセストークン**：トークン取得コマンドで取得したアクセストークン
- **API-KEY**：WebAPI転送モジュール起動時の環境変数で指定したVALID_API_KEYの値（複数ある場合はどれか1つ）
- **任意のヘッダー**：転送先で必要となるヘッダーを追加可能
- **任意のボディ**：転送先で必要となるボディ(例："userid":112233)