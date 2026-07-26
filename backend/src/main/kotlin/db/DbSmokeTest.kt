package db

import java.sql.DriverManager

fun main() {
    val url = "jdbc:postgresql://localhost:5433/sorta"
    val user = "postgres"
    val password = "postgres"

    DriverManager.getConnection(url, user, password).use { connection ->
        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT count(*) FROM products")
            resultSet.next()
            val count = resultSet.getInt(1)
            println("Product count: $count")
        }
    }
}