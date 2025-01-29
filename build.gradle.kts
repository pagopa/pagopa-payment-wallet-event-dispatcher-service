import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

group = "it.pagopa.wallet.eventdispatcher"

version = "0.9.1"

description = "pagopa-wallet-event-dispatcher-service"

object Deps {
  const val kotlinBom = "1.7.22"
  const val kotlinCoroutinesBom = "1.6.4"
  const val springBootVersion = "3.3.1"
  const val springCloudAzureVersion = "5.13.0"
  const val vavrVersion = "0.10.4"
  const val nettyMacosResolver = "4.1.90.Final"
  const val ecsLoggingVersion = "1.5.0"
  const val googleFindBugs = "3.0.2"
  const val mockitoKotlin = "4.0.0"
  const val openapiGenerator = "7.1.0"
  const val openapiDataBinding = "0.2.6"
  const val mockWebServer = "4.12.0"
  const val openTelemetryVersion = "1.37.0"
  const val openTelemetryInstrumentationVersion = "2.3.0-alpha"
  const val mockitoInline = "5.2.0"
  const val reactorKafka = "1.3.23"
  const val swaggerAnnotations = "2.2.8"
}

plugins {
  id("java")
  id("org.springframework.boot") version "3.3.1"
  id("io.spring.dependency-management") version "1.1.5"
  id("com.diffplug.spotless") version "6.18.0"
  id("org.openapi.generator") version "7.1.0"
  id("org.sonarqube") version "4.4.1.3373"
  id("com.dipien.semantic-version") version "2.0.0" apply false
  kotlin("plugin.spring") version "1.8.10"
  kotlin("jvm") version "1.8.10"
  jacoco
  application
}

java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "17" }

repositories { mavenCentral() }

dependencyManagement {
  imports {
    mavenBom("org.springframework.boot:spring-boot-dependencies:${Deps.springBootVersion}")
  }
  imports {
    mavenBom("com.azure.spring:spring-cloud-azure-dependencies:${Deps.springCloudAzureVersion}")
  }
  // Kotlin BOM
  imports { mavenBom("org.jetbrains.kotlin:kotlin-bom:${Deps.kotlinBom}") }
  imports { mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:${Deps.kotlinCoroutinesBom}") }
  // otel BOM
  imports {
    mavenBom("io.opentelemetry:opentelemetry-bom:${Deps.openTelemetryVersion}")
    mavenBom(
      "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${Deps.openTelemetryInstrumentationVersion}"
    )
  }
}

dependencies {
  implementation("com.azure.spring:spring-cloud-azure-starter")
  implementation("io.projectreactor:reactor-core")

  // spring integration
  implementation("org.springframework.boot:spring-boot-starter-integration")

  // azure
  implementation("com.azure.spring:spring-cloud-azure-starter-storage-queue")
  implementation("com.azure.spring:spring-cloud-azure-starter-integration-storage-queue")
  implementation("com.azure:azure-storage-queue")
  implementation("com.azure:azure-core-serializer-json-jackson")

  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("com.google.code.findbugs:jsr305:${Deps.googleFindBugs}")
  implementation("org.projectlombok:lombok")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("org.springframework.boot:spring-boot-starter-aop")
  implementation("io.netty:netty-resolver-dns-native-macos:${Deps.nettyMacosResolver}")
  implementation("io.vavr:vavr:${Deps.vavrVersion}")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

  // Kotlin dependencies
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

  // ECS logback encoder
  implementation("co.elastic.logging:logback-ecs-encoder:${Deps.ecsLoggingVersion}")
  // otel api
  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry.instrumentation:opentelemetry-reactor-3.1")

  // openapi
  implementation("org.openapitools:openapi-generator-gradle-plugin:${Deps.openapiGenerator}")
  implementation("org.openapitools:jackson-databind-nullable:${Deps.openapiDataBinding}")
  implementation("jakarta.xml.bind:jakarta.xml.bind-api")
  implementation("io.swagger.core.v3:swagger-annotations:${Deps.swaggerAnnotations}")

  // Azure Event Hubs (Kafka)
  implementation("org.springframework.kafka:spring-kafka")
  implementation("org.apache.kafka:kafka-clients")
  implementation("io.projectreactor.kafka:reactor-kafka:${Deps.reactorKafka}")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  runtimeOnly("org.springframework.boot:spring-boot-devtools")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.mockito:mockito-inline:${Deps.mockitoInline}")
  testImplementation("io.projectreactor:reactor-test")
  // Kotlin dependencies
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
  testImplementation("org.mockito.kotlin:mockito-kotlin:${Deps.mockitoKotlin}")

  testImplementation("com.squareup.okhttp3:mockwebserver:${Deps.mockWebServer}")
}

configurations {
  implementation.configure {
    exclude(module = "spring-boot-starter-web")
    exclude("org.apache.tomcat")
    exclude(group = "org.slf4j", module = "slf4j-simple")
  }
}
// Dependency locking - lock all dependencies
dependencyLocking { lockAllConfigurations() }

sourceSets {
  main {
    java { srcDirs("${layout.buildDirectory.get().asFile.path}/generated/src/main/java") }
    kotlin {
      srcDirs(
        "src/main/kotlin",
        "${layout.buildDirectory.get().asFile.path}/generated/src/main/kotlin"
      )
    }
    resources { srcDirs("src/resources") }
  }
}

springBoot {
  mainClass.set("it.pagopa.wallet.eventdispatcher.WalletEventDispatcherApplicationKt")
  buildInfo { properties { additional.set(mapOf("description" to project.description)) } }
}

tasks.create("applySemanticVersionPlugin") {
  dependsOn("prepareKotlinBuildScriptModel")
  apply(plugin = "com.dipien.semantic-version")
}

tasks.withType<KotlinCompile> {
  dependsOn("walletsApi", "eventDispatcherApi")
  kotlinOptions.jvmTarget = "17"
}

tasks.withType(JavaCompile::class.java).configureEach { options.encoding = "UTF-8" }

tasks.withType(Javadoc::class.java).configureEach { options.encoding = "UTF-8" }

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    toggleOffOn()
    targetExclude("build/**/*")
    ktfmt().kotlinlangStyle()
  }
  kotlinGradle {
    toggleOffOn()
    targetExclude("build/**/*.kts")
    ktfmt().googleStyle()
  }
  java {
    target("**/*.java")
    targetExclude("build/**/*")
    eclipse().configFile("eclipse-style.xml")
    toggleOffOn()
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.named<Jar>("jar") { enabled = false }

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
  dependsOn(tasks.test) // tests are required to run before generating the report

  classDirectories.setFrom(
    files(
      classDirectories.files.map {
        fileTree(it).matching { exclude("it/pagopa/wallet/WalletApplicationKt.class") }
      }
    )
  )

  reports { xml.required.set(true) }
}

/**
 * Task used to expand application properties with build specific properties such as artifact name
 * and version
 */
tasks.processResources { filesMatching("application.properties") { expand(project.properties) } }

tasks.register<GenerateTask>("walletsApi") {
  description = "Generate API client based on Wallet OpenAPI spec"
  group = "openapi-generate"

  generatorName.set("java")
  remoteInputSpec.set(
    "https://raw.githubusercontent.com/pagopa/pagopa-wallet-service/main/api-spec/wallet-api.yaml"
  )
  outputDir.set(layout.buildDirectory.dir("generated").get().asFile.path)
  apiPackage.set("it.pagopa.generated.wallets.api")
  modelPackage.set("it.pagopa.generated.wallets.model")
  generateApiTests.set(false)
  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
  library.set("webclient")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "true",
      "interfaceOnly" to "true",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "reactive" to "true",
      "useSpringBoot3" to "true",
      "oas3" to "true",
      "generateSupportingFiles" to "false",
      "useJakartaEe" to "true",
      "useOneOfInterfaces" to "true"
    )
  )
}

tasks.register<GenerateTask>("eventDispatcherApi") {
  description = "Generate API client based on Payment wallet event dispatcher OpenAPI spec"
  group = "openapi-generate"

  generatorName = "kotlin-spring"
  inputSpec = "$projectDir/api-spec/event-dispatcher-api.yaml"
  outputDir = layout.buildDirectory.dir("generated").get().asFile.path
  apiPackage = "it.pagopa.generated.paymentwallet.eventdispatcher.server.api"
  modelPackage = "it.pagopa.generated.paymentwallet.eventdispatcher.server.model"
  generateApiTests = false
  generateApiDocumentation = false
  generateModelTests = false
  library = "spring-boot"
  modelNameSuffix = "Dto"
  configOptions =
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "true",
      "interfaceOnly" to "true",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "reactive" to "true",
      "useSpringBoot3" to "true",
      "oas3" to "true",
      "generateSupportingFiles" to "false",
      "enumPropertyNaming" to "UPPERCASE",
      "useJakartaEe" to "true",
      "useOneOfInterfaces" to "true"
    )
}
