# 各コマンドについて
### ルート登録・確認
以下、サンプルとして3パターン記載している。

```
# リクエストされたpathと転送先のpathが変わる場合(pathの変換　【RewritePath】で指定)
# 登録
curl -X POST\
    -H "Content-Type: application/json"\
    -H "X-API-Key: <マネージメントAPI-Key>"\
    -d '{
        "id": "<ルート番号>",
        "uri": "<転送先ドメイン>",
        "predicates": [{
            "name": "Path",
            "args": {
                "_genkey_0": "<パス>**"
            }
        }],
        "filters": [{
            "name": "RewritePath",
            "args": {
                "_genkey_0": "/test(?<segment>.*)",
                "_genkey_1": "/demo/test/${segment}"
            }
        },
        {
            "name": "AddRequestHeader",
            "args": {
                "name": "X-API-Key",
                "value": "sent-api-key-123"
            }
        },
        {
            "name": "RemoveRequestHeader",
            "args": {
                "name": "API-Key"
            }
        }]
    }'\
    <Web API転送モジュールドメイン>/actuator/gateway/routes/<ルート番号>

# 確認
curl -s -H "X-API-Key: <マネージメントAPI-Key>"\
    <Web API転送モジュールドメイン>/actuator/gateway/routes
```

- **マネージメントAPI-Key**：管理用APIのAPI-Key（例：your-secret-management-api-key）
- **ルート番号**：登録ルートのインデックスとなる番号（例：route01）
- **転送先ドメイン**：転送先のドメイン（例：http://prism:4010）
- **predicates**：ルート分岐の判断材料
    - **Path**：リクエストパスによるルートの判定
        - **args**：判定条件
            - **_genkey_0**：Gateway が受け付けるリクエストパス（`<パス>**`）
- **filters**：そのルートに入ったときにかかるフィルター
    - **RewritePath**：リクエストパスの書き換え
        - **args**：書き換え条件
            - **_genkey_0**：書き換え前のリクエストパスを表す正規表現（`/test(?<segment>.*)`）
            - **_genkey_1**：書き換え後の送信先パス（`/demo/test/${segment}`）
    - **AddRequestHeader**：指定したHeader追加
    - **RemoveRequestHeader**：指定したHeaderの削除
- **WebAPI転送モジュールドメイン**：WebAPI転送モジュールのドメイン（http://localhost:8090）
<br>

```
# リクエストされたpathと転送先のpathが同一の場合(pathの変換なし)
# 登録
curl -X POST\
    -H "Content-Type: application/json"\
    -H "X-API-Key: <マネージメントAPI-Key>"\
    -d '{
        "id": "<ルート番号>",
        "uri": "<転送先ドメイン>",
        "predicates": [{
            "name": "Path",
            "args": {
                "_genkey_0": "<パス>**"
            }
        }],
        "filters": [{
            "name": "AddRequestHeader",
            "args": {
                "name": "X-API-Key",
                "value": "sent-api-key-123"
            }
        },
        {
            "name": "RemoveRequestHeader",
            "args": {
                "name": "API-Key"
            }
        }]
    }'\
    <Web API転送モジュールドメイン>/actuator/gateway/routes/<ルート番号>

# 確認
curl -s -H "X-API-Key: <マネージメントAPI-Key>"\
    <Web API転送モジュールドメイン>/actuator/gateway/routes
```

- **マネージメントAPI-Key**：管理用APIのAPI-Key（例：your-secret-management-api-key）
- **ルート番号**：登録ルートのインデックスとなる番号（例：route01）
- **転送先ドメイン**：転送先のドメイン（例：http://prism:4010）
- **predicates**：ルート分岐の判断材料
    - **Path**：リクエストパスによるルートの判定
        - **args**：判定条件
            - **_genkey_0**：Gateway が受け付けるリクエストパス（`<パス>**`）
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
    -H "X-API-Key: <マネージメントAPI-Key>"\
    -d '{
        "id": "<ルート番号>",
        "uri": "<転送先ドメイン>",
        "predicates": [{
            "name": "Path",
            "args": {
                "_genkey_0": "<パス>**"
            }
        },
        {
            "name": "Method",
            "args": { 
              "_genkey_0": "<HTTPメソッド>"
            }
        }],
        "metadata": {
            "endpointId": "<エンドポイントID>"
        }
    }'\
    <Web API転送モジュールドメイン>/actuator/gateway/routes/<ルート番号>

# 確認
curl -s -H "X-API-Key: <マネージメントAPI-Key>"\
    <Web API転送モジュールドメイン>/actuator/gateway/routes
```

- **マネージメントAPI-Key**：管理用APIのAPI-Key（例：your-secret-management-api-key）
- **ルート番号**：登録ルートのインデックスとなる番号（例：route01）
- **転送先ドメイン**：転送先のドメイン（例：http://prism:4010）
- **predicates**：ルート分岐の判断材料
    - **Path**：リクエストパスによるルートの判定
        - **args**：判定条件
            - **_genkey_0**：Gateway が受け付けるリクエストパス（`<パス>**`）
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
  -H "X-API-Key: <マネージメントAPI-Key>" \
  <Web API転送モジュールドメイン>/actuator/gateway/routes/<ルート番号>

# 確認
curl -s -H "X-API-Key: <マネージメントAPI-Key>"\
    <Web API転送モジュールドメイン>/actuator/gateway/routes
```

- **マネージメントAPI-Key**：管理用APIのAPI-Key（例：your-secret-management-api-key）
- **WebAPI転送モジュールドメイン**：WebAPI転送モジュールのドメイン（http://localhost:8090）
- **ルート番号**：登録ルートのインデックスとなる番号（例：route01）

<br>

### トークンの取得(clientID:test_client)
```
curl -X POST <keycloakドメイン>/realms/<keycloakレルム>/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Accept: application/json" \
  -H "Accept-Language: ja-JP" \
  -d "grant_type=client_credentials" \
  -d "client_id=<client_id>" \
  -d "client_secret=<コピーしたclient secret>" 
```
- **keycloakドメイン**：keycloakのドメイン。WebAPI転送モジュール環境変数で指定したKEYCLOAK_URLと同じものを設定する（例：http://keycloak:8081）
- **keycloakレルム**：keycloakのレルム（例：master）
- **client_id**：keycloakのclient id（例：test_client）
- **コピーしたclient secret**：client idで指定したクライアントのシークレット。クライアントのクレデンシャルタブから確認可。

<br>

### Web API転送モジュールへリクエスト
```
curl -v -X POST <Web API転送モジュールドメイン><パス> \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <取得したアクセストークン>" \
  -H "API-Key: <API-Key>" \
  -H <任意のヘッダー> \
  -d '{<任意のボディ>}'
```
- **WebAPI転送モジュールドメイン**：WebAPI転送モジュールのドメイン（http://localhost:8090）
- **パス**：リクエストパス。受信したリクエストパスを転送先に引き継ぐ（例：/test）
- **取得したアクセストークン**：トークン取得コマンドで取得したアクセストークン
- **API-Key**：WebAPI転送モジュール起動時の環境変数で指定したVALID_API_KEYの値（複数ある場合はどれか1つ）
- **任意のヘッダー**：転送先で必要となるヘッダーを追加可能
- **任意のボディ**：転送先で必要となるボディ(例："userid":112233)