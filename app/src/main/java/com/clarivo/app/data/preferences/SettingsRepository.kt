package com.clarivo.app.data.preferences

interface SettingsRepository {
    var uiMode: String
    var colorMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String
    var enableBlur: Boolean
    var enableFloatingBottomBar: Boolean
    var enableFloatingBottomBarBlur: Boolean
    var pageScale: Float
    var checkUpdate: Boolean
    var language: String
    var enablePredictiveBack: Boolean
}
