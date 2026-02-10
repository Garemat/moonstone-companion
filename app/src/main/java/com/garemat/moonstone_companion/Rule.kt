package com.garemat.moonstone_companion

import kotlinx.serialization.Serializable

@Serializable
data class RuleSection(
    val id: String,
    val title: String,
    val category: String,
    val keywords: List<String> = emptyList(),
    val content: String,
    val searchable: Boolean = true
)
