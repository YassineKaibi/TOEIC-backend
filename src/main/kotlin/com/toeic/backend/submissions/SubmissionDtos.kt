package com.toeic.backend.submissions

import kotlinx.serialization.Serializable

@Serializable
data class StudentAnswerDto(val questionId: String, val selectedOption: String)

@Serializable
data class QuizSubmissionDto(
    val studentId: String,
    val durationSeconds: Int,
    val answers: List<StudentAnswerDto>
)

@Serializable
data class QuizSubmissionResponse(val submissionId: String, val status: String)

// Internal — used by the service to pass the computed result around
data class GradedSubmission(
    val submissionId: String,
    val quizId: String,
    val studentId: String,
    val score: Double,
    val maxScore: Double,
    val correctCount: Int,
    val totalCount: Int,
    val durationSeconds: Int,
    val answers: List<StudentAnswerDto>,
    val gradedEntries: List<com.toeic.backend.ai.AiAnswerEntry>,
    val submittedAt: kotlinx.datetime.Instant
)
