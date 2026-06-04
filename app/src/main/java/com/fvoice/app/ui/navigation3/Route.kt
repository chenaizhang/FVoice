package com.fvoice.app.ui.navigation3

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation keys for Navigation3.
 * Each destination is a NavKey and can be saved/restored in the back stack.
 */
sealed interface Route : NavKey, Parcelable {
    @Parcelize
    @Serializable
    data object Welcome : Route

    @Parcelize
    @Serializable
    data object Main : Route

    @Parcelize
    @Serializable
    data object Home : Route

    @Parcelize
    @Serializable
    data object Task : Route

    @Parcelize
    @Serializable
    data object Transcript : Route

    @Parcelize
    @Serializable
    data object Settings : Route

    @Parcelize
    @Serializable
    data object ProcessSettings : Route

    @Parcelize
    @Serializable
    data object Processing : Route

    @Parcelize
    @Serializable
    data object ResultDetail : Route

    @Parcelize
    @Serializable
    data object ThemeSettings : Route

    @Parcelize
    @Serializable
    data object PermissionSettings : Route

    @Parcelize
    @Serializable
    data object About : Route
}
