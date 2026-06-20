package com.example.ui

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
data class Editor(val templateId: String, val imageUrl: String)

@Serializable
object Admin
