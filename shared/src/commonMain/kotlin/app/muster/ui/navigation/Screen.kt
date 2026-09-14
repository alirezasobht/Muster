package app.muster.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Launch

@Serializable
data object RequestCode

@Serializable
data class EnterCode(val email: String)

@Serializable
data object SetName

@Serializable
data object Home

@Serializable
data object Settings

@Serializable
data object NewGroup

@Serializable
data class Group(val id: String)
