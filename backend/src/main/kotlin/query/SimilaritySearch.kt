package query

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.pgvector.PGvector
import embedding.embed
import java.sql.Connection
import java.sql.Date

data class SearchResult(
    val productId: String,
    val name: String,
    val category: String,
    val distance: Double,
    val unitsSold: Int
)

fun searchBySimilarity(
    queryText: String,
    dateRange: DateRange? = null,
    tokenizer: HuggingFaceTokenizer,
    env: OrtEnvironment,
    session: OrtSession,
    connection: Connection,
    limit: Int = 10
): List<SearchResult> {
    val queryEmbedding = embed(queryText, tokenizer, env, session)

    val sql = if (dateRange != null) {
        """
        SELECT
            p.product_id, p.name, p.category,
            p.embedding <-> ? AS distance,
            COALESCE((SELECT SUM(quantity) FROM sales
                WHERE sales.product_id = p.product_id
                AND sales.sale_date BETWEEN ? AND ?), 0) AS units_sold
        FROM products p
        WHERE EXISTS (
            SELECT 1 FROM sales
            WHERE sales.product_id = p.product_id
            AND sales.sale_date BETWEEN ? AND ?
        )
        ORDER BY distance
        LIMIT ?
        """.trimIndent()
    } else {
        """
        SELECT
            p.product_id, p.name, p.category,
            p.embedding <-> ? AS distance,
            COALESCE((SELECT SUM(quantity) FROM sales
                WHERE sales.product_id = p.product_id), 0) AS units_sold
        FROM products p
        ORDER BY distance
        LIMIT ?
        """.trimIndent()
    }

    connection.prepareStatement(sql).use { statement ->
        var paramIndex = 1
        statement.setObject(paramIndex++, PGvector(queryEmbedding))

        if (dateRange != null) {
            statement.setDate(paramIndex++, Date.valueOf(dateRange.start))
            statement.setDate(paramIndex++, Date.valueOf(dateRange.end))
            statement.setDate(paramIndex++, Date.valueOf(dateRange.start))
            statement.setDate(paramIndex++, Date.valueOf(dateRange.end))
        }

        statement.setInt(paramIndex, limit)

        val resultSet = statement.executeQuery()
        val results = mutableListOf<SearchResult>()
        while (resultSet.next()) {
            results.add(
                SearchResult(
                    productId = resultSet.getString("product_id"),
                    name = resultSet.getString("name"),
                    category = resultSet.getString("category"),
                    distance = resultSet.getDouble("distance"),
                    unitsSold = resultSet.getInt("units_sold")
                )
            )
        }
        return results
    }
}