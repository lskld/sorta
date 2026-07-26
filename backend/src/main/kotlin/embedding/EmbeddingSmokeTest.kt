package embedding

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.file.Paths

fun main() {
    val env = OrtEnvironment.getEnvironment()
    val session = env.createSession(
        "models/bge-small-en-v1.5-onnx/model.onnx",
        OrtSession.SessionOptions()
    )
    val tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("models/bge-small-en-v1.5-onnx"))

    println("Model inputs: ${session.inputNames}")
    println("Model outputs: ${session.outputNames}")

    val texts = listOf("blue jacket", "red raincoat", "chocolate cake recipe")
    val embeddings = texts.map { embed(it, tokenizer, env, session) }

    for (i in texts.indices) {
        for (j in i + 1 until texts.size) {
            val sim = cosineSimilarity(embeddings[i], embeddings[j])
            println("similarity(\"${texts[i]}\", \"${texts[j]}\") = $sim")
        }
    }

    session.close()
    tokenizer.close()
    env.close()
}

fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    var dot = 0f
    for (i in a.indices) dot += a[i] * b[i]
    return dot
}