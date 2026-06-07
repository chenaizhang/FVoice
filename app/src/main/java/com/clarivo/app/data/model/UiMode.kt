package com.clarivo.app.data.model

enum class UiMode(val value: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        const val DEFAULT_VALUE = "miuix"
        fun fromValue(value: String) = entries.find { it.value == value } ?: Miuix
    }
}
