package com.example.models

data class Template(
    val id: String,
    val category: String, // Festivals, Business, Motivation
    val imageUrl: String,
    val isPremium: Boolean
)
