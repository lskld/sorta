plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
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

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
