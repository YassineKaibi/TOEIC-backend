package com.toeic.backend

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class QuizRoutesTest {

    @Test
    fun `teacher can create a quiz`() = testApplication {
        val client = configureTestApp()
        val token = client.login("mondher@thee.tn")

        val response = client.post("/api/v1/teachers/me/quizzes") {
            contentType(ContentType.Application.Json)
            withAuth(token)
            setBody(mapOf("title" to "Listening Quiz 1"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<JsonObject>()
        assertNotNull(body["id"])
        assertEquals("Listening Quiz 1", body["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun `teacher can list their quizzes`() = testApplication {
        val client = configureTestApp(seedQuizData = true)
        val token = client.login("mondher@thee.tn")

        val response = client.get("/api/v1/teachers/me/quizzes") {
            withAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<JsonObject>()
        assertNotNull(body["items"])
        assertNotNull(body["total"])
    }

    @Test
    fun `teacher quiz list is paginated`() = testApplication {
        val client = configureTestApp(seedQuizData = true)
        val token = client.login("mondher@thee.tn")

        // seed already has quiz-01 and quiz-02 (2 non-archived); create 2 more = 4 total
        repeat(2) { i ->
            client.post("/api/v1/teachers/me/quizzes") {
                contentType(ContentType.Application.Json)
                withAuth(token)
                setBody(mapOf("title" to "Extra Quiz $i"))
            }
        }

        val response = client.get("/api/v1/teachers/me/quizzes?page=1&pageSize=2") {
            withAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<JsonObject>()
        assertEquals(2, body["items"]?.jsonArray?.size)
        assertEquals(4, body["total"]?.jsonPrimitive?.int)
        assertEquals(1, body["page"]?.jsonPrimitive?.int)
        assertEquals(2, body["pageSize"]?.jsonPrimitive?.int)
    }
}
