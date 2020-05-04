@file:Suppress("SpellCheckingInspection")

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "ktor.funny"
version = "0.0.1"

application {
    mainClassName = "ServerKt"
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClassName
            )
        )
    }
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
    maven { url = uri("https://kotlin.bintray.com/kotlin-js-wrappers") }
    maven { url = uri("https://kotlin.bintray.com/kotlinx") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-html-builder:$ktor_version")
    implementation("org.jetbrains:kotlin-css-jvm:1.0.0-pre.31-kotlin-1.2.41")
    implementation("io.ktor:ktor-freemarker:$ktor_version")
    implementation("io.ktor:ktor-velocity:$ktor_version")
    implementation("io.ktor:ktor-mustache:$ktor_version")
    implementation("io.ktor:ktor-thymeleaf:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-locations:$ktor_version")
    implementation("io.ktor:ktor-metrics:$ktor_version")
    implementation("io.ktor:ktor-server-sessions:$ktor_version")
    implementation("io.ktor:ktor-webjars:$ktor_version")
    implementation("org.webjars:jquery:3.2.1")
    implementation("io.ktor:ktor-websockets:$ktor_version")
    implementation("io.ktor:ktor-auth:$ktor_version")
    implementation("io.ktor:ktor-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-auth-ldap:$ktor_version")
    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("io.ktor:ktor-network:$ktor_version")
    implementation("io.ktor:ktor-network-tls:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-apache:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-jetty:$ktor_version")
    implementation("io.ktor:ktor-client-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-gson:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")
    implementation("io.ktor:ktor-client-logging-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("io.ktor:ktor-client-mock:$ktor_version")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktor_version")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")
