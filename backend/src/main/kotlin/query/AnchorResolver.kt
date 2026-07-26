package query

import java.sql.Connection

data class AnchorResult(val productId: String?, val score: Double)

fun resolveAnchor(queryText: String, connection: Connection): AnchorResult {
    val sql = """
        SELECT product_id, word_similarity(?, name) AS score
        FROM products
        ORDER BY score DESC
        LIMIT 1
    """.trimIndent()

    connection.prepareStatement(sql).use { statement ->
        statement.setString(1, queryText)
        val resultSet = statement.executeQuery()
        resultSet.next()

        val productId = resultSet.getString("product_id")
        val score = resultSet.getDouble("score")

        return if (score >= 0.4) {
            AnchorResult(productId, score)
        } else {
            AnchorResult(null, score)
        }
    }
}