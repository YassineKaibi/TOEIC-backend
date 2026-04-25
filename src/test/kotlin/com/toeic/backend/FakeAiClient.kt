package com.toeic.backend

import com.toeic.backend.ai.AiClient
import com.toeic.backend.ai.AiQuizResultRequest
import com.toeic.backend.ai.AiQuizResultResponse
import com.toeic.backend.ai.AiRecommendationsResponse
import com.toeic.backend.common.BadGatewayException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeAiClient(
    private val mode: FakeAiMode = FakeAiMode.SUCCESS,
    private val delayMs: Long = 0,
    private val recommendationFn: (() -> AiRecommendationsResponse)? = null
) : AiClient {
    val callOrder = mutableListOf<String>()
    private val mutex = Mutex()

    override suspend fun postQuizResult(payload: AiQuizResultRequest): AiQuizResultResponse {
        if (delayMs > 0) delay(delayMs)
        mutex.withLock {
            callOrder.add(payload.submissionId)
        }
        return when (mode) {
            FakeAiMode.SUCCESS -> AiQuizResultResponse(status = "received")
            FakeAiMode.FAILURE -> throw BadGatewayException("Mock AI service failed", "AI_UNAVAILABLE")
        }
    }

    override suspend fun getRecommendations(studentId: String): AiRecommendationsResponse {
        if (delayMs > 0) delay(delayMs)
        return when (mode) {
            FakeAiMode.SUCCESS -> recommendationFn?.invoke() ?: AiRecommendationsResponse(recommendedQuizIds = emptyList())
            FakeAiMode.FAILURE -> throw BadGatewayException("Mock AI service failed", "AI_UNAVAILABLE")
        }
    }

    override fun close() {
        // no-op
    }
}

enum class FakeAiMode {
    SUCCESS, FAILURE
}
