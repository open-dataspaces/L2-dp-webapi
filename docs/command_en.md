# Explanation of Each Command
### Route Registration & Confirmation
Below are three sample patterns.

```
# When the requested path and the destination path are different (specify path conversion with "RewritePath")
# Registration
curl -X POST\
    -H "Content-Type: application/json"\
    -H "X-API-KEY: <Management API-KEY>"\
    -d '{
    "id": "<Route Number>",
    "uri": "<Destination Domain>",
    "predicates": [{
        "name": "Path",
        "args": {
        "_genkey_0": "<Path>**" ← Request path accepted by Gateway
        }
    }],
    "filters": [
     {
      "name": "RewritePath",
      "args": {
        "_genkey_0": "/test(?<segment>.*)", ← Request path before rewrite
        "_genkey_1": "/demo/test/${segment}" ← Destination path after rewrite
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
    <WebAPI Transfer Module Domain>/actuator/gateway/routes/<Route Number>

# Confirmation
curl -s -H "X-API-KEY: <Management API-KEY>"\
    <WebAPI Transfer Module Domain>/actuator/gateway/routes
```

- **Management API-KEY**: API-KEY for the management API (e.g., your-secret-management-api-key)
- **Route Number**: The index number for the registered route (e.g., route01)
- **Destination Domain**: The domain to forward to (e.g., http://prism:4010)
- **predicates**: Criteria for route branching decisions
    - **Path**: Request path. The received request path is forwarded to the destination (e.g., /test)
- **filters**: The filter applied when entering that route
    - **RewritePath**: Subdomain settings. Allows path rewriting
    - **AddRequestHeader**: Adds the specified header
    - **RemoveRequestHeader**: Removes the specified header
- **WebAPI Transfer Module Domain**: The domain of the WebAPI transfer module (http://localhost:8090)
<br>

```
# When the requested path and the destination path are the same (no path conversion)
# Registration
curl -X POST\
    -H "Content-Type: application/json"\
    -H "X-API-KEY: <Management API-KEY>"\
    -d '{
    "id": "<Route Number>",
    "uri": "<Destination Domain>",
    "predicates": [{
        "name": "Path",
        "args": {
        "_genkey_0": "<Path>**"
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
    <WebAPI Transfer Module Domain>/actuator/gateway/routes/<Route Number>

# Confirmation
curl -s -H "X-API-KEY: <Management API-KEY>"\
    <WebAPI Transfer Module Domain>/actuator/gateway/routes
```

- **Management API-KEY**: API-KEY for the management API (e.g., your-secret-management-api-key)
- **Route Number**: The index number for the registered route (e.g., route01)
- **Destination Domain**: The domain to forward to (e.g., http://prism:4010)
- **predicates**: Criteria for route branching decisions
    - **Path**: Request path. The received request path is forwarded to the destination (e.g., /test)
- **filters**: The filter applied when entering that route
    - **AddRequestHeader**: Adds the specified header
    - **RemoveRequestHeader**: Removes the specified header
- **WebAPI Transfer Module Domain**: The domain of the WebAPI transfer module (http://localhost:8090)
<br>

```
# When authorization by OpenFGA is required (add metadata block)
# Registration
curl -X POST\
    -H "Content-Type: application/json"\
    -H "X-API-KEY: <Management API-KEY>"\
    -d '{
    "id": "<Route Number>",
    "uri": "<Destination Domain>",
    "predicates": [{
        "name": "Path",
        "args": {
        "_genkey_0": "<Path>**"
      },
      { 
        "name": "Method",
        "args": { 
        "_genkey_0": "<HTTP Method>"
        }
    }
    }],
    "metadata": {
      "endpointId": "<Endpoint ID>"
     }
    }'\
    <WebAPI Transfer Module Domain>/actuator/gateway/routes/<Route Number>

# Confirmation
curl -s -H "X-API-KEY: <Management API-KEY>"\
    <WebAPI Transfer Module Domain>/actuator/gateway/routes
```

- **Management API-KEY**: API-KEY for the management API (e.g., your-secret-management-api-key)
- **Route Number**: The index number for the registered route (e.g., route01)
- **Destination Domain**: The domain to forward to (e.g., http://prism:4010)
- **predicates**: Criteria for route branching decisions
    - **Path**: Request path. The received request path is forwarded to the destination (e.g., /test)
    - **HTTP Method**: HTTP method name (e.g., POST, GET, PUT, DELETE)
- **metadata**: Information required for authorization
    - **Endpoint ID**: Identifier for the API endpoint subject to authorization.<br>
      Used as a marker to indicate "which operation of which API (e.g., GET /api)" the request is for.<br>
      OpenFGA uses this marker to determine whether the request should be allowed.
- **WebAPI Transfer Module Domain**: The domain of the WebAPI transfer module(http://localhost:8090)

<br>

### Route Deletion

```
# Deletion
curl -X DELETE \
  -H "X-API-KEY: <Management API-KEY>" \
  <WebAPI Transfer Module Domain>/actuator/gateway/routes/<Route Number>

# Confirmation
curl -s -H "X-API-KEY: <Management API-KEY>"\
    <WebAPI Transfer Module Domain>/actuator/gateway/routes
```

- **Management API-KEY**: API-KEY for the management API (e.g., your-secret-management-api-key)
- **WebAPI Transfer Module Domain**: The domain of the WebAPI transfer module (http://localhost:8090)
- **Route Number**: The index number for the registered route (e.g., route01)

<br>

### Token Acquisition (clientID: test_client)
```
curl -X POST <keycloak domain>/realms/<keycloak realm>/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "accept: application/json" \
  -H "Accept-Language: ja-JP" \
  -d "grant_type=client_credentials" \
  -d "client_id=<client_id>" \
  -d "client_secret=<copied client secret>" 
```
- **keycloak domain**: The domain for keycloak. Set the same value as KEYCLOAK_URL specified in the WebAPI transfer module environment variable (e.g., http://keycloak:8010)
- **keycloak realm**: The realm for keycloak (e.g., master)
- **client_id**: The client id for keycloak (e.g., test_client)
- **copied client secret**: The secret for the client specified by client id. Can be checked in the credentials tab of the client.

<br>

### Request to WebAPI Transfer Module
```
curl -v -X POST <WebAPI Transfer Module Domain><Path> \
  -H "Content-Type: application/json" \
  -H "Authorization: bearer <acquired access token>" \
  -H "api-key: <API-KEY>" \
  -H <optional header> \
  -d '{<optional body>}'
```
- **WebAPI Transfer Module Domain**: The domain of the WebAPI transfer module (http://localhost:8090)
- **Path**: Request path. The received request path is forwarded to the destination (e.g., /test)
- **Acquired access token**: The access token obtained from the token acquisition command
- **API-KEY**: The value of VALID_API_KEY specified in the environment variable when starting the WebAPI transfer module (If there are multiple, use any one)
- **Optional header**: You can add headers required by the destination
- **Optional body**: The body required by the destination (e.g., "userid":112233)