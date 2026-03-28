package com.toeic.backend

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ClassRoutesTest {

    @Test
    fun `teacher can create a class`() = testApplication {
        val client = configureTestApp()
        val token = client.login("mondher@thee.tn")

        val response = client.post("/api/v1/classes") {
            contentType(ContentType.Application.Json)
            withAuth(token)
            setBody(mapOf("name" to "English 101", "description" to "Beginner TOEIC prep"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<JsonObject>()
        assertNotNull(body["id"])
        assertEquals("English 101", body["name"]?.jsonPrimitive?.content)
        assertNotNull(body["joinCode"])
    }

    @Test
    fun `student cannot create a class`() = testApplication {
        val client = configureTestApp()
        val token = client.login("yassine@thee.tn")

        val response = client.post("/api/v1/classes") {
            contentType(ContentType.Application.Json)
            withAuth(token)
            setBody(mapOf("name" to "Hacking 101"))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `student can join a class with valid code`() = testApplication {
        val client = configureTestApp()
        val teacherToken = client.login("mondher@thee.tn")
        val studentToken = client.login("yassine@thee.tn")

        // Teacher creates a class
        val createResponse = client.post("/api/v1/classes") {
            contentType(ContentType.Application.Json)
            withAuth(teacherToken)
            setBody(mapOf("name" to "Join Test Class"))
        }
        val joinCode = createResponse.body<JsonObject>()["joinCode"]!!.jsonPrimitive.content

        // Student joins
        val joinResponse = client.post("/api/v1/classes/join") {
            contentType(ContentType.Application.Json)
            withAuth(studentToken)
            setBody(mapOf("code" to joinCode))
        }

        assertEquals(HttpStatusCode.OK, joinResponse.status)
    }

    @Test
    fun `joining with invalid code returns 400`() = testApplication {
        val client = configureTestApp()
        val token = client.login("yassine@thee.tn")

        val response = client.post("/api/v1/classes/join") {
            contentType(ContentType.Application.Json)
            withAuth(token)
            setBody(mapOf("code" to "BADCODE"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.body<JsonObject>()
        assertEquals("WRONG_CODE", body["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun `joining same class twice returns 409`() = testApplication {
        val client = configureTestApp()
        val teacherToken = client.login("mondher@thee.tn")
        val studentToken = client.login("yassine@thee.tn")

        val createResponse = client.post("/api/v1/classes") {
            contentType(ContentType.Application.Json)
            withAuth(teacherToken)
            setBody(mapOf("name" to "Duplicate Join Test"))
        }
        val joinCode = createResponse.body<JsonObject>()["joinCode"]!!.jsonPrimitive.content

        // Join once
        client.post("/api/v1/classes/join") {
            contentType(ContentType.Application.Json)
            withAuth(studentToken)
            setBody(mapOf("code" to joinCode))
        }

        // Join again
        val response = client.post("/api/v1/classes/join") {
            contentType(ContentType.Application.Json)
            withAuth(studentToken)
            setBody(mapOf("code" to joinCode))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("ALREADY_JOINED", response.body<JsonObject>()["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun `teacher can list their classes`() = testApplication {
        val client = configureTestApp()
        val token = client.login("mondher@thee.tn")

        // Create two classes
        repeat(2) { i ->
            client.post("/api/v1/classes") {
                contentType(ContentType.Application.Json)
                withAuth(token)
                setBody(mapOf("name" to "Class $i"))
            }
        }

        val response = client.get("/api/v1/teachers/me/classes") {
            withAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<JsonObject>()
        val items = body["items"]!!.jsonArray
        assertEquals(2, items.size)
        assertNotNull(items[0].jsonObject["studentsCount"])
    }

    @Test
    fun `student can list their enrolled classes`() = testApplication {
        val client = configureTestApp()
        val teacherToken = client.login("mondher@thee.tn")
        val studentToken = client.login("yassine@thee.tn")

        // Teacher creates, student joins
        val createResponse = client.post("/api/v1/classes") {
            contentType(ContentType.Application.Json)
            withAuth(teacherToken)
            setBody(mapOf("name" to "Student List Test"))
        }
        val joinCode = createResponse.body<JsonObject>()["joinCode"]!!.jsonPrimitive.content

        client.post("/api/v1/classes/join") {
            contentType(ContentType.Application.Json)
            withAuth(studentToken)
            setBody(mapOf("code" to joinCode))
        }

        val response = client.get("/api/v1/students/me/classes") {
            withAuth(studentToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val items = response.body<JsonObject>()["items"]!!.jsonArray
        assertEquals(1, items.size)
        assertNotNull(items[0].jsonObject["teacher"])
    }

    @Test
    fun `teacher can view and remove students from their class`() = testApplication {
        val client = configureTestApp()
	
		val teacherToken = client.login("mondher@thee.tn")

		val studentToken1 = client.login("yassine@thee.tn")
		val studentToken2 = client.login("amira@thee.tn")

		val createResponse = client.post("/api/v1/classes") {
			contentType(ContentType.Application.Json)
			withAuth(teacherToken)
			setBody(mapOf("name" to "Student List Test"))
		}

		val joinCode = createResponse.body<JsonObject>()["joinCode"]!!.jsonPrimitive.content
		val classId = createResponse.body<JsonObject>()["id"]!!.jsonPrimitive.content

	    client.post("/api/v1/classes/join") {
			contentType(ContentType.Application.Json)
	        withAuth(studentToken1)
	        setBody(mapOf("code" to joinCode))
	    }
	    
	    client.post("/api/v1/classes/join") {
	    	contentType(ContentType.Application.Json)
	    	withAuth(studentToken2)
	    	setBody(mapOf("code" to joinCode))
	    }
		
        val response = client.get("/api/v1/classes/$classId/students"){
        	withAuth(teacherToken)
        }

		assertEquals(HttpStatusCode.OK, response.status)
		val items = response.body<JsonObject>()["items"]!!.jsonArray
		assertEquals(2, items.size)

	    val removeResponse = client.delete("/api/v1/classes/$classId/students/$STUDENT_ID") {                                                                                                   
	          withAuth(teacherToken)
	      }                                                                                                                                                                                       
	      assertEquals(HttpStatusCode.NoContent, removeResponse.status)

		val afterResponse = client.get("/api/v1/classes/$classId/students") {                                                                                                                   
		      withAuth(teacherToken)                                                                                                                                                              
		  }
		  assertEquals(HttpStatusCode.OK, afterResponse.status)                                                                                                                                   
		  val remainingItems = afterResponse.body<JsonObject>()["items"]!!.jsonArray
		  assertEquals(1, remainingItems.size)
    }

    @Test
    fun `student can leave a class they joined`() = testApplication {
        val client = configureTestApp()
        val teacherToken = client.login("mondher@thee.tn")
        val studentToken = client.login("yassine@thee.tn")

        // Teacher creates a class
        val createResponse = client.post("/api/v1/classes") {
            contentType(ContentType.Application.Json)
            withAuth(teacherToken)
            setBody(mapOf("name" to "Leave Test Class"))
        }
        val joinCode = createResponse.body<JsonObject>()["joinCode"]!!.jsonPrimitive.content
        val classId = createResponse.body<JsonObject>()["id"]!!.jsonPrimitive.content

        // Student joins
        client.post("/api/v1/classes/join") {
            contentType(ContentType.Application.Json)
            withAuth(studentToken)
            setBody(mapOf("code" to joinCode))
        }

        // Student leaves
        val leaveResponse = client.delete("/api/v1/students/me/classes/$classId") {
            withAuth(studentToken)
        }
        assertEquals(HttpStatusCode.NoContent, leaveResponse.status)

        // Student's class list should be empty
        val classesResponse = client.get("/api/v1/students/me/classes") {
            withAuth(studentToken)
        }
        assertEquals(HttpStatusCode.NoContent, classesResponse.status)
    }

    @Test
    fun `leaving a class without being enrolled returns 403`() = testApplication {
        val client = configureTestApp()
        val teacherToken = client.login("mondher@thee.tn")
        val studentToken = client.login("yassine@thee.tn")

        // Teacher creates a class
        val createResponse = client.post("/api/v1/classes") {
            contentType(ContentType.Application.Json)
            withAuth(teacherToken)
            setBody(mapOf("name" to "Not Enrolled Class"))
        }
        val classId = createResponse.body<JsonObject>()["id"]!!.jsonPrimitive.content

        // Student tries to leave without joining
        val response = client.delete("/api/v1/students/me/classes/$classId") {
            withAuth(studentToken)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("NOT_ENROLLED", response.body<JsonObject>()["errorCode"]?.jsonPrimitive?.content)
    }
}
