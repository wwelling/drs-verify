# Info

Actuator info endpoint.

**URL** : `/actuator/info`

**Method** : `GET`

## Success Response

**Code** : `200 OK`

**Content example**

```json
{
    "build": {
        "artifact": "verify",
        "name": "edu.harvard.drs:verify",
        "description": "DRS Ingest Verification Service",
        "version": "0.0.1-SNAPSHOT"
    }
}
```

# Health

Actuator health endpoint.

**URL** : `/actuator/health`

**Method** : `GET`

## Success Response

**Code** : `200 OK`

**Content example**

```json
{
    "status": "UP",
    "components": {
        "diskSpace": {
            "status": "UP",
            "details": {
                "total": 269490393088,
                "free": 229776134144,
                "threshold": 10485760,
                "exists": true
            }
        },
        "ping": {
            "status": "UP"
        }
    }
}
```


# Logfile

Actuator logfile endpoint.

**URL** : `/actuator/logfile`

**Method** : `GET`

## Success Response

**Code** : `200 OK`

**Content example**

```text
2021-10-05 10:32:09.314  INFO 33360 --- [main] e.h.drs.verify.VerifyApplicationTests    : Starting VerifyApplicationTests using Java 11.0.11 on 254338SD-OP7760 with PID 33360 (started by william_welling in C:\Users\william_welling\Development\harvard\drs-verify)
2021-10-05 10:32:09.327 DEBUG 33360 --- [main] e.h.drs.verify.VerifyApplicationTests    : Running with Spring Boot v2.5.5, Spring v5.3.10
2021-10-05 10:32:09.328  INFO 33360 --- [main] e.h.drs.verify.VerifyApplicationTests    : The following profiles are active: development
2021-10-05 10:32:09.433 DEBUG 33360 --- [main] o.s.w.c.s.GenericWebApplicationContext   : Refreshing org.springframework.web.context.support.GenericWebApplicationContext@cbd9494
2021-10-05 10:32:12.405  INFO 33360 --- [main] e.h.drs.verify.VerifyApplicationTests    : Started VerifyApplicationTests in 3.956 seconds (JVM running for 5.739)
```