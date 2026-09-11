package com.lab.assistant.data

data class OpenModel(
    val id: String,
    val name: String,
    val desc: String,
    val sizeMb: Int,
    val url: String,
    val fileName: String
)

object ModelsCatalog {
    val models = listOf(
        OpenModel(
            "gemma2-2b-it",
            "Gemma 2 2B IT (int8 GPU)",
            "Google open model, instruction-tuned. Good balance for phones. Requires license accept on Kaggle.",
            1800,
            "https://www.kaggle.com/models/google/gemma-2/tfLite/gemma2-2b-it-gpu-int8/1/download/gemma2-2b-it-gpu-int8.task",
            "gemma2-2b-it-gpu-int8.task"
        ),
        OpenModel(
            "phi3-mini",
            "Phi-3 Mini (int8)",
            "Microsoft open model, strong reasoning for size. Convert to .task via MediaPipe converter.",
            2300,
            "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct/resolve/main/phi-3-mini.task?download=true",
            "phi-3-mini.task"
        ),
        OpenModel(
            "llama32-3b",
            "Llama 3.2 3B IT (int8)",
            "Meta open model, multilingual. Needs .task conversion, best quality on 8GB+ RAM.",
            2800,
            "https://huggingface.co/meta-llama/Llama-3.2-3B-Instruct/resolve/main/llama32-3b.task?download=true",
            "llama32-3b.task"
        )
    )
}
