import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val dusseldorfKtorVersion = "3.2.2.4-f09ec97"
val ktorVersion = "2.2.4"

val k9FormatVersion = "8.0.8"
val slf4jVersion = "2.0.7"
val kotlinxCoroutinesVersion = "1.6.4"
val openhtmltopdfVersion = "1.0.10"
val kafkaTestcontainerVersion = "1.17.6"
val kafkaVersion = "3.4.0"
val handlebarsVersion = "4.3.1"
val fuelVersion = "2.3.1"
val jsonassertVersion = "1.5.1"

val mainClass = "no.nav.helse.OmsorgspengerutbetalingeSoknadProsesseringKt"

plugins {
    kotlin("jvm") version "1.8.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    // Server
    implementation("no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-health:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$kotlinxCoroutinesVersion")
    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:$fuelVersion") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }

    // Client
    implementation("no.nav.helse:dusseldorf-ktor-client:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")

    // PDF
    implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:$openhtmltopdfVersion")
    implementation("com.openhtmltopdf:openhtmltopdf-slf4j:$openhtmltopdfVersion")
    implementation("org.slf4j:jcl-over-slf4j:$slf4jVersion")
    implementation("com.github.jknack:handlebars:$handlebarsVersion")

    // Kafka
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")

    //K9-format
    implementation("no.nav.k9:soknad:$k9FormatVersion")

    // Test
    testImplementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    testImplementation("org.testcontainers:kafka:$kafkaTestcontainerVersion")
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/k9-format")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()

    maven("https://jitpack.io")
}


java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("app")
    archiveClassifier.set("")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to mainClass
            )
        )
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "8.0.2"
}

tasks.withType<Test> {
    useJUnitPlatform()
}
