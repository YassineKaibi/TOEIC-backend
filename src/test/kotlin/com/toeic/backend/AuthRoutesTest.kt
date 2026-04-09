package com.toeic.backend

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthRoutesTest {
    
    @Test
    fun `login with valid credentials returns token and user info`() = testApplication {
        val client = configureTestApp()

        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to "mondher@thee.tn", "password" to TEST_PASSWORD))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<JsonObject>()
        assertNotNull(body["accessToken"])
        val user = body["user"] as JsonObject
        assertEquals("teacher", user["role"]?.jsonPrimitive?.content)
        assertEquals("Mondher Ben Ali", user["fullName"]?.jsonPrimitive?.content)
    }

    @Test
    fun `login with wrong password returns 401`() = testApplication {
        val client = configureTestApp()

        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to "mondher@thee.tn", "password" to "wrongpass"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = response.body<JsonObject>()
        assertEquals("INVALID_CREDENTIALS", body["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun `login with unknown email returns 401`() = testApplication {
        val client = configureTestApp()

        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to "nobody@thee.tn", "password" to TEST_PASSWORD))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `accessing protected route without token returns 401`() = testApplication {
        val client = configureTestApp()

        val response = client.get("/api/v1/teachers/me/classes")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `register with valid data returns token and user info`() = testApplication {
        val client = configureTestApp()

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "fullName" to "New User",
                "email" to "newuser@thee.tn",
                "password" to "securepass",
                "role" to "student"
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<JsonObject>()
        assertNotNull(body["accessToken"])
        val user = body["user"] as JsonObject
        assertEquals("New User", user["fullName"]?.jsonPrimitive?.content)
        assertEquals("newuser@thee.tn", user["email"]?.jsonPrimitive?.content)
        assertEquals("student", user["role"]?.jsonPrimitive?.content)
    }

    @Test
    fun `register with duplicate email returns 409`() = testApplication {
        val client = configureTestApp()

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "fullName" to "Duplicate",
                "email" to "mondher@thee.tn",
                "password" to "securepass",
                "role" to "teacher"
            ))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("EMAIL_ALREADY_EXISTS", response.body<JsonObject>()["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun `register with invalid role returns 400`() = testApplication {
        val client = configureTestApp()

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "fullName" to "Bad Role",
                "email" to "badrole@thee.tn",
                "password" to "securepass",
                "role" to "admin"
            ))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("INVALID_ROLE", response.body<JsonObject>()["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun `registered user can use token to access protected route`() = testApplication {
        val client = configureTestApp()

        val registerResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "fullName" to "Fresh Teacher",
                "email" to "fresh@thee.tn",
                "password" to "securepass",
                "role" to "teacher"
            ))
        }
        val token = registerResponse.body<JsonObject>()["accessToken"]!!.jsonPrimitive.content

        val classesResponse = client.get("/api/v1/teachers/me/classes") {
            withAuth(token)
        }

        assertEquals(HttpStatusCode.NoContent, classesResponse.status)
    }
}
