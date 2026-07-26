package query

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.pgvector.PGvector
import java.nio.file.Paths
import java.sql.DriverManager

fun main() {
    val env = OrtEnvironment.getEnvironment()
    val session = env.createSession("models/bge-small-en-v1.5-onnx/model.onnx", OrtSession.SessionOptions())
    val tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("models/bge-small-en-v1.5-onnx"))

    val url = "jdbc:postgresql://localhost:5433/sorta"
    val user = "postgres"
    val password = "postgres"

    DriverManager.getConnection(url, user, password).use { connection ->
        PGvector.addVectorType(connection)

        val results = searchBySimilarity("cozy winter gifts", tokenizer, env, session, connection)
        for (r in results) {
            println("${r.name} (${r.category}) - distance ${r.distance}")
        }
    }

    session.close()
    tokenizer.close()
    env.close()
}