package com.clarivo.app.core.model

data class ModelInfo(
    val id: String,
    val name: String,
    val type: ModelType,
    val version: String = "",
    val sizeBytes: Long = 0,
    val status: ModelStatus = ModelStatus.NOT_INSTALLED,
    val path: String = "",
    val isBundled: Boolean = false,
    val license: String = "",
    val description: String = "",
    val sourceUrl: String = ""
)
