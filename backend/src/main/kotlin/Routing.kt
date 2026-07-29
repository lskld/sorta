package com.lskld

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.pgvector.PGvector
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import query.getReferenceDate
import java.nio.file.Paths
import java.sql.DriverManager

fun Application.configureRouting() {

    val env = OrtEnvironment.getEnvironment()
    val session = env.createSession(
        "models/bge-small-en-v1.5-onnx/model.onnx",
        OrtSession.SessionOptions()
    )
    val tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("models/bge-small-en-v1.5-onnx"))

    val connection = DriverManager.getConnection(
        "jdbc:postgresql://localhost:5433/sorta",
        "postgres",
        "postgres"
    )
    PGvector.addVectorType(connection)

    val refDate = getReferenceDate(connection)

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
    }
}