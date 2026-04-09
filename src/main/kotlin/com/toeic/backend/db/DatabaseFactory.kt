package com.toeic.backend.db

import at.favre.lib.crypto.bcrypt.BCrypt
import com.toeic.backend.classes.ClassesTable
import com.toeic.backend.enrollments.EnrollmentsTable
import com.toeic.backend.quizzes.QuestionsTable
import com.toeic.backend.quizzes.QuizzesTable
import com.toeic.backend.users.UsersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
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
        val user = System.getenv("POSTGRES_USER") ?: "postgres"
        val password = System.getenv("POSTGRES_PASSWORD") ?: "admin"

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
            SchemaUtils.create(UsersTable, ClassesTable, EnrollmentsTable, QuizzesTable, QuestionsTable)
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

        logger.info("Seeded 3 dev users (password: password123)")
    }
}

suspend fun <T> dbQuery(block: () -> T): T =
    newSuspendedTransaction(Dispatchers.IO, DatabaseFactory.database) { block() }
