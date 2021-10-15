# build base image
FROM maven:3-openjdk-11-slim as maven

# copy pom.xml
COPY ./pom.xml ./pom.xml

# copy src files
COPY ./src ./src

# build
RUN mvn package -DskipTests

# final base image
FROM openjdk:11-jre-slim

ARG APP_ID_NUMBER=61
ARG APP_ID_NAME=drsadm
ARG GROUP_ID_NUMBER=199
ARG GROUP_ID_NAME=appadmin

RUN groupadd -g ${GROUP_ID_NUMBER} ${GROUP_ID_NAME} && \
  useradd -u ${APP_ID_NUMBER} -g ${GROUP_ID_NUMBER} -s /bin/bash ${APP_ID_NAME} && \
  apt-get update && \
  apt-get install -y curl

USER ${APP_ID_NAME}

# set deployment directory
WORKDIR /home/${APP_ID_NAME}

# copy over the built artifact from the maven image
COPY  --from=maven ./target/verify-*.jar ./drs-verify.jar

# run java command
CMD ["java", "-jar", "-Xmx8192m", "./drs-verify.jar"]
