package com.toeic.backend.recommendations

import com.toeic.backend.ai.AiClient
import com.toeic.backend.quizzes.QuizRepository
import org.slf4j.LoggerFactory

class RecommendationService(
    private val aiClient: AiClient,
    private val quizRepository: QuizRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getFor(studentId: String): RecommendationsResponse {
        val ids = aiClient.getRecommendations(studentId).recommendedQuizIds
        if (ids.isEmpty()) return RecommendationsResponse(emptyList())
        val quizzes = quizRepository.getQuizzesByIds(ids)
        val foundIds = quizzes.map { it.id }.toSet()
        ids.filterNot { it in foundIds }.forEach { logger.warn("AI recommended unknown quiz id: $it") }
        // TODO(difficulty): once Quiz has a difficulty field, populate it here.
        return RecommendationsResponse(
            quizzes.map { QuizRecommendation(quizId = it.id, title = it.title, difficulty = null) }
        )
    }
}
