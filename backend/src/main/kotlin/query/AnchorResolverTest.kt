package query

import java.sql.DriverManager

fun main() {
    val url = "jdbc:postgresql://localhost:5433/sorta"
    val user = "postgres"
    val password = "postgres"

    DriverManager.getConnection(url, user, password).use { connection ->
        val testQueries = listOf("blue mug", "xyzzy quux")

        for (text in testQueries) {
            val result = resolveAnchor(text, connection)
            println("\"$text\" -> productId=${result.productId}, score=${result.score}")
        }
    }
}