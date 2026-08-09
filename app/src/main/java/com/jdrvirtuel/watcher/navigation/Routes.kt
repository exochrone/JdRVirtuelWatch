package com.jdrvirtuel.watcher.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SettingsRoute

@Serializable
data object DebugRoute

@Serializable
data class ForumDetailRoute(val forumId: Int)

@Serializable
data object VerificationRoute

@Serializable
data object DiagnosticRoute
