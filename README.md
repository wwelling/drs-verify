# drs-verify

DRS Verify service provides API for verifying ingest and update of OCFL objects in S3.

## API

### Verify

* [Verify](verify.md#verify) : `POST /verify/{id}`
* [Verify Update](verify.md#verify-update) : `POST /verify/{id}/update`

### Actuator

* [Info](actuator.md#info) : `GET /actuator/info`
* [Health](actuator.md#health) : `GET /actuator/health`
* [Logfile](actuator.md#logfile) : `GET /actuator/logfile`

## Environment

Here are some of the environment variables that can be set. See [application.yml](https://github.com/wwelling/drs-verify/blob/main/src/main/resources/application.yml) configuration that can be set via environment variables.

| Variable                          | Description                                | Default                             |
| --------------------------        | ------------------------------------------ | ----------------------------------- |
| LOGGING_FILE_PATH                 | path to store logs                         | logs                                |
| LOGGING_LEVEL_EDU_HARVARD_DRS     | drs package log level                      | DEBUG                               |
| LOGGING_LEVEL_ORG_SPRINGFRAMEWORK | spring log level                           | INFO                                |
| LOGGING_LEVEL_WEB                 | we log level                               | INFO                                |
| SERVER_PORT                       | port service listening on                  | 9000                                |
| SPRING_PROFILES_ACTIVE            | active profile                             | development                         |
| AWS_BUCKET_NAME                   | AWS S3 bucket name                         | drs-preservation                    |
| AWS_REGION                        | AWS region                                 | us-east-1                           |
| AWS_ACCESS_KEY_ID                 | AWS access key id                          | foo                                 |
| AWS_SECRET_ACCESS_KEY             | AWS secret access key                      | bar                                 |
| AWS_ENDPOINT_OVERRIDE             | AWS endpoint override                      |                                     |


## Run

Build
```
mvn clean package
```

Run
```
java -jar target\verify-<version>.jar
```

## Development

Start with devtools
```
mvn clean spring-boot:run
```

## Test

Run Tests
```
mvn clean test
```

## Docker

Build
```
docker build --no-cache -t drs-verify .
```

Run
```
docker run -v /logs:/logs --env-file=.env drs-verify
```

## Docker Compose

Build image for local use
```
docker-compose -f docker-compose-local.yml build --no-cache
```

Build image for repository
```
docker-compose -f docker-compose.yml build --no-cache --build-arg TAG=<image tag>
```

Run locally
```
docker-compose -f docker-compose-local.yml up
```

> If using docker, do not forget to copy `.env.example` to `.env` and update accordingly.
