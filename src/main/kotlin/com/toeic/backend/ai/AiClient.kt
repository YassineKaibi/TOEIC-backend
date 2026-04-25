package com.toeic.backend.ai

interface AiClient {
    suspend fun postQuizResult(payload: AiQuizResultRequest): AiQuizResultResponse
    suspend fun getRecommendations(studentId: String): AiRecommendationsResponse
    fun close()
}
