package com.lab.assistant.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LlmEngine(private val ctx: Context) {
    private var llm: LlmInference? = null
    private var loadedPath = ""
    private var loadedMax = 0

    fun isReady() = llm != null

    suspend fun load(modelPath: String, useGpu: Boolean = true, maxTokens: Int = 2048) = withContext(Dispatchers.IO) {
        if (loadedPath == modelPath && llm != null && loadedMax == maxTokens) return@withContext true
        close()
        if (modelPath.isBlank() || !java.io.File(modelPath).exists()) return@withContext false
        try {
            val opts = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(maxTokens)
                .build()
            llm = LlmInference.createFromOptions(ctx, opts)
            loadedPath = modelPath
            loadedMax = maxTokens
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generate(prompt: String, temp: Float = 0.8f, onPartial: (String) -> Unit = {}): String = withContext(Dispatchers.IO) {
        val engine = llm ?: return@withContext "Load a model first in Models tab."
        val sys = "You are Lab Assistant, a defensive-security tutor. Only help with authorized, defensive, educational use in isolated labs. Refuse real-world attack automation. Keep answers concise."
        val full = "System: $sys\nUser: $prompt\nAssistant:"
        try {
            val out = engine.generateResponse(full)
            onPartial(out)
            out.ifBlank { "Model returned empty. Try a smaller prompt or re-load model." }
        } catch (e: Exception) {
            "Inference error: ${e.message}"
        }
    }

    fun close() {
        try { llm?.close() } catch (_: Exception) {}
        llm = null; loadedPath = ""; loadedMax = 0
    }
}
