package com.toeic.backend.quizzes

import kotlinx.serialization.Serializable

@Serializable
data class CreateQuizDto(
    val title: String,
    val description: String? = null,
    val timeLimitMinutes: Int? = null
)

@Serializable
data class UpdateQuizDto(
    val title: String? = null,
    val description: String? = null,
    val timeLimitMinutes: Int? = null
)

@Serializable
data class CreateQuestionDto(
    val prompt: String,
    val order: Int,
    val points: Double,
    val options: List<String>,
    val correctAnswer: String
)

@Serializable
data class UpdateQuestionDto(
    val prompt: String? = null,
    val order: Int? = null,
    val points: Double? = null,
    val options: List<String>? = null,
    val correctAnswer: String? = null
)

@Serializable
data class QuizResponse(
    val id: String,
    val title: String,
    val description: String?,
    val timeLimitMinutes: Int?,
    val teacherId: String,
    val createdAt: String,
    val questions: List<QuestionResponse>? = null
)

@Serializable
data class QuestionResponse(
    val id: String,
    val quizId: String,
    val prompt: String,
    val order: Int,
    val points: Double,
    val options: List<String>,
    val correctAnswer: String
)
