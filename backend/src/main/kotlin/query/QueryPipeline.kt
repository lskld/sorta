package query

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.pgvector.PGvector
import embedding.embed
import org.postgresql.jdbc.PgArray
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.time.LocalDate

data class SearchResult(
    val productId: String,
    val name: String,
    val category: String,
    val distance: Double,
    val unitsSold: Int
)

data class QueryResponse(
    val results: List<SearchResult>,
    val anchorResolved: Boolean
)

fun fetchProductEmbedding(productId: String, connection: Connection): PGvector? {
    val sql = "SELECT embedding FROM products WHERE product_id = ?"
    connection.prepareStatement(sql).use { statement ->
        statement.setString(1, productId)
        val resultSet = statement.executeQuery()
        if (!resultSet.next()) return null
        return resultSet.getObject("embedding") as? PGvector
    }
}

private fun collectResults(statement: PreparedStatement): List<SearchResult> {
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

fun runQuery(
    queryText: String,
    refDate: LocalDate,
    tokenizer: HuggingFaceTokenizer,
    env: OrtEnvironment,
    session: OrtSession,
    connection: Connection,
    limit: Int = 10
): QueryResponse {
    val anchor = resolveAnchor(queryText, connection)
    val dateRange = resolveDatePhrase(queryText, refDate)

    val targetEmbedding: PGvector = if (anchor.productId != null) {
        fetchProductEmbedding(anchor.productId, connection)
            ?: PGvector(embed(queryText, tokenizer, env, session))
    } else {
        PGvector(embed(queryText, tokenizer, env, session))
    }

    val results: List<SearchResult> = when {
        // Case 1: Anchor + Date Present
        anchor.productId != null && dateRange != null -> {
            val sql = """
                SELECT p.product_id, p.name, p.category,
                    p.embedding <-> ? AS distance,
                    COALESCE((SELECT SUM(quantity) FROM sales
                        WHERE sales.product_id = p.product_id
                        AND sales.sale_date BETWEEN ? AND ?), 0) AS unitsSold
                FROM products p
                WHERE p.product_id != ?
                    AND EXISTS (
                        SELECT 1 FROM sales
                        WHERE sales.product_id = p.product_id
                        AND sales.sale_date BETWEEN ? AND ?
                    )
                ORDER BY distance
                LIMIT ?
            """.trimIndent()
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, targetEmbedding)
                statement.setDate(2, Date.valueOf(dateRange.start))
                statement.setDate(3, Date.valueOf(dateRange.end))
                statement.setString(4, anchor.productId)
                statement.setDate(5, Date.valueOf(dateRange.start))
                statement.setDate(6, Date.valueOf(dateRange.end))
                statement.setInt(7, limit)
                collectResults(statement)
            }
        }
        // Case 2: Anchor only, no date
        anchor.productId != null -> {
            val sql = """
                SELECT p.product_id, p.name, p.category,
                    p.embedding <-> ? AS distance,
                    COALESCE((SELECT SUM(quantity) FROM sales
                              WHERE sales.product_id = p.product_id), 0) AS units_sold
                FROM products p
                WHERE p.product_id != ?
                ORDER BY distance
                LIMIT ?
            """.trimIndent()
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, targetEmbedding)
                statement.setString(2, anchor.productId)
                statement.setInt(3, limit)
                collectResults(statement)
            }
        }
        //Case 3: Date only, no anchor
        dateRange != null -> {
            val sql = """
                SELECT p.product_id, p.name, p.category,
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
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, targetEmbedding)
                statement.setDate(2, Date.valueOf(dateRange.start))
                statement.setDate(3, Date.valueOf(dateRange.end))
                statement.setDate(4, Date.valueOf(dateRange.start))
                statement.setDate(5, Date.valueOf(dateRange.end))
                statement.setInt(6, limit)
                collectResults(statement)
            }
        }
        // Case 4: No date, no anchor
        else -> {
            val sql = """
                SELECT p.product_id, p.name, p.category,
                    p.embedding <-> ? AS distance,
                    COALESCE((SELECT SUM(quantity) FROM sales
                              WHERE sales.product_id = p.product_id), 0) AS units_sold
                FROM products p
                ORDER BY distance
                LIMIT ?
            """.trimIndent()
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, targetEmbedding)
                statement.setInt(2, limit)
                collectResults(statement)
            }
        }
    }
    return QueryResponse(results = results, anchorResolved = anchor.productId != null)
}