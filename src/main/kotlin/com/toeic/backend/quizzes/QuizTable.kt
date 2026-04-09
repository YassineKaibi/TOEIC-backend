package com.toeic.backend.quizzes

import com.toeic.backend.users.UsersTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object QuizzesTable : Table("quizzes") {
    val id = varchar("id", 50)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val timeLimitMinutes = integer("time_limit_minutes").nullable()
    val teacherId = varchar("teacher_id", 50).references(UsersTable.id)
    val archived = bool("archived").default(false)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

object QuestionsTable : Table("questions") {
    val id = varchar("id", 50)
    val quizId = varchar("quiz_id", 50).references(QuizzesTable.id)
    val prompt = text("prompt")
    val order = integer("order_index")
    val points = double("points")
    val options = text("options") // serialized JSON string of List<String>
    val correctAnswer = text("correct_answer")

    override val primaryKey = PrimaryKey(id)
}
