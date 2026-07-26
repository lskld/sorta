package db

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.pgvector.PGvector
import embedding.embed
import java.nio.file.Paths
import java.sql.DriverManager

fun main() {
    val url = "jdbc:postgresql://localhost:5433/sorta"
    val user = "postgres"
    val password = "postgres"

    val env = OrtEnvironment.getEnvironment()
    val session = env.createSession(
        "models/bge-small-en-v1.5-onnx/model.onnx",
        OrtSession.SessionOptions()
    )
    val tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("models/bge-small-en-v1.5-onnx"))

    DriverManager.getConnection(url, user, password).use { connection ->
        PGvector.addVectorType(connection)

        val products = mutableListOf<Pair<String, String>>()
        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT product_id, name FROM products")
            while (resultSet.next()) {
                products.add(resultSet.getString("product_id") to resultSet.getString("name"))
            }
        }

        println("Loaded ${products.size} products")

        val updateSql = "UPDATE products SET embedding = ? WHERE product_id = ?"
        connection.prepareStatement(updateSql).use { updateStatement ->
            for ((index, product) in products.withIndex()) {
                val (productId, name) = product
                val vector = embed(name, tokenizer, env, session)

                updateStatement.setObject(1, PGvector(vector))
                updateStatement.setString(2, productId)
                updateStatement.executeUpdate()

                if ((index + 1) % 100 == 0) {
                    println("Embedded ${index + 1} / ${products.size}")
                }
            }
        }
        println("Done.")
    }

    session.close()
    tokenizer.close()
    env.close()
}