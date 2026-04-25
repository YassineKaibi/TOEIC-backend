package com.toeic.backend.ai

import com.toeic.backend.common.BadGatewayException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class KtorAiClient : AiClient {
    private val baseUrl = System.getenv("AI_BASE_URL") ?: "http://localhost:9000"
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun postQuizResult(payload: AiQuizResultRequest): AiQuizResultResponse {
        return try {
            val response = client.post("$baseUrl/ai/quiz/result") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            if (response.status.value in 200..299) {
                response.body()
            } else {
                throw BadGatewayException("AI service error: HTTP ${response.status.value}", "AI_UNAVAILABLE")
            }
        } catch (e: BadGatewayException) {
            throw e
        } catch (e: Exception) {
            throw BadGatewayException("AI service error: ${e.message}", "AI_UNAVAILABLE")
        }
    }

    override suspend fun getRecommendations(studentId: String): AiRecommendationsResponse {
        return try {
            val response = client.get("$baseUrl/ai/recommendations/$studentId")
            if (response.status.value in 200..299) {
                response.body()
            } else {
                throw BadGatewayException("AI service error: HTTP ${response.status.value}", "AI_UNAVAILABLE")
            }
        } catch (e: BadGatewayException) {
            throw e
        } catch (e: Exception) {
            throw BadGatewayException("AI service error: ${e.message}", "AI_UNAVAILABLE")
        }
    }

    override fun close() {
        client.close()
    }
}
