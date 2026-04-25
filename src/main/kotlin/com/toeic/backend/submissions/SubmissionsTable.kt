package com.toeic.backend.submissions

import com.toeic.backend.quizzes.QuizzesTable
import com.toeic.backend.users.UsersTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object SubmissionsTable : Table("submissions") {
    val id = varchar("id", 50)
    val quizId = varchar("quiz_id", 50).references(QuizzesTable.id)
    val studentId = varchar("student_id", 50).references(UsersTable.id)
    val durationSeconds = integer("duration_seconds")
    val score = double("score")
    val maxScore = double("max_score")
    val correctCount = integer("correct_count")
    val totalCount = integer("total_count")
    val answersJson = text("answers_json")          // serialized List<StudentAnswerDto>
    val aiDispatchStatus = varchar("ai_dispatch_status", 20).default("pending") // pending|received|failed
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, studentId)
        index(false, quizId)
    }
}
