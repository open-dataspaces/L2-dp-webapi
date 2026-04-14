# Web API Transfer Module (API Gateway Method) Specification
This document organizes the specifications for the Web API Transfer Module using the API Gateway method.


## Introduction
This specification describes the Web API Transfer Module, one of the data plane modules of the ODS Flex Dataspace Connector (ODS-FDC) defined in the transaction layer (L2) of the technical reference document "Ouranos Ecosystem Dataspaces Reference Architecture Model (ODS-RAM)" for the Ouranos Ecosystem.
For details on ODS-RAM and ODS-FDC, refer to [here](http://open-dataspaces.gitbook.io/ods-docs).

## Prerequisites

The Web API Transfer Module adopts a transparent API gateway method and is limited to routing functionality only. Therefore, it does not provide data analysis or model conversion features supported in previous reference implementations. The module mediates communication between industry services (client applications) and external systems, providing token validation and PEP (Policy Enforcement Point) functions in cooperation with the identity component. The PEP function offers authorization control at the API level.

Due to the transparent API gateway method, there are no restrictions caused by API version or specification changes on the external system side. On the other hand, since the module acts as the interface layer with client applications, certain header information must be added during communication. Refer to the header specification for required headers.

※"External system" in this document refers to systems that communicate via the Web API Transfer Module when requested by client applications (systems involved in transactions other than the client application and Web API Transfer Module).

## Header Specification
### Request Header Items
When a client application requests an API via the Web API Transfer Module, the following four header items must be included. If the X-TrackingId is not present in the request header, the module automatically generates a UUID for subsequent tracking management, making X-TrackingId optional.
Additionally, if a header with the prefix "X-ODS-" is included in the request, the module records its content in its own log. For example, in the settlement/payment service (a Data Space Complementary Service, DCS), items required for integrity verification processing using logs may be added. This header item is optional, depending on the service or use case.

| Header Name | Description | Required/Optional | 
|--------------:|--------------------------------------------------|-------------------| 
| API-Key | API key issued by this service | Required | 
| Authorization | Access token issued by the identity component (JWT format) | Required | 
| X-TrackingId | Log output item for provenance management (UUID format) | Optional | 
| X-ODS-xxx | Logging target item. Any string can be used for xxx (e.g., X-ODS-UserId) | Optional |

## Response Specification
### Responses from External Systems

The Web API Transfer Module uses a transparent API gateway method. Therefore, normal responses (HTTP 200 series) and error responses (HTTP 400 and 500 series, including external service-specific errors) returned from external systems are generally passed through unchanged. For detailed specifications of these responses, refer to the documentation for each external system.

### List of Responses
Below is a list of **responses returned by the Web API Transfer Module itself**.

| HTTP Status Code        | Message                                       | Description                                                  |
|-------------------------|-----------------------------------------------|--------------------------------------------------------------|
| Normal                  | Complies with external system API specs        | Complies with external system API specs                      |
| 400 BadRequest          | Invalid request parameters                      | In case of an invalid request (Missing headers for required fields, etc.) |
| 401 Unauthorized        | Invalid or expired token                        | If the JWT Bearer token is invalid or expired                |
| 401 Unauthorized        | Failed to fetch user info                       | When OIDC UserInfo retrieval fails                           |
| 403 Forbidden           | Access denied                                   | If the request is denied authorization                       |
| 404 NotFound            | Endpoint not found                              | If the requested resource (such as an endpoint or ID) does not exist |
| 500 InternalServerError | Unexpected error occurred                       | Unexpected error within the Web API transfer module          |
| 502 BadGateway          | Received an invalid response from the outer service | When the response from the external system is invalid     |
| 503 ServiceUnavailable  | Failed to connect to the outer service         | When unable to communicate with external systems             |
| 504 GatewayTimeout      | Too long to receive a response from outer service | If the response from the external system times out        |


### Common Error Response Format
The following is the common format for error messages occurring in the Web API transfer module. All error responses are returned in JSON format.

```json
{
  "code": "[prefix] {Error Status}",
  "message": "{Error Message}",
  "detail": "{Timestamp}"
}
```

- code: Error code including prefix (e.g., "[dataspace] NotFound")
- message: Specific error message
- detail: Timestamp (ISO 8601 UTC format)

The following prefixes are used in the current specification.

- **dataspace**:When an error occurs in the Web API Transfer Module
- **auth**:When an authentication/authorization-related error occurs


### Error Message Details (JSON)
Below are the JSON responses returned for each error.

- **400 BadRequest**
Returned when the request is invalid (e.g., missing required headers).

```json
    {
      "code": "[dataspace] BadRequest",
      "message": "Invalid request parameters",
      "detail": "timeStamp: 2025-09-26T14:30:00Z"
    }
```

- **401 Unauthorized**
  - **Pattern 1: JWT Authentication Error**
Returned when the Authorization header's Bearer token (JWT) is invalid or expired.

```json
    {
      "code": "[auth] Unauthorized",
      "message": "Invalid or expired token",
      "detail": "timeStamp: 2025-09-26T14:30:00Z"
    }
```

  - **Pattern 2: OIDC UserInfo Retrieval Error**
  Returned when the OIDC UserInfo endpoint call fails.

```json
    {
      "code": "[auth] Unauthorized",
      "message": "Failed to fetch user info",
      "detail": "timeStamp: 2025-09-26T14:30:00Z"
    }
```

- **403 Forbidden**
Returned when the request is denied authorization.

```json
    {
      "code": "[auth] Forbidden",
      "message": "Access denied",
      "detail": "timeStamp: 2025-09-25T14:30:00Z"
    }
```

- **404 NotFound**
Returned when the requested resource (endpoint or ID) does not exist.

```json
  {
    "code": "[dataspace] NotFound",
    "message": "Endpoint not found",
    "detail": "timeStamp: 2025-09-25T14:30:00Z"
  }
```

- **500 InternalServerError**
Returned when an unexpected error occurs during processing inside the module.

```json
  {
    "code": "[dataspace] InternalServerError",
    "message": "Unexpected error occurred",
    "detail": "timeStamp: 2025-09-25T14:30:00Z"
  }
```

- **502 BadGateway**
Returned when the response received from the external system is invalid.

```json
  {
    "code": "[dataspace] BadGateway",
    "message": "Received an invalid response from the outer service",
    "detail": "timeStamp: 2025-09-25T14:30:00Z"
  }
```

- **503 ServiceUnavailable**
Returned when unable to communicate with the external system.

```json
  {
    "code": "[dataspace] ServiceUnavailable",
    "message": "Failed to connect to the outer service",
    "detail": "timeStamp: 2025-09-25T14:30:00Z"
  }
```

- **504 GatewayTimeout**
Returned when the response from the external system times out.

```json
  {
    "code": "[dataspace] GatewayTimeout",
    "message": "Too long to receive a response from outer service",
    "detail": "timeStamp: 2025-09-25T14:30:00Z"
  }
```
