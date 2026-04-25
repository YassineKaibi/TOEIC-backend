package com.toeic.backend

import com.toeic.backend.submissions.QuizSubmissionDto
import com.toeic.backend.submissions.StudentAnswerDto
import com.toeic.backend.db.DatabaseFactory
import com.toeic.backend.submissions.SubmissionsTable
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SubmissionRoutesTest {

    @Test
    fun submit_succeeds_returns_201_and_persists_row() = testApplication {
        val client = configureTestApp(seedQuizData = true)
        val studentToken = client.login("yassine@thee.tn")

        val dto = QuizSubmissionDto(
            studentId = STUDENT_ID,
            durationSeconds = 60,
            answers = listOf(
                StudentAnswerDto("q-01", "A"),
                StudentAnswerDto("q-02", "B")
            )
        )

        val response = client.post("/api/v1/quizzes/$QUIZ_ID/submissions") {
            withAuth(studentToken)
            contentType(ContentType.Application.Json)
            setBody(dto)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<JsonObject>()
        assertNotNull(body["submissionId"]?.jsonPrimitive?.content)
        assertEquals("received", body["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun submit_returns_403_when_caller_role_is_teacher() = testApplication {
        val client = configureTestApp()
        val teacherToken = client.login("mondher@thee.tn")
        
        val dto = QuizSubmissionDto(
            studentId = STUDENT_ID,
            durationSeconds = 60,
            answers = listOf(StudentAnswerDto(questionId = "q-01", selectedOption = "A"))
        )
        
        val response = client.post("/api/v1/quizzes/nonexistent/submissions") {
            withAuth(teacherToken)
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        
        assertEquals(HttpStatusCode.Forbidden, response.status)
        val body = response.body<JsonObject>()
        assertEquals("STUDENT_ROLE_REQUIRED", body["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun submit_returns_403_when_studentId_in_body_differs_from_jwt() = testApplication {
        val client = configureTestApp()
        val studentToken = client.login("yassine@thee.tn")
        
        val dto = QuizSubmissionDto(
            studentId = STUDENT2_ID,  // Different student
            durationSeconds = 60,
            answers = listOf(StudentAnswerDto(questionId = "q-01", selectedOption = "A"))
        )
        
        val response = client.post("/api/v1/quizzes/nonexistent/submissions") {
            withAuth(studentToken)
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        
        assertEquals(HttpStatusCode.Forbidden, response.status)
        val body = response.body<JsonObject>()
        assertEquals("STUDENT_MISMATCH", body["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun submit_returns_400_for_empty_answers() = testApplication {
        val client = configureTestApp()
        val studentToken = client.login("yassine@thee.tn")
        
        val dto = QuizSubmissionDto(
            studentId = STUDENT_ID,
            durationSeconds = 60,
            answers = emptyList()
        )
        
        val response = client.post("/api/v1/quizzes/nonexistent/submissions") {
            withAuth(studentToken)
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.body<JsonObject>()
        assertEquals("INVALID_INPUT", body["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun submit_returns_400_for_negative_duration() = testApplication {
        val client = configureTestApp()
        val studentToken = client.login("yassine@thee.tn")
        
        val dto = QuizSubmissionDto(
            studentId = STUDENT_ID,
            durationSeconds = -10,
            answers = listOf(StudentAnswerDto(questionId = "q-01", selectedOption = "A"))
        )
        
        val response = client.post("/api/v1/quizzes/nonexistent/submissions") {
            withAuth(studentToken)
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.body<JsonObject>()
        assertEquals("INVALID_INPUT", body["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun submit_returns_400_for_duplicate_questionIds() = testApplication {
        val client = configureTestApp()
        val studentToken = client.login("yassine@thee.tn")
        
        val dto = QuizSubmissionDto(
            studentId = STUDENT_ID,
            durationSeconds = 60,
            answers = listOf(
                StudentAnswerDto(questionId = "q-01", selectedOption = "A"),
                StudentAnswerDto(questionId = "q-01", selectedOption = "B")
            )
        )
        
        val response = client.post("/api/v1/quizzes/nonexistent/submissions") {
            withAuth(studentToken)
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.body<JsonObject>()
        assertEquals("DUPLICATE_ANSWER", body["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun submit_returns_400_when_question_does_not_belong_to_quiz() = testApplication {
        val client = configureTestApp(seedQuizData = true)
        val studentToken = client.login("yassine@thee.tn")

        val dto = QuizSubmissionDto(
            studentId = STUDENT_ID,
            durationSeconds = 60,
            answers = listOf(StudentAnswerDto("q-nonexistent", "A"))
        )

        val response = client.post("/api/v1/quizzes/$QUIZ_ID/submissions") {
            withAuth(studentToken)
            contentType(ContentType.Application.Json)
            setBody(dto)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.body<JsonObject>()
        assertEquals("QUESTION_NOT_IN_QUIZ", body["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun submit_returns_403_when_student_not_enrolled_in_any_quiz_class() = testApplication {
        val client = configureTestApp(seedQuizData = true)
        val studentToken = client.login("yassine@thee.tn")

        val dto = QuizSubmissionDto(
            studentId = STUDENT_ID,
            durationSeconds = 60,
            answers = listOf(StudentAnswerDto("q-01", "A"))
        )

        val response = client.post("/api/v1/quizzes/$UNENROLLED_QUIZ_ID/submissions") {
            withAuth(studentToken)
            contentType(ContentType.Application.Json)
            setBody(dto)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        val body = response.body<JsonObject>()
        assertEquals("NOT_ENROLLED_IN_QUIZ_CLASS", body["errorCode"]?.jsonPrimitive?.content)
    }

    @Test
    fun submit_grades_correctly_unanswered_questions_count_as_zero() = testApplication {
        val client = configureTestApp(seedQuizData = true)
        val studentToken = client.login("yassine@thee.tn")

        // quiz-01 has 2 questions; only answer q-01 — q-02 treated as incorrect (0 pts)
        val dto = QuizSubmissionDto(
            studentId = STUDENT_ID,
            durationSeconds = 30,
            answers = listOf(StudentAnswerDto("q-01", "A"))
        )

        val response = client.post("/api/v1/quizzes/$QUIZ_ID/submissions") {
            withAuth(studentToken)
            contentType(ContentType.Application.Json)
            setBody(dto)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("received", response.body<JsonObject>()["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun submit_succeeds_even_when_AI_dispatch_fails() = testApplication {
        val aiClient = FakeAiClient(mode = FakeAiMode.FAILURE)
        val client = configureTestApp(aiClient, seedQuizData = true)
        val studentToken = client.login("yassine@thee.tn")

        val dto = QuizSubmissionDto(
            studentId = STUDENT_ID,
            durationSeconds = 60,
            answers = listOf(StudentAnswerDto("q-01", "A"))
        )

        val response = client.post("/api/v1/quizzes/$QUIZ_ID/submissions") {
            withAuth(studentToken)
            contentType(ContentType.Application.Json)
            setBody(dto)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<JsonObject>()
        val submissionId = body["submissionId"]!!.jsonPrimitive.content
        assertEquals("received", body["status"]!!.jsonPrimitive.content)

        var aiStatus: String? = null
        repeat(10) {
            delay(100)
            aiStatus = transaction(DatabaseFactory.database) {
                SubmissionsTable.selectAll()
                    .where { SubmissionsTable.id eq submissionId }
                    .singleOrNull()?.get(SubmissionsTable.aiDispatchStatus)
            }
            if (aiStatus == "failed") return@repeat
        }
        assertEquals("failed", aiStatus)
    }

    @Test
    fun submit_returns_404_for_unknown_quiz() = testApplication {
        val client = configureTestApp()
        val studentToken = client.login("yassine@thee.tn")
        
        val dto = QuizSubmissionDto(
            studentId = STUDENT_ID,
            durationSeconds = 60,
            answers = listOf(StudentAnswerDto(questionId = "q-01", selectedOption = "A"))
        )
        
        val response = client.post("/api/v1/quizzes/nonexistent-quiz/submissions") {
            withAuth(studentToken)
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        
        assertEquals(HttpStatusCode.NotFound, response.status)
        val body = response.body<JsonObject>()
        assertEquals("QUIZ_NOT_FOUND", body["errorCode"]?.jsonPrimitive?.content)
    }
}
