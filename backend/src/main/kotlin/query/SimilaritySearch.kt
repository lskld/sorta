package query

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.pgvector.PGvector
import embedding.embed
import java.sql.Connection

data class SearchResult(val productId: String, val name: String, val category: String, val distance: Double)

fun searchBySimilarity(
    queryText: String,
    tokenizer: HuggingFaceTokenizer,
    env: OrtEnvironment,
    session: OrtSession,
    connection: Connection,
    limit: Int = 10
): List<SearchResult> {
    val queryEmbedding = embed(queryText, tokenizer, env, session)

    val sql = """
        SELECT product_id, name, category, embedding <-> ? AS distance 
        FROM products
        ORDER BY distance
        LIMIT ? 
    """.trimIndent()

    connection.prepareStatement(sql).use { statement ->
        statement.setObject(1, PGvector(queryEmbedding))
        statement.setInt(2, limit)

        val resultSet = statement.executeQuery()
        val results = mutableListOf<SearchResult>()
        while (resultSet.next()) {
            results.add(
                SearchResult(
                    productId = resultSet.getString("product_id"),
                    name = resultSet.getString("name"),
                    category = resultSet.getString("category"),
                    distance = resultSet.getDouble("distance")
                )
            )
        }
        return results
    }
}