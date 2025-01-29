# PagoPA Wallet Event Dispatcher Service

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pagopa_pagopa-wallet-event-dispatcher-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=pagopa_pagopa-wallet-event-dispatcher-service)

This microservice is responsible for processing asynchronous workflows involving wallets inside the PagoPA platform.
For more information about wallets, see [pagopa-wallet-service](https://github.com/pagopa/pagopa-wallet-service).

- [PagoPA Wallet Event Dispatcher Service](#pagopa-wallet-event-dispatcher-service)
    * [Technology Stack](#technology-stack)
    * [Start Project Locally 🚀](#start-project-locally-)
        + [Prerequisites](#prerequisites)
        + [Run docker container](#run-docker-container)
    * [Develop Locally 💻](#develop-locally-)
        + [Prerequisites](#prerequisites-1)
        + [Run the project](#run-the-project)
        + [Testing 🧪](#testing-)
            - [Unit testing](#unit-testing)
            - [Integration testing](#integration-testing)
            - [Performance testing](#performance-testing)
    * [Dependency management 🔧](#dependency-management-)
        + [Dependency lock](#dependency-lock)
        + [Dependency verification](#dependency-verification)
    * [Contributors 👥](#contributors-)
        + [Maintainers](#maintainers)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with
markdown-toc</a></i></small>

## Technology Stack

- Kotlin
- Spring Boot

---

## Start Project Locally 🚀

### Prerequisites

- docker

### Populate the environment

The microservice needs a valid `.env` file in order to be run.

If you want to start the application without too much hassle, you can just copy `.env.example` with

```shell
$ cp .env.example .env
```

to get a good default configuration.

If you want to customize the application environment, reference this table:

| Variable name                                        | Description                                                                                | type    | default                 |
|------------------------------------------------------|--------------------------------------------------------------------------------------------|---------|-------------------------|
| DEFAULT_LOGGING_LEVEL                                | Application root logging level                                                             | string  | INFO                    |
| APP_LOGGING_LEVEL                                    | Application specific logging level                                                         | string  | INFO                    |
| WALLET_STORAGE_QUEUE_KEY                             | Azure wallet storage queue account key                                                     | string  |                         |
| WALLET_STORAGE_QUEUE_ACCOUNT_NAME                    | Azure wallet storage queue account name                                                    | string  |                         |
| WALLET_STORAGE_QUEUE_ENDPOINT                        | Azure wallet storage queue endpoint                                                        | string  |                         |
| WALLET_USAGE_QUEUE_NAME                              | Storage queue name for event sent to notify wallet usage                                   | string  | wallet-usage-queue      |
| WALLET_EXPIRATION_QUEUE_NAME                         | Storage queue name for event sent to notify wallet onboarding process expiration           | string  | wallet-expiration-queue |
| WALLET_EXPIRATION_QUEUE_POLLING_MAX_MESSAGE_PER_POLL | Max message to be retrieved from `WALLET_EXPIRATION_QUEUE_NAME` for each poll iteration    | number  | 10                      |
| WALLET_EXPIRATION_QUEUE_POLLING_FIXED_DELAY_MS       | Polling fixed delay in millis used when retrieve event from `WALLET_EXPIRATION_QUEUE_NAME` | number  | 1000                    |
| WALLET_SERVICE_URI                                   | Wallet service endpoint                                                                    | string  |                         |
| WALLET_SERVICE_READ_TIMEOUT                          | Wallet service read timeout                                                                | number  | 10000                   |
| WALLET_SERVICE_CONNECTION_TIMEOUT                    | Wallet service connection timeout                                                          | number  | 10000                   |
| AZURE_EVENTHUB_CONNECTION_STRING                     | Azure event hub connection string                                                          | string  |                         |
| AZURE_EVENTHUB_TOPIC_NAME                            | Azure event hub topic name                                                                 | string  |                         |
| AZURE_EVENTHUB_BOOTSTRAP_SERVER                      | Azure event hub bootstrap server                                                           | string  |                         |
| CDC_SEND_RETRY_MAX_ATTEMPTS                          | Max configurable attempts for performing the logic business related to a change event      | number  |                         |                               
| CDC_SEND_RETRY_INTERVAL_IN_MS                        | Configurable interval in milliseconds between retries attempts                             | number  |                         |
| WALLET_CDC_QUEUE_NAME                                | Storage queue name for event sent by cdc hub                                               | string  | wallet-cdc-queue        |
| REDIS_STREAM_EVENT_CONTROLLER_STREAM_KEY             | Event (receivers) controller redis stream key                                              | string  |                         |
| REDIS_STREAM_EVENT_CONTROLLER_CONSUMER_NAME_PREFIX   | Event (receivers) controller redis stream consumer name prefix                             | string  |                         |
| EVENT_CONTROLLER_STATUS_POLLING_CHRON                | Chron used to schedule event receivers status polling                                      | string  |                         |
| DEPLOYMENT_VERSION                                   | Env property used to identify deployment version (STAGING/PROD)                            | string  | PROD                    |
| REDIS_HOST                                           | Redis hostname                                                                             | string  |                         |
| REDIS_PASSWORD                                       | Redis port                                                                                 | string  |                         |
| REDIS_PORT                                           | Redis password                                                                             | number  |                         |
| REDIS_SSL_ENABLED                                    | Boolean value indicating if SSL is enabled for connection to redis                         | boolean |                         |

### Run docker container

```shell
$ docker compose up --build
```

---

## Develop Locally 💻

### Prerequisites

- git
- gradle
- jdk-17

### Run the project

```shell
$ export $(grep -v '^#' .env.local | xargs)
$ ./gradlew bootRun
```

### Testing 🧪

#### Unit testing

To run the **Junit** tests:

```shell
$ ./gradlew test
```

#### Integration testing

TODO

#### Performance testing

> [!IMPORTANT]  
> This section is not updated

install [k6](https://k6.io/) and then from `./performance-test/src`

1. `k6 run --env VARS=local.environment.json --env TEST_TYPE=./test-types/load.json main_scenario.js`

### Dependency management 🔧

For support reproducible build this project has the following gradle feature enabled:

- [dependency lock](https://docs.gradle.org/8.1/userguide/dependency_locking.html)
- [dependency verification](https://docs.gradle.org/8.1/userguide/dependency_verification.html)

#### Dependency lock

This feature use the content of `gradle.lockfile` to check the declared dependencies against the locked one.

If a transitive dependencies have been upgraded the build will fail because of the locked version mismatch.

The following command can be used to upgrade dependency lockfile:

```shell
./gradlew dependencies --write-locks --no-build-cache --refresh-dependencies 
```

Running the above command will cause the `gradle.lockfile` to be updated against the current project dependency
configuration

#### Dependency verification

This feature is enabled by adding the gradle `./gradle/verification-metadata.xml` configuration file.

Perform checksum comparison against dependency artifact (jar files, zip, ...) and metadata (pom.xml, gradle module
metadata, ...) used during build
and the ones stored into `verification-metadata.xml` file raising error during build in case of mismatch.

The following command can be used to recalculate dependency checksum:

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies 
```

In the above command the `clean`, `spotlessApply` `build` tasks where chosen to be run
in order to discover all transitive dependencies used during build and also the ones used during
spotless apply task used to format source code.

The above command will upgrade the `verification-metadata.xml` adding all the newly discovered dependencies' checksum.
Those checksum should be checked against a trusted source to check for corrispondence with the library author published
checksum.

`/gradlew --write-verification-metadata sha256` command appends all new dependencies to the verification files but does
not remove
entries for unused dependencies.

This can make this file grow every time a dependency is upgraded.

To detect and remove old dependencies make the following steps:

1. Delete, if present, the `gradle/verification-metadata.dryrun.xml`
2. Run the gradle write-verification-metadata in dry-mode (this will generate a verification-metadata-dryrun.xml file
   leaving untouched the original verification file)
3. Compare the verification-metadata file and the verification-metadata.dryrun one checking for differences and removing
   old unused dependencies

The 1-2 steps can be performed with the following commands

```Shell
rm -f ./gradle/verification-metadata.dryrun.xml 
./gradlew --write-verification-metadata sha256 clean spotlessApply build --dry-run --no-build-cache --refresh-dependencies 
```

The resulting `verification-metadata.xml` modifications must be reviewed carefully checking the generated
dependencies checksum against official websites or other secure sources.

If a dependency is not discovered during the above command execution it will lead to build errors.

You can add those dependencies manually by modifying the `verification-metadata.xml`
file adding the following component:

```xml

<verification-metadata>
    <!-- other configurations... -->
    <components>
        <!-- other components -->
        <component group="GROUP_ID" name="ARTIFACT_ID" version="VERSION">
            <artifact name="artifact-full-name.jar">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
            <artifact name="artifact-pom-file.pom">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
        </component>
    </components>
</verification-metadata>
```

Add those components at the end of the components list and then run the

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies 
```

that will reorder the file with the added dependencies checksum in the expected order.

Finally, you can add new dependencies both to gradle.lockfile writing verification metadata running

```shell
 ./gradlew dependencies --write-locks --write-verification-metadata sha256 --no-build-cache --refresh-dependencies 
```

For more information read the
following [article](https://docs.gradle.org/8.1/userguide/dependency_verification.html#sec:checksum-verification)

## Contributors 👥

Made with ❤️ by PagoPA S.p.A.

### Maintainers

See `CODEOWNERS` file
