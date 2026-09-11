package com.lab.assistant.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LlmEngine(private val ctx: Context) {
    private var llm: LlmInference? = null
    private var loadedPath = ""

    fun isReady() = llm != null

    suspend fun load(modelPath: String, useGpu: Boolean = true, maxTokens: Int = 2048) = withContext(Dispatchers.IO) {
        if (loadedPath == modelPath && llm != null) return@withContext true
        close()
        if (modelPath.isBlank() || !java.io.File(modelPath).exists()) return@withContext false
        try {
            val opts = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(maxTokens)
                .setPreferredBackend(if (useGpu) LlmInference.Backend.GPU else LlmInference.Backend.CPU)
                .build()
            llm = LlmInference.createFromOptions(ctx, opts)
            loadedPath = modelPath
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generate(prompt: String, temp: Float = 0.8f, onPartial: (String) -> Unit = {}): String = withContext(Dispatchers.IO) {
        val engine = llm ?: return@withContext "Load a model first in Models tab."
        val sys = "You are Lab Assistant, a defensive-security tutor. Only help with authorized, defensive, educational use in isolated labs. Refuse real-world attack automation. Keep answers concise."
        val full = "<system>$sys</system>\n<user>$prompt</user>\n<assistant>"
        try {
            var acc = ""
            val session = LlmInference.LlmInferenceSession.createFromOptions(
                engine,
                LlmInference.LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTemperature(temp).build()
            )
            try {
                session.addQueryChunk(full)
                session.generateResponseAsync { partial, done ->
                    acc += partial
                    onPartial(acc)
                }
            } finally {
                try { session.close() } catch (_: Exception) {}
            }
            val t0 = System.currentTimeMillis()
            while (System.currentTimeMillis() - t0 < 120000) {
                Thread.sleep(50)
                if (acc.isNotEmpty()) break
            }
            acc.ifBlank { "Model returned empty. Try a smaller prompt or re-load model." }
        } catch (e: Exception) {
            "Inference error: ${e.message}"
        }
    }

    fun close() {
        try { llm?.close() } catch (_: Exception) {}
        llm = null; loadedPath = ""
    }
}
