package com.toeic.backend.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiQuizResultRequest(
    val submissionId: String,
    val studentId: String,
    val quizId: String,
    val score: Double,
    val maxScore: Double,
    val correctCount: Int,
    val totalCount: Int,
    val durationSeconds: Int,
    val answers: List<AiAnswerEntry>,
    val submittedAt: String   // ISO-8601 from Instant.toString()
)

@Serializable
data class AiAnswerEntry(
    val questionId: String,
    val selectedOption: String,
    val correctOption: String,
    val correct: Boolean,
    val points: Double
)

@Serializable
data class AiQuizResultResponse(val status: String) // expect "received"

@Serializable
data class AiRecommendationsResponse(val recommendedQuizIds: List<String>)
