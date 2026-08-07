package com.lskld

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.pgvector.PGvector
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import query.getReferenceDate
import query.runQuery
import java.nio.file.Paths
import java.sql.DriverManager
import io.github.cdimascio.dotenv.Dotenv

@Serializable
data class QueryRequest(
    @SerialName("query_text") val queryText: String,
    @SerialName("anchor_id") val anchorId: String? = null
)

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json()
    }

    val env = OrtEnvironment.getEnvironment()
    val session = env.createSession(
        "models/bge-small-en-v1.5-onnx/model.onnx",
        OrtSession.SessionOptions()
    )
    val tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("models/bge-small-en-v1.5-onnx"))

    val dotenv = Dotenv.load()
    val port = dotenv["DB_PORT"] ?: "5432"
    val user = dotenv["DB_USER"] ?: throw IllegalStateException("DB_USER not defined")
    val password = dotenv["DB_PASSWORD"] ?: throw IllegalStateException("DB_PASSWORD not defined")

    val connection = DriverManager.getConnection(
        "jdbc:postgresql://localhost:${port}/sorta",
        user,
        password
    )
    PGvector.addVectorType(connection)

    val refDate = getReferenceDate(connection)

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }

        post("/query") {
            val request = call.receive<QueryRequest>()
            val response = runQuery(
                queryText = request.queryText,
                anchorId = request.anchorId,
                refDate = refDate,
                tokenizer = tokenizer,
                env = env,
                session = session,
                connection = connection
            )
            call.respond(response)
        }
    }
}