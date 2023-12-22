import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.jlleitschuh.gradle.ktlint") version "12.0.3"
    kotlin("plugin.spring") version "1.9.21"
    kotlin("jvm") version "1.9.21"
}

group = "no.nav.helse.flex"
version = "1.0"
description = "flex-syketilfelle"
java.sourceCompatibility = JavaVersion.VERSION_17

ext["okhttp3.version"] = "4.9.3" // For at token support testen kjører

val githubUser: String by project
val githubPassword: String by project

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

val tokenSupportVersion = "3.2.0"
val logstashEncoderVersion = "7.4"
val testContainersVersion = "1.19.3"
val kluentVersion = "1.73"
val sykepengesoknadKafkaVersion = "2023.12.05-10.16-3ffa06f7"
val syfoSmCommon = "2.0.6"
val jsonSchemaValidatorVersion = "1.0.87"
val inntektsmeldingKontrakt = "2023.10.13-04-47-c372d"
val httpClientVersion = "5.3"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.slf4j:slf4j-api")
    implementation("org.flywaydb:flyway-core")
    implementation("org.apache.httpcomponents.client5:httpclient5:$httpClientVersion")
    implementation("org.postgresql:postgresql")
    implementation("org.hibernate.validator:hibernate-validator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")
    implementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:$inntektsmeldingKontrakt")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("no.nav.helse:syfosm-common-models:$syfoSmCommon")
    implementation("no.nav.helse.flex:sykepengesoknad-kafka:$sykepengesoknadKafkaVersion")

    testImplementation(platform("org.testcontainers:testcontainers-bom:$testContainersVersion"))
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.awaitility:awaitility")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("com.networknt:json-schema-validator:$jsonSchemaValidatorVersion")
}

tasks.getByName<BootJar>("bootJar") {
    this.archiveFileName.set("app.jar")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("STARTED", "PASSED", "FAILED", "SKIPPED")
        exceptionFormat = TestExceptionFormat.FULL
    }
}
