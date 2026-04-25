package com.toeic.backend.db

import at.favre.lib.crypto.bcrypt.BCrypt
import com.toeic.backend.classes.ClassesTable
import com.toeic.backend.enrollments.EnrollmentsTable
import com.toeic.backend.quizzes.QuestionsTable
import com.toeic.backend.quizzes.QuizzesTable
import com.toeic.backend.quizzes.QuizClassesTable
import com.toeic.backend.submissions.SubmissionsTable
import com.toeic.backend.users.UsersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {

    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    lateinit var database: Database

    fun init() {
        val host = System.getenv("POSTGRES_HOST") ?: "localhost"
        val port = System.getenv("POSTGRES_PORT") ?: "5432"
        val db = System.getenv("POSTGRES_DB") ?: "toeic_backend"
        val url = "jdbc:postgresql://$host:$port/$db"
        val user = System.getenv("POSTGRES_USER") ?: "thee"
        val password = System.getenv("POSTGRES_PASSWORD") ?: ""

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = url
            driverClassName = "org.postgresql.Driver"
            username = user
            this.password = password
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        database = Database.connect(HikariDataSource(hikariConfig))

        transaction(database) {
            SchemaUtils.create(UsersTable, ClassesTable, EnrollmentsTable, QuizzesTable, QuestionsTable, SubmissionsTable, QuizClassesTable)
            seedDevData()
        }
    }

    private fun seedDevData() {
        val hasUsers = UsersTable.selectAll().count() > 0
        if (hasUsers) return

        logger.info("Seeding dev data...")

        val hash = { pwd: String ->
            BCrypt.withDefaults().hashToString(12, pwd.toCharArray())
        }

        UsersTable.insert {
            it[id] = "teacher-01"
            it[fullName] = "Mondher Ben Ali"
            it[email] = "mondher@thee.tn"
            it[passwordHash] = hash("password123")
            it[role] = "teacher"
        }

        UsersTable.insert {
            it[id] = "student-01"
            it[fullName] = "Yassine Kaibi"
            it[email] = "yassine@thee.tn"
            it[passwordHash] = hash("password123")
            it[role] = "student"
        }

        UsersTable.insert {
            it[id] = "student-02"
            it[fullName] = "Amira Trabelsi"
            it[email] = "amira@thee.tn"
            it[passwordHash] = hash("password123")
            it[role] = "student"
        }

        val now = Clock.System.now()

        // Create a demo class
        ClassesTable.insert {
            it[id] = "class-01"
            it[name] = "Sprint 2 Demo"
            it[teacherId] = "teacher-01"
            it[joinCode] = "DEMO01"
            it[createdAt] = now
        }

        // Enroll students in the class
        EnrollmentsTable.insert {
            it[classId] = "class-01"
            it[studentId] = "student-01"
            it[joinedAt] = now
        }

        EnrollmentsTable.insert {
            it[classId] = "class-01"
            it[studentId] = "student-02"
            it[joinedAt] = now
        }

        // Create a demo quiz
        QuizzesTable.insert {
            it[id] = "quiz-01"
            it[title] = "Demo Listening"
            it[description] = null
            it[timeLimitMinutes] = 0
            it[teacherId] = "teacher-01"
            it[archived] = false
            it[createdAt] = now
            it[updatedAt] = now
        }

        // Create questions for the quiz
        QuestionsTable.insert {
            it[id] = "q-01"
            it[quizId] = "quiz-01"
            it[prompt] = "Question 1"
            it[order] = 1
            it[points] = 1.0
            it[options] = Json.encodeToString(listOf("A", "B", "C", "D"))
            it[correctAnswer] = "A"
        }

        QuestionsTable.insert {
            it[id] = "q-02"
            it[quizId] = "quiz-01"
            it[prompt] = "Question 2"
            it[order] = 2
            it[points] = 1.0
            it[options] = Json.encodeToString(listOf("A", "B", "C", "D"))
            it[correctAnswer] = "B"
        }

        // Link quiz to class
        QuizClassesTable.insert {
            it[quizId] = "quiz-01"
            it[classId] = "class-01"
        }

        logger.info("Seeded 3 dev users (password: password123)")
    }
}

suspend fun <T> dbQuery(block: () -> T): T =
    newSuspendedTransaction(Dispatchers.IO, DatabaseFactory.database) { block() }
