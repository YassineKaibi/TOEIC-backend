package com.toeic.backend.recommendations

import kotlinx.serialization.Serializable

@Serializable
data class QuizRecommendation(
    val quizId: String,
    val title: String,
    val difficulty: String? = null
)

@Serializable
data class RecommendationsResponse(val quizzes: List<QuizRecommendation>)
