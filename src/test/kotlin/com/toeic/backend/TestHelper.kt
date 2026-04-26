package com.toeic.backend

import at.favre.lib.crypto.bcrypt.BCrypt
import com.toeic.backend.auth.AuthService
import com.toeic.backend.auth.JwtService
import com.toeic.backend.classes.ClassRepository
import com.toeic.backend.classes.ClassService
import com.toeic.backend.classes.ClassesTable
import com.toeic.backend.enrollments.EnrollmentRepository
import com.toeic.backend.enrollments.EnrollmentsTable
import com.toeic.backend.plugins.*
import com.toeic.backend.quizzes.QuizRepository
import com.toeic.backend.quizzes.QuizService
import com.toeic.backend.quizzes.QuizzesTable
import com.toeic.backend.quizzes.QuestionsTable
import com.toeic.backend.quizzes.QuizClassesTable
import com.toeic.backend.recommendations.RecommendationService
import com.toeic.backend.submissions.SubmissionDispatcher
import com.toeic.backend.submissions.SubmissionRepository
import com.toeic.backend.submissions.SubmissionService
import com.toeic.backend.submissions.SubmissionsTable
import com.toeic.backend.users.UserRepository
import com.toeic.backend.users.UsersTable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

private val dbCounter = AtomicInteger(0)

const val TEACHER_ID = "teacher-01"
const val STUDENT_ID = "student-01"
const val STUDENT2_ID = "student-02"
const val TEST_PASSWORD = "password123"
const val QUIZ_ID = "quiz-01"
const val UNENROLLED_QUIZ_ID = "quiz-02"

fun ApplicationTestBuilder.configureTestApp(
    aiClient: FakeAiClient = FakeAiClient(),
    seedQuizData: Boolean = false
): HttpClient {
    application {
        testModule(aiClient, seedQuizData)
    }
    return createClient {
        install(ContentNegotiation) { json() }
    }
}

fun Application.testModule(aiClient: FakeAiClient = FakeAiClient(), seedQuizData: Boolean = false) {
    configureSerialization()
    configureStatusPages()

    val jwtService = JwtService()
    configureAuthentication(jwtService)

    initTestDatabase(seedQuizData)

    val userRepository = UserRepository()
    val classRepository = ClassRepository()
    val enrollmentRepository = EnrollmentRepository()
    val quizRepository = QuizRepository()

    val authService = AuthService(userRepository, jwtService)
    val classService = ClassService(classRepository, enrollmentRepository)
    val quizService = QuizService(quizRepository)

    val submissionRepository = SubmissionRepository()
    val submissionDispatcher = SubmissionDispatcher(submissionRepository, aiClient)
    val submissionService = SubmissionService(
        quizRepository, submissionRepository, enrollmentRepository, submissionDispatcher
    )
    val recommendationService = RecommendationService(aiClient, quizRepository)

    configureRouting(authService, classService, quizService, submissionService, recommendationService)
}

fun initTestDatabase(seedQuizData: Boolean = false) {
    val dbName = "test_${dbCounter.incrementAndGet()}"
    val db = Database.connect("jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", driver = "org.h2.Driver")
    com.toeic.backend.db.DatabaseFactory.database = db
    val passwordHash = BCrypt.withDefaults().hashToString(12, TEST_PASSWORD.toCharArray())
    transaction(db) {
        SchemaUtils.create(UsersTable, ClassesTable, EnrollmentsTable, QuizzesTable, QuestionsTable, SubmissionsTable, QuizClassesTable)
        seedTestUsers(passwordHash)
        if (seedQuizData) seedTestQuizData()
    }
}

private fun seedTestUsers(passwordHash: String) {
    UsersTable.insert {
        it[id] = TEACHER_ID
        it[fullName] = "Mondher Ben Ali"
        it[email] = "mondher@thee.tn"
        it[UsersTable.passwordHash] = passwordHash
        it[role] = "teacher"
    }

    UsersTable.insert {
        it[id] = STUDENT_ID
        it[fullName] = "Yassine Kaibi"
        it[email] = "yassine@thee.tn"
        it[UsersTable.passwordHash] = passwordHash
        it[role] = "student"
    }

    UsersTable.insert {
        it[id] = STUDENT2_ID
        it[fullName] = "Amira Trabelsi"
        it[email] = "amira@thee.tn"
        it[UsersTable.passwordHash] = passwordHash
        it[role] = "student"
    }
}

private fun seedTestQuizData() {
    val now = Clock.System.now()

    ClassesTable.insert {
        it[id] = "class-01"
        it[name] = "Sprint 2 Demo"
        it[teacherId] = TEACHER_ID
        it[joinCode] = "DEMO01"
        it[createdAt] = now
    }

    EnrollmentsTable.insert {
        it[id] = UUID.randomUUID().toString()
        it[classId] = "class-01"
        it[studentId] = STUDENT_ID
        it[joinedAt] = now
    }

    EnrollmentsTable.insert {
        it[id] = UUID.randomUUID().toString()
        it[classId] = "class-01"
        it[studentId] = STUDENT2_ID
        it[joinedAt] = now
    }

    QuizzesTable.insert {
        it[id] = QUIZ_ID
        it[title] = "Demo Listening"
        it[description] = null
        it[timeLimitMinutes] = null
        it[teacherId] = TEACHER_ID
        it[archived] = false
        it[createdAt] = now
        it[updatedAt] = now
    }

    QuestionsTable.insert {
        it[id] = "q-01"
        it[quizId] = QUIZ_ID
        it[prompt] = "Question 1"
        it[order] = 1
        it[points] = 1.0
        it[options] = Json.encodeToString(listOf("A", "B", "C", "D"))
        it[correctAnswer] = "A"
    }

    QuestionsTable.insert {
        it[id] = "q-02"
        it[quizId] = QUIZ_ID
        it[prompt] = "Question 2"
        it[order] = 2
        it[points] = 1.0
        it[options] = Json.encodeToString(listOf("A", "B", "C", "D"))
        it[correctAnswer] = "B"
    }

    QuizClassesTable.insert {
        it[quizId] = QUIZ_ID
        it[classId] = "class-01"
    }

    // quiz-02 has no quiz_classes row — used to test NOT_ENROLLED_IN_QUIZ_CLASS
    QuizzesTable.insert {
        it[id] = UNENROLLED_QUIZ_ID
        it[title] = "Unenrolled Quiz"
        it[description] = null
        it[timeLimitMinutes] = null
        it[teacherId] = TEACHER_ID
        it[archived] = false
        it[createdAt] = now
        it[updatedAt] = now
    }
}

suspend fun HttpClient.login(email: String, password: String = TEST_PASSWORD): String {
    val response = post("/api/v1/auth/login") {
        header(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
        setBody(mapOf("email" to email, "password" to password))
    }
    val body = response.body<kotlinx.serialization.json.JsonObject>()
    return body["accessToken"]!!.jsonPrimitive.content
}

fun HttpRequestBuilder.withAuth(token: String) {
    header("Authorization", "Bearer $token")
}
