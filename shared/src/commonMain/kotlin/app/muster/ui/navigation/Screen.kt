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
data class Group(val id: String, val name: String)

@Serializable
data class AddMemberByEmail(val groupId: String, val groupName: String)

@Serializable
data class NewEvent(val groupId: String)

@Serializable
data class Event(val id: String, val title: String, val groupId: String)
