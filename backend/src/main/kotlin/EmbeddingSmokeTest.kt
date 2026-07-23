package com.lskld.embedding

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer
import java.nio.file.Paths
import kotlin.math.sqrt

fun embed(
    text: String,
    tokenizer: HuggingFaceTokenizer,
    env: OrtEnvironment,
    session: OrtSession
): FloatArray {
    val encoding = tokenizer.encode(text)
    val inputIds = encoding.ids
    val attentionMask = encoding.attentionMask
    val tokenTypeIds = LongArray(inputIds.size) { 0L }

    val shape = longArrayOf(1, inputIds.size.toLong())

    OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape).use { idsTensor ->
        OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape).use { maskTensor ->
            OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), shape).use { typeTensor ->

                val inputs = mapOf(
                    "input_ids" to idsTensor,
                    "attention_mask" to maskTensor,
                    "token_type_ids" to typeTensor
                )

                session.run(inputs).use { result ->
                    @Suppress("UNCHECKED_CAST")
                    val lastHiddenState = result.get(0).value as Array<Array<FloatArray>>
                    val clsVector = lastHiddenState[0][0]
                    return normalize(clsVector)
                }
        }}
    }
}

fun normalize(vec: FloatArray): FloatArray {
    val norm = sqrt(vec.sumOf { (it * it).toDouble() }).toFloat()
    return FloatArray(vec.size) { i -> vec[i] / norm }
}

fun cosineSimiliarity(a: FloatArray, b: FloatArray): Float {
    var dot = 0f
    for (i in a.indices) dot += a[i] * b[i]
    return dot
}

fun main() {
    val env = OrtEnvironment.getEnvironment()
    val session = env.createSession(
        "C:\\Users\\theol\\source\\repos\\sorta\\backend\\models\\bge-small-en-v1.5-onnx\\model.onnx",
        OrtSession.SessionOptions()
    )
    val tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("C:\\Users\\theol\\source\\repos\\sorta\\backend\\models\\bge-small-en-v1.5-onnx"))

    println("Model inputs: ${session.inputNames}")
    println("Model outputs: ${session.outputNames}")

    val texts = listOf("blue jacket", "red raincoat", "chocolate cake recipe")
    val embeddings = texts.map { embed(it, tokenizer, env, session) }

    for (i in texts.indices) {
        for (j in i + 1 until texts.size) {
            val sim = cosineSimiliarity(embeddings[i], embeddings[j])
            println("similarity(\"${texts[i]}\", \"${texts[j]}\") = $sim")
        }
    }

    session.close()
    tokenizer.close()
    env.close()
}