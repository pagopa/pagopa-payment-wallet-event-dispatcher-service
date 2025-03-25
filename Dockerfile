FROM amazoncorretto:17-alpine@sha256:7ce85412211246ff5ce362b59aa5dbdc9093d8bb088da7f7c8b9ef09a8704cdf as build
WORKDIR /workspace/app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.lockfile .
COPY gradle.properties .

COPY eclipse-style.xml eclipse-style.xml
COPY api-spec api-spec
COPY src src
RUN ./gradlew build -x test
RUN mkdir build/extracted && java -Djarmode=layertools -jar build/libs/*.jar extract --destination build/extracted

FROM amazoncorretto:17-alpine@sha256:7ce85412211246ff5ce362b59aa5dbdc9093d8bb088da7f7c8b9ef09a8704cdf

RUN addgroup --system user && adduser --ingroup user --system user
USER user:user

WORKDIR /app/

ARG EXTRACTED=/workspace/app/build/extracted

# OpenTelemetry agent
ADD --chown=user https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.3.0/opentelemetry-javaagent.jar .

COPY --from=build --chown=user ${EXTRACTED}/dependencies/ ./
RUN true
COPY --from=build --chown=user ${EXTRACTED}/spring-boot-loader/ ./
RUN true
COPY --from=build --chown=user ${EXTRACTED}/snapshot-dependencies/ ./
RUN true
COPY --from=build --chown=user ${EXTRACTED}/application/ ./
RUN true

ENTRYPOINT ["java","-javaagent:opentelemetry-javaagent.jar","org.springframework.boot.loader.launch.JarLauncher"]
