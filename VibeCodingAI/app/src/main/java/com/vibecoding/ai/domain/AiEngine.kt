package com.vibecoding.ai.domain

data class GeneratedFile(val path: String, val content: String)
data class GenerationResult(val summary: String, val files: List<GeneratedFile>)

interface AiEngine {
    val displayName: String
    suspend fun generate(command: String, existing: Map<String, String>): GenerationResult
}
