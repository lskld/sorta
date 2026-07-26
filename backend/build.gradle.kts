plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
}

tasks.withType<JavaExec> {
    workingDir = rootDir
}

group = "com.lskld"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(libs.logback.classic)
    implementation("com.microsoft.onnxruntime:onnxruntime:1.27.0")
    implementation("ai.djl.huggingface:tokenizers:0.36.0")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("com.pgvector:pgvector:0.1.6")

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
