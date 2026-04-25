package com.toeic.backend

import com.toeic.backend.ai.AiRecommendationsResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class RecommendationRoutesTest {

    @Test
    fun recommendations_returns_200_with_mapped_quizzes_in_AI_order() = testApplication {
        val aiClient = FakeAiClient(recommendationFn = {
            AiRecommendationsResponse(recommendedQuizIds = listOf(QUIZ_ID))
        })
        val client = configureTestApp(aiClient, seedQuizData = true)
        val studentToken = client.login("yassine@thee.tn")

        val response = client.get("/api/v1/students/$STUDENT_ID/recommendations") {
            withAuth(studentToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val quizzes = response.body<JsonObject>()["quizzes"]!!.jsonArray
        assertEquals(1, quizzes.size)
        assertEquals(QUIZ_ID, quizzes[0].jsonObject["quizId"]?.jsonPrimitive?.content)
        assertEquals("Demo Listening", quizzes[0].jsonObject["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun recommendations_skips_unknown_quiz_ids_returned_by_AI() = testApplication {
        val aiClient = FakeAiClient(recommendationFn = {
            AiRecommendationsResponse(recommendedQuizIds = listOf(QUIZ_ID, "nonexistent-quiz"))
        })
        val client = configureTestApp(aiClient, seedQuizData = true)
        val studentToken = client.login("yassine@thee.tn")

        val response = client.get("/api/v1/students/$STUDENT_ID/recommendations") {
            withAuth(studentToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val quizzes = response.body<JsonObject>()["quizzes"]!!.jsonArray
        assertEquals(1, quizzes.size)
        assertEquals(QUIZ_ID, quizzes[0].jsonObject["quizId"]?.jsonPrimitive?.content)
    }

    @Test
    fun recommendations_returns_200_with_empty_list_when_AI_returns_none() = testApplication {
        val aiClient = FakeAiClient()
        val client = configureTestApp(aiClient)
        
        val studentToken = client.login("yassine@thee.tn")
        
        val response = client.get("/api/v1/students/$STUDENT_ID/recommendations") {
            withAuth(studentToken)
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<JsonObject>()
        val quizzes = body["quizzes"]
        assertEquals("[]", quizzes.toString())
    }

    @Test
    fun recommendations_returns_403_when_path_studentId_differs_from_jwt() = testApplication {
        val client = configureTestApp()
        
        val studentToken = client.login("yassine@thee.tn")
        
        val response = client.get("/api/v1/students/$STUDENT2_ID/recommendations") {
            withAuth(studentToken)
        }
        
        assertEquals(HttpStatusCode.Forbidden, response.status)
        val body = response.body<JsonObject>()
        assertEquals("STUDENT_MISMATCH", body["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun recommendations_returns_403_when_caller_is_teacher() = testApplication {
        val client = configureTestApp()
        
        val teacherToken = client.login("mondher@thee.tn")
        
        val response = client.get("/api/v1/students/$STUDENT_ID/recommendations") {
            withAuth(teacherToken)
        }
        
        assertEquals(HttpStatusCode.Forbidden, response.status)
        val body = response.body<JsonObject>()
        assertEquals("STUDENT_ROLE_REQUIRED", body["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun recommendations_returns_502_when_AI_unavailable() = testApplication {
        val aiClient = FakeAiClient(mode = FakeAiMode.FAILURE)
        val client = configureTestApp(aiClient)
        
        val studentToken = client.login("yassine@thee.tn")
        
        val response = client.get("/api/v1/students/$STUDENT_ID/recommendations") {
            withAuth(studentToken)
        }
        
        assertEquals(HttpStatusCode.BadGateway, response.status)
        val body = response.body<JsonObject>()
        assertEquals("AI_UNAVAILABLE", body["errorCode"]?.jsonPrimitive?.content)
    }
}
