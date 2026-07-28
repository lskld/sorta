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

    DriverManager.getConnection("jdbc:postgresql://localhost:5433/sorta", "postgres", "postgres").use { connection ->
        PGvector.addVectorType(connection)
        val refDate = getReferenceDate(connection)

        val realAnchorId = "22304"

        data class TestCase(val label: String, val queryText: String, val anchorId: String?)

        val testCases = listOf(
            TestCase("Case 1: anchor + date", "items sold last month", realAnchorId),
            TestCase("Case 2: anchor only", "similar items", realAnchorId),
            TestCase("Case 3: date only", "christmas decorations last month", null),
            TestCase("Case 4: neither", "christmas decorations", null),
            TestCase("Regression: invalid anchorId should fall back cleanly", "christmas decorations", "NOT_A_REAL_ID")
        )

        for (tc in testCases) {
            println("\n=== ${tc.label} (queryText=\"${tc.queryText}\", anchorId=${tc.anchorId}) ===")
            val response = runQuery(tc.queryText, tc.anchorId, refDate, tokenizer, env, session, connection)
            println("anchorResolved: ${response.anchorResolved}")
            for (r in response.results) {
                println("  ${r.productId} ${r.name} (${r.category}) - distance ${r.distance}, sold ${r.unitsSold}")
            }
        }
    }

    session.close()
    tokenizer.close()
    env.close()
}