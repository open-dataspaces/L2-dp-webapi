## Overview and Purpose
This repository publishes the Web API Transfer Module, one of the data plane modules in the Transaction Layer (L2), as a reference implementation for the Open Data Spaces Reference Architecture Model (ODS-RAM).

For details about ODS-RAM, please click [here](http://open-dataspaces.gitbook.io/ods-docs). 

## Core Concepts
Open Data Spaces (ODS) is an open and scalable foundation for distributed data, built on organizational and national diversity by design.

To accelerate practical deployment of data spaces, the ODS architecture model (ODS-RAM) defines a service lifecycle and a set of layers that address formats, queries, and protocols. The Transaction Layer (L2) solves transaction-level concerns; the Web API Transfer Module is an L2 data-plane module specialized for low-payload Web API Transfer Module.

## Features and References
See the [Open Data Spaces Protocol (ODP) ](http://open-dataspaces.gitbook.io/ods-docs)and the [Web API Transfer Module specification](docs/Web-API-Transfer-Module-API-Gateway-Specification_en.md) for L2 protocol and feature details.

## Repository Layout
```
.
├── src/                                    # Source code
│   ├── main/
│   │   ├── java/nedo/ods/svc/dp/
│   │   │   ├── OdsSvcDpHttpApplication.java # Spring Boot application entry point
│   │   │   ├── authzen/                    # AuthZEN integration
│   │   │   │   ├── api/                    # AuthZEN API client
│   │   │   │   ├── config/                 # AuthZEN config
│   │   │   │   ├── context/                # Authorization context management
│   │   │   │   ├── exception/              # Authorization exceptions
│   │   │   │   ├── model/                  # AuthZEN models
│   │   │   │   ├── serialization/          # Serialization
│   │   │   │   └── transport/              # Transport layer
│   │   │   ├── gateway/                    # Spring Cloud Gateway
│   │   │   │   ├── config/                 # Gateway config
│   │   │   │   ├── error/                  # Error handling
│   │   │   │   ├── filter/                 # Request/response filters
│   │   │   │   └── logger/                 # Logging
│   │   │   ├── persistence/                # Data access (route management)
│   │   │   └── security/                   # Authentication & authorization
│   │   │       ├── config/                 # Security configuration
│   │   │       ├── exception/              # Authentication-related exception handling
│   │   │       ├── oauth2/                 # OAuth2/JWT verification
│   │   │       └── server/                 # Security server configuration
│   │   └── resources/
│   │       ├── application.yml             # Application configuration
│   │       ├── logback-spring.xml          # Logging configuration
│   │       └── db/migration/               # Flyway DB migrations
│   └── test/                               # Tests
│       ├── java/nedo/ods/svc/dp/           # Unit and integration tests
│       └── resources/                      # Test resources
├── docs/                                   # Documentation
├── pom.xml                                 # Maven build configuration
├── Dockerfile                              # Docker image build
├── docker-compose.yml                      # Container orchestration
├── openapi.yaml                            # OpenAPI spec (for Prism mock)
├── README.md                               # README (Japanese)
└── README_en.md                            # This file (English version)
```

## Environment Preparation
Prepare an execution environment to run this module (L2 / Web API Transfer Module).

### Required software and tools
Install the runtime and build tools used by this repository.
- Java-related software
    - OpenJDK
    - Apache Maven

- For running the module and supporting mock services in containers:
    - Docker
    - Docker Compose

### Verified environment
- Windows 11 + WSL (Windows Subsystem for Linux)
  - WSL version: 2.6.3.0
  - Distribution: Ubuntu-24.04

Example verified versions:
| Name              | Version |
|:------------------|:--------|
| Java              | 21.0.9  |
| Apache Maven      | 3.8.7   |
| Docker            | 27.5.1  |
| Docker Compose    | 2.38.2  |

### Materials
**Web API Transfer Module materials are available in the repository:**<br>
[L2-dp-webapi](https://github.com/open-dataspaces/L2-dp-webapi)<br>

Included components:
- Web API Transfer Module
- Route registry database (PostgreSQL)
- Identity Provider (Keycloak)
- Authorization (OpenFGA)
- Mock server (Prism)

Keycloak and Prism are included in the provided `docker-compose.yml`.

### Environment variables (docker-compose.yml → application.yml)
The module requires the following environment variables. Set them in `docker-compose.yml` to pass them to `application.yml`.<br>
If changes are needed, edit the docker-compose.yml file.

| Variable                         | Description                                            | Example |
|:---------------------------------|:-------------------------------------------------------|:--------|
| KEYCLOAK_URL                     | Keycloak URL used for JWT verification                 | http://localhost:8081 |
| KEYCLOAK_REALM                   | Keycloak realm name                                    | master |
| FGA_URL                          | OpenFGA URL                                            | http://openFGA:8080 |
| FGA_STORE_ID                     | OpenFGA store ID                                       |        |
| DB_URL                           | Database URL for route registry                        | jdbc:postgresql://postgres:5432/postgres |
| DB_USERNAME                      | Database user                                         | postgres |
| DB_PASSWORD                      | Database password                                     | password |
| DB_SCHEMA                        | Database schema for routes                            | public |
| VALID_API_KEYS                    | API key value validated by the module                 | 12345-test-key |
| LOGLEVEL                         | Logging level                                         | INFO, DEBUG |
| VALID_API_KEYS_ENABLED           | Enable API key validation (true/false)                | true/false |
| AUTHZEN_AUTHORIZATION_ENABLED    | Enable AuthZEN/PEP integration (true/false)           | true/false |

## Build and Run
Commands below use UNIX/Linux shell syntax.

### 1. Clone repository
```
$ git clone https://github.com/open-dataspaces/L2-dp-webapi.git
$ git checkout <branch>
```

### 2. Build Docker image for the module
```
$ mvn clean install -DskipTests
$ docker build -t l2-test .
```

### 3. Create Docker network
```
$ docker network create test-network
```

### 4. Start containers
```
$ docker compose up -d
```

### 5. Configure Keycloak hostname
```
$ echo "127.0.0.1 keycloak" | sudo tee -a /etc/hosts
$ getent hosts keycloak
```
Confirm the output shows `127.0.0.1 keycloak`.

## Reference tutorial
After preparing the environment, follow the tutorial to run the example workflow:[tutorial](docs/tutorial_en.md)

## License
- This repository is provided under the MIT License.
- Copyright for source code and related documentation belongs to NTT DATA Group and NTT DATA Corporation.

## Disclaimer
- The content of this repository may change or be removed without notice.
- We accept no liability for any loss or damage arising from use of this repository.